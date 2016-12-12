#include <stdlib.h>
#include <stdio.h>
#include "contiki.h"
#include "contiki-lib.h"
#include "dev/watchdog.h"

#include "topologycontrol-lktc.h"
#include "topologycontrol-aktc.h"
#include "../../app-conf.h"
#include "../../lib/boot.h"
#include "../../lib/components.h"
#include "../../lib/neighbors.h"
#include "../../lib/networkaddr.h"
#include "../../lib/uniqueid.h"
#include "../../lib/utilities.h"

/**
 * Implementation Concept
 * ======================
 *
 * Spreading the Hopcount
 * ----------------------
 * Starting at the base station the hopcount is spread through the whole sensor network. The base station
 * is sharing its (newly calculated) hopcount=0. Every other node that has the base station as nexthop for
 * transmitting messages to the base station now knows that it has hopcount=1 and shares this information.
 * This will (slowly) spread the information of the required hops to the base station for every node and
 * will update (slowly) when routing changes occur.
 * Sharing happens by doing an immediate broadcast once to fastly spread the information. In case a mote
 * missed the broadcast the hopcount is sent again with a small delay once, from there on the hopcount is
 * broadcasted in a periodic way.
 *
 * Removing edges
 * --------------
 * The l-ktc algorithm defines that any edge e = (u, v) may only be removed if the distance from u and v only
 * increases by a specific increase factor a. Removing an edge would need coordination between node u and v to
 * discuss how the hopcount would increase if the edge is dropped, but this is complex and would rise the
 * message overhead of the system.
 * This implementation is working completely on local information by using a simple trick: The l-ktc algorithm
 * is only defined for data collection applications sending messages from any node to the base station. So any
 * implementation would only need to drop an edge e = (u, v) which is directed towards the base station. Removing
 * any other edges would not have any effect because they are not used for sending messages. So these edges could
 * be removed by u and v, but v would never use this edge. The only node (for this implementation) which will
 * remove the edge is u because it can do this completely on local information as the hopcount on v will
 * not change.
 * The implementation will evaluate every triangle if the hopcount of the current node (u) is higher (or equal) than the
 * directhop (v). This rule ensures only edges are tested that when removing the edge node v's hopcount will not increase,
 * because if (1) the hopcount of v is lower, node u is farer away and v is a possible nexthop to the base station, (2)
 * if hopcount of u and v are same neither node is forwarding any packet for each other and the hopcount of v is
 * impossible to increase.
 *
 * Violation of edge-direction removal rule
 * ----------------------------------------
 * l-ktc implies that only edges towards the base station are dropped. But edges can only be dropped unidirectional at a mote.
 * So if a mote drops an edge to a nexthop any message sent from the mote to the nexthop will still be received. Route discovery
 * may nevertheless want to create a route towards the base station by the dropped nexthop. This is because this mote is doing
 * netflooding by broadcasting a route discovery request (RREQ). This mote may drop all messages it receives from the nexthop but
 * the nexthop is not aware of this dropped link and still received the RREQ and correctly handles it. So it would answer this mote
 * that it can route any message by him but this route reply (RREP) would be discarded by this mote. Finally no route can ever be
 * constructed because the dropped link is unidirectional.
 * To solve this problem the edge-direction rule has to be violated: If a mote x will drop an edge to mote y according to the original
 * l-ktc rule, the mote y will drop the edge to mote x as well. This can be done solely on local information without any error
 * because both motes will calculate the same triangle information. So mote y can drop the edge as well because it is able to calculate
 * that mote x will correctly drop the edge according to the l-ktc ruleset.
 */
#if COMPONENT_TOPOLOGYCONTROL == COMPONENT_TOPOLOGYCONTROL_LKTC

#define DEBUG 0
#if DEBUG
#define PRINTF(...) printf(__VA_ARGS__)
#else
#define PRINTF(...)
#endif

