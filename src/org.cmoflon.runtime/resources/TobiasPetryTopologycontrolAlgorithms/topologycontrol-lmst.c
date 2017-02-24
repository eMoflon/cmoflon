#include <stdbool.h>
#include <stdio.h>
#include "contiki.h"
#include "contiki-lib.h"
#include "dev/watchdog.h"

#include "topologycontrol-lmst.h"
#include "../../app-conf.h"
#include "../../lib/boot.h"
#include "../../lib/components.h"
#include "../../lib/neighbors.h"
#include "../../lib/networkaddr.h"
#include "../../lib/utilities.h"

#define DEBUG 0
#if DEBUG
#define PRINTF(...) printf(__VA_ARGS__)
#else
#define PRINTF(...)
#endif

#define NETWORKADDR_MAX(edge) (networkaddr_cmp(edge->node1, edge->node2) < 0 ? edge->node2 : edge->node1)

typedef struct node {
	struct neighbor *next;
	networkaddr_t *address;
	neighbor_t *edge;
} node_t;

void _lmst_nodelist_reconstruct();
bool _lmst_nodelist_hasunconnected();
bool _lmst_nodelist_isconnected(networkaddr_t *address);
void _lmst_nodelist_connect(neighbor_t *edge);

#if COMPONENT_TOPOLOGYCONTROL == COMPONENT_TOPOLOGYCONTROL_LMST

MEMB(memb_nodelist, node_t, COMPONENT_NETWORK_NEXTHOPS_MEMORY);
LIST(list_nodelist);

PROCESS(component_topologycontrol, "topologycontrol: lmst");
PROCESS_THREAD(component_topologycontrol, ev, data) {
	PROCESS_BEGIN();

	memb_init(&memb_nodelist);
	list_init(list_nodelist);

	BOOT_COMPONENT_WAIT(component_topologycontrol);

	static struct etimer waittime;
	etimer_set(&waittime, CLOCK_SECOND * COMPONENT_TOPOLOGYCONTROL_LMST_UPDATEINTERVAL);
	while(1) {
		PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&waittime));
		etimer_reset(&waittime);

		// with a large neighbourhood the algorithm may take a looong time, so stop the watchdog
		// that the node is not rebooted because the CPU thinks there's an endless loop running
		watchdog_stop();

		// Prim algorithm
		// (connects edge of every node except the graph's root node - the node this code is running on=
		_lmst_nodelist_reconstruct();
		while(_lmst_nodelist_hasunconnected()) {

			// find best edge according to PRIM and LMST algorithm
			neighbor_t *edge_actual, *edge_best = NULL;
			for(edge_actual = list_head(component_neighbordiscovery_neighbors()); edge_actual != NULL; edge_actual = list_item_next(edge_actual)) {
				bool node1_connected = _lmst_nodelist_isconnected(edge_actual->node1) && !_lmst_nodelist_isconnected(edge_actual->node2);
				bool node2_connected = _lmst_nodelist_isconnected(edge_actual->node2) && !_lmst_nodelist_isconnected(edge_actual->node1);
				if(node1_connected ^ node2_connected) {
					bool criteria1 = edge_best == NULL;
          			bool criteria2 =  MAX(edge_actual->weight_node1_to_node2, edge_actual->weight_node2_to_node1) <  MAX(edge_best->weight_node1_to_node2, edge_best->weight_node2_to_node1));
					bool criteria3 = (MAX(edge_actual->weight_node1_to_node2, edge_actual->weight_node2_to_node1) == MAX(edge_best->weight_node1_to_node2, edge_best->weight_node2_to_node1) 
                           && networkaddr_cmp(NETWORKADDR_MAX(edge_actual), NETWORKADDR_MAX(edge_best)) < 0);
					if(criteria1 || criteria2 || criteria3)
						edge_best = edge_actual;
				}
			}
			if(edge_best == NULL) {
				printf("ERROR[topologycontrol-lmst]: no edge for spanning tree found\n");
				watchdog_reboot(); // we would end in an endless loop
			}

			_lmst_nodelist_connect(edge_best);
		}

#if DEBUG
		node_t *item_node;
		PRINTF("DEBUG: [topologycontrol-lmst] spanning tree: ");
		for(item_node = list_head(list_nodelist); item_node != NULL; item_node = list_item_next(item_node)) {
			if(networkaddr_equal(item_node->address, networkaddr_node_addr()))
				continue;

			PRINTF("%s<->", networkaddr2string_buffered(item_node->edge->node1));
			PRINTF("%s", networkaddr2string_buffered(item_node->edge->node2));
			PRINTF("%s", item_node->next == NULL ? "\n" : ", ");
		}
