#include <stdlib.h>
#include <stdio.h>
#include "contiki.h"
#include "contiki-lib.h"
#include "dev/watchdog.h"

#include "topologycontrol-aktc.h"
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

#define AKTC_MIN(a, b) (a != COMPONENT_NEIGHBORDISCOVERY_WEIGHTUNKNOWN && b != COMPONENT_NEIGHBORDISCOVERY_WEIGHTUNKNOWN ? MIN(a, b) : COMPONENT_NEIGHBORDISCOVERY_WEIGHTUNKNOWN)
#define AKTC_MAX(a, b) (a != COMPONENT_NEIGHBORDISCOVERY_WEIGHTUNKNOWN && b != COMPONENT_NEIGHBORDISCOVERY_WEIGHTUNKNOWN ? MAX(a, b) : COMPONENT_NEIGHBORDISCOVERY_WEIGHTUNKNOWN)
#define AKTC_AVG(a, b) (a != COMPONENT_NEIGHBORDISCOVERY_WEIGHTUNKNOWN && b != COMPONENT_NEIGHBORDISCOVERY_WEIGHTUNKNOWN ? ((a + b) / 2) : COMPONENT_NEIGHBORDISCOVERY_WEIGHTUNKNOWN)

#if COMPONENT_TOPOLOGYCONTROL == COMPONENT_TOPOLOGYCONTROL_AKTC

PROCESS(component_topologycontrol, "topologycontrol: aktc");
PROCESS_THREAD(component_topologycontrol, ev, data) {
	PROCESS_BEGIN();

	BOOT_COMPONENT_WAIT(component_topologycontrol);

	static struct etimer waittime;
	etimer_set(&waittime, CLOCK_SECOND * COMPONENT_TOPOLOGYCONTROL_AKTC_UPDATEINTERVAL);
	while(1) {
		PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&waittime));
		etimer_reset(&waittime);

		// with a large neighbourhood the algorithm may take a looong time, so stop the watchdog
		// that the node is not rebooted because the CPU thinks there's an endless loop running
		watchdog_stop();

		// find triangles and drop edges with a-ktc criteria
		neighbor_t *onehop;
		for(onehop = list_head(component_neighbordiscovery_neighbors()); onehop != NULL; onehop = list_item_next(onehop)) {
			if(networkaddr_equal(onehop->node1, networkaddr_node_addr())) {
				neighbor_t *nexthop = neighbors_find_triangle(component_neighbordiscovery_neighbors(), onehop, aktc_criteria);
				if(nexthop != NULL) {
					component_network_ignoredlinks_add(onehop->node2);
				}
			}
		}

		// drop unidirectional edges
		for(onehop = list_head(component_neighbordiscovery_neighbors()); onehop != NULL; onehop = list_item_next(onehop)) {
			if(networkaddr_equal(onehop->node1, networkaddr_node_addr()) && onehop->weight_node1_to_node2 == COMPONENT_NEIGHBORDISCOVERY_WEIGHTUNKNOWN) {
				component_network_ignoredlinks_add(onehop->node2);

#if DEBUG
				char node_other[NETWORKADDR_STRSIZE];
				PRINTF("DEBUG: [topologycontrol-aktc] drop unidirectional edge ");
				PRINTF("node1=%s, node2=%s: ", networkaddr2string_buffered(onehop->node1), networkaddr2string(node_other, onehop->node2));
				PRINTF("node1->node2[weight=%d] ", onehop->weight_node1_to_node2);
				PRINTF("node2->node1[weight=%d]", onehop->weight_node2_to_node1);
				PRINTF("\n");
#endif
			}
		}

		watchdog_start();
	}

	PROCESS_END();
}

#endif

int aktc_criteria(const neighbor_t *directhop, const neighbor_t *onehop, const neighbor_t *twohop) {
	int8_t edge_directhop = AKTC_AVG(directhop->weight_node1_to_node2, directhop->weight_node2_to_node1);
	int8_t edge_onehop    = AKTC_AVG(onehop->weight_node1_to_node2, onehop->weight_node2_to_node1);
	int8_t edge_twohop    = AKTC_AVG(twohop->weight_node1_to_node2, twohop->weight_node2_to_node1);
	int8_t edge_shortest =  AKTC_MIN(edge_onehop, edge_twohop);

#if DEBUG
	char node_other[NETWORKADDR_STRSIZE];
	PRINTF("DEBUG: [topologycontrol-aktc] possible triangle:\n");
	PRINTF("DEBUG: [topologycontrol-aktc] * directhop ");
	PRINTF("node1=%s, node2=%s: ", networkaddr2string_buffered(directhop->node1), networkaddr2string(node_other, directhop->node2));
	PRINTF("node1->node2[weight=%d] ", directhop->weight_node1_to_node2);
	PRINTF("node2->node1[weight=%d]", directhop->weight_node2_to_node1);
	PRINTF("\n");
	PRINTF("DEBUG: [topologycontrol-aktc] * onehop ");
	PRINTF("node1=%s, node2=%s: ", networkaddr2string_buffered(onehop->node1), networkaddr2string(node_other, onehop->node2));
	PRINTF("node1->node2[weight=%d] ", onehop->weight_node1_to_node2);
	PRINTF("node2->node1[weight=%d]", onehop->weight_node2_to_node1);
	PRINTF("\n");
	PRINTF("DEBUG: [topologycontrol-aktc] * twohop ");
	PRINTF("node1=%s, node2=%s: ", networkaddr2string_buffered(twohop->node1), networkaddr2string(node_other, twohop->node2));
	PRINTF("node1->node2[weight=%d] ", twohop->weight_node1_to_node2);
	PRINTF("node2->node1[weight=%d]", twohop->weight_node2_to_node1);
	PRINTF("\n");
	PRINTF("DEBUG: [topologycontrol-aktc] * edge_directhop=%d, edge_onehop=%d, edge_twohop=%d, edge_shortest=%d\n", edge_directhop, edge_onehop, edge_twohop, edge_shortest);

#endif

	if(edge_directhop == COMPONENT_NEIGHBORDISCOVERY_WEIGHTUNKNOWN)
		goto skip;
	if(edge_onehop == COMPONENT_NEIGHBORDISCOVERY_WEIGHTUNKNOWN)
		goto skip;
	if(edge_twohop == COMPONENT_NEIGHBORDISCOVERY_WEIGHTUNKNOWN)
		goto skip;
	if(edge_directhop <= edge_onehop || edge_directhop <= edge_twohop)
		goto skip; // adjacent hop is not the longest one

	if(COMPONENT_TOPOLOGYCONTROL_AKTC_K * edge_shortest - edge_directhop < 0) {
		PRINTF("DEBUG: [topologycontrol-aktc] * => criteria fulfilled\n");
		return 1;
	}

	skip:
		PRINTF("DEBUG: [topologycontrol-aktc] * => criteria not fulfilled\n");
		return 0;
}