typedef struct hopcount {
	struct hopcount *next;
	networkaddr_t *address;
	int8_t hopcount;
} hopcount_t;

MEMB(memb_hopcount, hopcount_t, COMPONENT_NETWORK_NEXTHOPS_MEMORY);
LIST(list_hopcount);

static uint8_t messagetype_hopcount;
static int8_t my_hopcount = -1;

static int _hopcount_update();
static int8_t _hopcount_get(networkaddr_t *node);
static void _hopcount_recv(const networkaddr_t *source, buffer_t *data, int8_t rssi);
static void _hopcount_broadcast();

static struct etimer etimer_broadcast;

PROCESS(component_topologycontrol, "topologycontrol: lktc");
PROCESS_THREAD(component_topologycontrol, ev, data) {
	PROCESS_BEGIN();

	memb_init(&memb_hopcount);
	list_init(list_hopcount);

	BOOT_COMPONENT_WAIT(component_topologycontrol);

	messagetype_hopcount = uniqueid_assign();
	component_network_packet_subscribe(messagetype_hopcount, _hopcount_recv);

	static struct etimer etimer_hopcountstart;
	etimer_set(&etimer_hopcountstart, CLOCK_SECOND * 90);

	// static struct etimer etimer_broadcast
	etimer_set(&etimer_broadcast, CLOCK_SECOND * 500); // unimportant high value, real interval is set later in time

	static struct etimer etimer_hopcountupdate;
	etimer_set(&etimer_hopcountupdate, CLOCK_SECOND * 30);

	static struct etimer etimer_ktc;
	etimer_set(&etimer_ktc, CLOCK_SECOND * COMPONENT_TOPOLOGYCONTROL_LKTC_UPDATEINTERVAL);

	while(1) {
		PROCESS_WAIT_EVENT();

		// a small delay for the base station to start broadcasting that all motes have booted and can receive this information
		if(etimer_expired(&etimer_hopcountstart)) {
			if(networkaddr_equal(component_network_address_basestation(), networkaddr_node_addr())) {
				PRINTF("DEBUG: [topologycontrol-lktc] updated hopcount to 0 hops\n");
				my_hopcount = 0;

				etimer_set(&etimer_broadcast, random(CLOCK_SECOND * COMPONENT_TOPOLOGYCONTROL_LKTC_BROADCASTHOPCOUNT_IMMEDIATE_MIN, CLOCK_SECOND * COMPONENT_TOPOLOGYCONTROL_LKTC_BROADCASTHOPCOUNT_IMMEDIATE_MAX));
			}

			etimer_stop(&etimer_hopcountstart);
			etimer_hopcountstart.p = PROCESS_ZOMBIE; // etimer_expired is true if no process is assigned to etimer which happens when expiring (event if stopped), so assign the zombie process
		}

		if(etimer_expired(&etimer_broadcast)) {
			_hopcount_broadcast();
			// _hopcount_broadcast will update the timer, so no reseting here
		}

		if(etimer_expired(&etimer_hopcountupdate)) {
			etimer_reset(&etimer_hopcountupdate);

			// in some cases the hopcount information has already been received but the route to the basestation has
			// not been found at the time of receiving the messages or it has changed
			if(_hopcount_update()) {
				etimer_set(&etimer_broadcast, random(CLOCK_SECOND * COMPONENT_TOPOLOGYCONTROL_LKTC_BROADCASTHOPCOUNT_IMMEDIATE_MIN, CLOCK_SECOND * COMPONENT_TOPOLOGYCONTROL_LKTC_BROADCASTHOPCOUNT_IMMEDIATE_MAX));
			}
		}

		if(etimer_expired(&etimer_ktc)) {
			etimer_reset(&etimer_ktc);

			// with a large neighbourhood the algorithm may take a looong time, so stop the watchdog
			// that the node is not rebooted because the CPU thinks there's an endless loop running
			watchdog_stop();

			// find triangles and drop edges with l-ktc criteria
			neighbor_t *onehop;
			for(onehop = list_head(component_neighbordiscovery_neighbors()); onehop != NULL; onehop = list_item_next(onehop)) {
				if(networkaddr_equal(onehop->node1, networkaddr_node_addr())) {
					neighbor_t *nexthop = neighbors_find_triangle(component_neighbordiscovery_neighbors(), onehop, lktc_criteria);
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
					PRINTF("DEBUG: [topologycontrol-lktc] drop unidirectional edge ");
					PRINTF("node1=%s, node2=%s: ", networkaddr2string_buffered(onehop->node1), networkaddr2string(node_other, onehop->node2));
					PRINTF("node1->node2[weight=%d] ", onehop->weight_node1_to_node2);
					PRINTF("node2->node1[weight=%d]", onehop->weight_node2_to_node1);
					PRINTF("\n");
#endif
				}
			}

			watchdog_start();
		}
	}

	PROCESS_END();
}