#endif

		/**
		 * We ignore every node(!!) in in the LMST entry list that
		 */
		node_t *node;
		for(node = list_head(list_nodelist); node != NULL; node = list_item_next(node)) {
			if(!networkaddr_equal(node->address, networkaddr_node_addr()) && 		// The node is not the self-node
                !networkaddr_equal(node->edge->node1, networkaddr_node_addr()) &&	// edge->node1 is not the self-node
                !networkaddr_equal(node->edge->node2, networkaddr_node_addr())) 	// edge->node2 is not the self-node
			{
			  component_network_ignoredlinks_add(node->address);					// ==> Do not receive messages from this node anymore, which means disabling the link TO node->address
			}
		}

		watchdog_start();

		// LMST is only run once because if links have been ignored they are no longer available in the neighbor discovery
		// and hence a spanning tree can no longer be built
		PRINTF("DEBUG: [topologycontrol-lmst] LMST algorithm is finished and will run no more\n");
		break;
	}

	PROCESS_END();
}

/**
 * Fills the LMST entry list with an LMST entry for each node
 */
void _lmst_nodelist_reconstruct() {
	// clean list
	while(list_length(list_nodelist) > 0) {
		node_t *item = list_pop(list_nodelist);
		networkaddr_reference_free(item->address);
		memb_free(&memb_nodelist, item);
	}

	// add all nodes to list
	neighbor_t *item_neighbor;
	for(item_neighbor = list_head(component_neighbordiscovery_neighbors()); item_neighbor != NULL; item_neighbor = list_item_next(item_neighbor)) {
		node_t *item_node;
		bool found;

		// check for node1
		found = false;
		for(item_node = list_head(list_nodelist); item_node != NULL; item_node = list_item_next(item_node)) {
			if(networkaddr_equal(item_neighbor->node1, item_node->address)) {
				found = true;
				break;
			}
		}
		if(!found) {
			if((item_node = memb_alloc(&memb_nodelist)) == NULL) {
				printf("ERROR[topologycontrol-lmst]: nodelist is full\n");
			} else {
				item_node->address = networkaddr_reference_alloc(item_neighbor->node1);
				item_node->edge = NULL;
				list_add(list_nodelist, item_node);
			}
		}

		// check for node2
		found = false;
		for(item_node = list_head(list_nodelist); item_node != NULL; item_node = list_item_next(item_node)) {
			if(networkaddr_equal(item_neighbor->node2, item_node->address)) {
				found = true;
				break;
			}
		}
		if(!found) {
			if((item_node = memb_alloc(&memb_nodelist)) == NULL) {
				printf("ERROR[topologycontrol-lmst]: nodelist is full\n");
			} else {
				item_node->address = networkaddr_reference_alloc(item_neighbor->node2);
				item_node->edge = NULL;
				list_add(list_nodelist, item_node);
			}
		}
	}
}

/**
 * Identifies a node for which "_lmst_nodelist_isconnected" would return false
 */
bool _lmst_nodelist_hasunconnected() {
	node_t *item_node;
	for(item_node = list_head(list_nodelist); item_node != NULL; item_node = list_item_next(item_node)) {
		if(item_node->edge == NULL && !networkaddr_equal(networkaddr_node_addr(), item_node->address))
			return true;
	}

	return false;
}

/**
 * Returns whether the given node has an edge in the (tentative) LMST
 *
 * A node is connected if its LMST entry has a non-null edge
 *
 * By default, the self-node is always connected.
 */
bool _lmst_nodelist_isconnected(networkaddr_t *address) {
	node_t *item_node;
	for(item_node = list_head(list_nodelist); item_node != NULL; item_node = list_item_next(item_node)) {
		if(networkaddr_equal(item_node->address, address) &&
				(item_node->edge != NULL || networkaddr_equal(networkaddr_node_addr(), item_node->address)))
			return true;
	}

	return false;
}

void _lmst_nodelist_connect(neighbor_t *edge) {
	node_t *item_node;
	/*
	 * Iterate over all LMST entries,
	 * check which of the entries correspond to the source and
	 * target node of the to-be-connected edge and connect the edge to the source and target node entries.
	 *
	 * Exception: The self-node (cf. networkadd_node_add()) should not be connected to any edge
	 */
	for(item_node = list_head(list_nodelist); item_node != NULL; item_node = list_item_next(item_node)) {
		if (item_node->edge == NULL)
		{
			if(networkaddr_equal(item_node->address, edge->node1) && !networkaddr_equal(networkaddr_node_addr(), edge->node1))
				item_node->edge = edge;

			if(networkaddr_equal(item_node->address, edge->node2) && !networkaddr_equal(networkaddr_node_addr(), edge->node2))
				item_node->edge = edge;
		}
	}
}

#endif