static int _hopcount_update() {
	int8_t copy_my_hopcount = my_hopcount;
	networkaddr_t *nexthop_basestation = component_network_nexthops_basestation();

	if(networkaddr_equal(networkaddr_node_addr(), component_network_address_basestation()))
		my_hopcount = 0;

	hopcount_t *item;
	for(item = list_head(list_hopcount); item != NULL; item = list_item_next(item)) {
		if(networkaddr_equal(item->address, nexthop_basestation)) {
			my_hopcount = item->hopcount + 1;
		}
	}

	if(copy_my_hopcount != my_hopcount) {
		PRINTF("DEBUG: [topologycontrol-lktc] updated hopcount to %d hops\n", my_hopcount);
		return 1;
	}

	return 0;
}

static int8_t _hopcount_get(networkaddr_t *node) {
	if(networkaddr_equal(networkaddr_node_addr(), node))
		return my_hopcount;

	hopcount_t *item;
	for(item = list_head(list_hopcount); item != NULL; item = list_item_next(item)) {
		if(networkaddr_equal(item->address, node)) {
			return item->hopcount;
		}
	}

	return -1;
}

static void _hopcount_recv(const networkaddr_t *source, buffer_t *data, int8_t rssi) {
	hopcount_t *item;
	for(item = list_head(list_hopcount); item != NULL; item = list_item_next(item)) {
		if(networkaddr_equal(item->address, source))
			break;
	}

	if(item == NULL) {
		if((item = memb_alloc(&memb_hopcount)) == NULL) {
			printf("ERROR[topologycontrol-lktc]: hopcount-list is full\n");
			return;
		}

		item->address = networkaddr_reference_alloc(source);
		list_add(list_hopcount, item);
	}

	item->hopcount = buffer_read_uint8t(data);
	if(_hopcount_update()) {
		// schedule broadcasting the new hopcount
		etimer_set(&etimer_broadcast, random(CLOCK_SECOND * COMPONENT_TOPOLOGYCONTROL_LKTC_BROADCASTHOPCOUNT_IMMEDIATE_MIN, CLOCK_SECOND * COMPONENT_TOPOLOGYCONTROL_LKTC_BROADCASTHOPCOUNT_IMMEDIATE_MAX));
	}
}

static void _hopcount_broadcast(s) {
	// the initialized timer expired for a node which does not yet know it's hopcount
	if(my_hopcount == -1) {
		etimer_reset(&etimer_broadcast);
		return;
	}

	PRINTF("DEBUG: [topologycontrol-lktc] broadcasting my hopcount = %d\n", my_hopcount);
	buffer_t *data = component_network_packet_sendbuffer();
	buffer_append_uint8t(data, my_hopcount);
	component_network_packet_send(COMPONENT_NETWORK_TRANSMISSION_LINKLOCAL_BROADCAST, messagetype_hopcount, NULL, data, -1, -1);

	// set new broadcast time on special rules:
	// * last interval <= IMMEDIATE_MAX: smalldelay broadcast
	// * else: periodic broadcast
	if(etimer_broadcast.timer.interval <= CLOCK_SECOND * COMPONENT_TOPOLOGYCONTROL_LKTC_BROADCASTHOPCOUNT_IMMEDIATE_MAX)
		etimer_set(&etimer_broadcast, random(CLOCK_SECOND * COMPONENT_TOPOLOGYCONTROL_LKTC_BROADCASTHOPCOUNT_SMALLDELAY_MIN, CLOCK_SECOND * COMPONENT_TOPOLOGYCONTROL_LKTC_BROADCASTHOPCOUNT_SMALLDELAY_MAX));
	else
		etimer_set(&etimer_broadcast, random(CLOCK_SECOND * COMPONENT_TOPOLOGYCONTROL_LKTC_BROADCASTHOPCOUNT_PERIODIC_MIN, CLOCK_SECOND * COMPONENT_TOPOLOGYCONTROL_LKTC_BROADCASTHOPCOUNT_PERIODIC_MAX));
}

int lktc_criteria(const neighbor_t *directhop, const neighbor_t *onehop, const neighbor_t *twohop) {
	if(!aktc_criteria(directhop, onehop, twohop))
		return 0;

	int8_t hopcount_self = _hopcount_get(networkaddr_node_addr());
	int8_t hopcount_newnexthop = _hopcount_get(onehop->node2);
	int8_t hopcount_destination = _hopcount_get(directhop->node2);

#if DEBUG
	PRINTF("DEBUG: [topologycontrol-lktc] possible triangle:\n");
	PRINTF("DEBUG: [topologycontrol-lktc] * directhop ");
	PRINTF("node1[%s, %d hops] -> ", networkaddr2string_buffered(directhop->node1), hopcount_self);
	PRINTF("node2[%s, %d hops]\n", networkaddr2string_buffered(directhop->node2), hopcount_destination);

	PRINTF("DEBUG: [topologycontrol-lktc] * onehop ");
	PRINTF("node1[%s, %d hops] -> ", networkaddr2string_buffered(onehop->node1), hopcount_self);
	PRINTF("node2[%s, %d hops]\n", networkaddr2string_buffered(onehop->node2), hopcount_newnexthop);

	PRINTF("DEBUG: [topologycontrol-lktc] * twohop ");
	PRINTF("node1[%s, %d hops] -> ", networkaddr2string_buffered(twohop->node1), hopcount_newnexthop);
	PRINTF("node2[%s, %d hops]\n", networkaddr2string_buffered(twohop->node2), hopcount_destination);
#endif

	// one member of the triangle is still in an invalid state
	if(hopcount_self == -1 || hopcount_newnexthop == -1|| hopcount_destination == -1)
		goto skip;

	// option 1: drop edge if it's not directed towards the base station => same hopcount
	if(hopcount_self == hopcount_destination)
		goto drop;

	// option 1: check if i would drop the edge
	float increase_factor = (hopcount_self == 0) ? 1 : (((float) hopcount_newnexthop + 1) / ((float) hopcount_self));
	if(hopcount_destination <= hopcount_self && increase_factor <= COMPONENT_TOPOLOGYCONTROL_LKTC_STRETCHFACTOR)
		goto drop;

	// option 2: check if the other mote would drop the edge
	float increase_factor_inverse = (hopcount_destination == 0) ? 1 : (((float) hopcount_newnexthop + 1) / ((float) hopcount_destination));
	if(hopcount_destination > hopcount_self && increase_factor_inverse <= COMPONENT_TOPOLOGYCONTROL_LKTC_STRETCHFACTOR)
		goto drop;

	skip:
		PRINTF("DEBUG: [topologycontrol-lktc] * => criteria not fulfilled\n");
		return 0;

	drop:
		PRINTF("DEBUG: [topologycontrol-lktc] * => criteria fulfilled\n");
		return 1;
}

#endif
