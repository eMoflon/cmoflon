#include "topologycontrol-cMoflonDemoLanguage2_C.h"


// --- Start of declarations for hop count usage
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
static void _hopcount_recv(const networkaddr_t *source, buffer_t *data, int8_t rssi);
static void _hopcount_broadcast();

static struct etimer etimer_broadcast;

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
	printf("DEBUG: [topologycontrol-lktc] updated hopcount to %d hops\n", my_hopcount);
	return 1;
}

	return 0;
}

static void _hopcount_recv(const networkaddr_t *source, buffer_t *data, int8_t rssi) {
	hopcount_t *item;
	for(item = list_head(list_hopcount); item != NULL; item = list_item_next(item)) {
		if(networkaddr_equal(item->address, source))
			break;
	}

	if(item == NULL) {
		if((item = memb_alloc(&memb_hopcount)) == NULL) {
			printf("ERROR[topologycontrol]: hopcount-list is full\n");
			return;
		}
			item->address = networkaddr_reference_alloc(source);
		list_add(list_hopcount, item);
	}

	item->hopcount = buffer_read_uint8t(data);
	if(_hopcount_update()) {
		// schedule broadcasting the new hopcount
		etimer_set(&etimer_broadcast, random(CLOCK_SECOND * COMPONENT_TOPOLOGYCONTROL_CMOFLONDEMOLANGUAGE2_C_BROADCASTHOPCOUNT_IMMEDIATE_MIN, CLOCK_SECOND * COMPONENT_TOPOLOGYCONTROL_CMOFLONDEMOLANGUAGE2_C_BROADCASTHOPCOUNT_IMMEDIATE_MAX));
	}
}

static void _hopcount_broadcast(s) {
	// the initialized timer expired for a node which does not yet know it's hopcount
	if(my_hopcount == -1) {
		etimer_reset(&etimer_broadcast);
		return;
	}

	printf("DEBUG: [topologycontrol-lktc] broadcasting my hopcount = %d\n", my_hopcount);
	buffer_t *data = component_network_packet_sendbuffer();
	buffer_append_uint8t(data, my_hopcount);
	component_network_packet_send(COMPONENT_NETWORK_TRANSMISSION_LINKLOCAL_BROADCAST, messagetype_hopcount, NULL, data, -1, -1);

	// set new broadcast time on special rules:
	// * last interval <= IMMEDIATE_MAX: smalldelay broadcast
	// * else: periodic broadcast
	if(etimer_broadcast.timer.interval <= CLOCK_SECOND * COMPONENT_TOPOLOGYCONTROL_CMOFLONDEMOLANGUAGE2_C_BROADCASTHOPCOUNT_IMMEDIATE_MAX)
		etimer_set(&etimer_broadcast, random(CLOCK_SECOND * COMPONENT_TOPOLOGYCONTROL_CMOFLONDEMOLANGUAGE2_C_BROADCASTHOPCOUNT_SMALLDELAY_MIN, CLOCK_SECOND * COMPONENT_TOPOLOGYCONTROL_CMOFLONDEMOLANGUAGE2_C_BROADCASTHOPCOUNT_SMALLDELAY_MAX));
	else
		etimer_set(&etimer_broadcast, random(CLOCK_SECOND * COMPONENT_TOPOLOGYCONTROL_CMOFLONDEMOLANGUAGE2_C_BROADCASTHOPCOUNT_PERIODIC_MIN, CLOCK_SECOND * COMPONENT_TOPOLOGYCONTROL_CMOFLONDEMOLANGUAGE2_C_BROADCASTHOPCOUNT_PERIODIC_MAX));
}
// --- End of declarations for hopcount usage
// --- Begin of default cMoflon helpers
// --- Begin of default cMoflon code
#define min(a,b) (((a)<(b))?(a):(b))
#define max(a,b) (((a)>(b))?(a):(b))

/**
 * This function returns the first element ('item') in the given 'list' for which pred(item, _this) returns true
 */
void* list_head_pred(list_t list, void* _this, bool (*pred)(void*, void*)) {
	void* item;
	for (item = list_head(list); item != NULL; item = list_item_next(item)) {
		if (pred(item, _this))
			return item;
	}
	return NULL;
}

/**
 * This function returns the closest preceding element ('item') of the given 'item' for which pred(item, _this) returns true
 */
void* list_item_next_pred(void* item, void* _this, bool (*pred)(void*, void*)) {
	for (item = list_item_next(item); item != NULL;
			item = list_item_next(item)) {
		if (pred(item, _this)) {
			return item;
		}
	}
	return NULL;
}

list_t node_getIncomingLinks(NODE_T* _this) {
	return component_neighbordiscovery_neighbors();
}

bool node_containsIncomingLinks(NODE_T* _this, LINK_T* value) {
	LINK_T* link;
	for (link = list_head_pred(component_neighbordiscovery_neighbors(), _this,
			&node_isIncomingLinks); link != NULL;
			link = list_item_next_pred(link, _this, &node_isIncomingLinks)) {
		if (link_equals(value, link))
			return true;
	}
	return false;
}

bool node_isIncomingLinks(void* candidate, void* _this) {
	if (node_equals((NODE_T*) _this, ((LINK_T*) candidate)->node2)
			&& ((LINK_T*) candidate)->weight_node2_to_node1
					!= COMPONENT_NEIGHBORDISCOVERY_WEIGHTUNKNOWN) {
		return true;
	} else
		return false;
}

list_t node_getOutgoingLinks(NODE_T* _this) {
	return component_neighbordiscovery_neighbors();
}

bool node_containsOutgoingLinks(NODE_T* _this, LINK_T* value) {
	LINK_T* link;
	for (link = list_head_pred(component_neighbordiscovery_neighbors(), _this,
			&node_isOutgoingLinks); link != NULL;
			link = list_item_next_pred(link, _this, &node_isOutgoingLinks)) {
		if (link_equals(value, link))
			return true;
	}
	return false;
}

bool node_isOutgoingLinks(void* candidate, void* _this) {
	if (node_equals((NODE_T*) _this, ((LINK_T*) candidate)->node1)
			&& ((LINK_T*) candidate)->weight_node1_to_node2
					!= COMPONENT_NEIGHBORDISCOVERY_WEIGHTUNKNOWN) {
		return true;
	} else
		return false;
}

list_t node_getNeighborhood(NODE_T* _this) {
	return component_neighbordiscovery_neighbors();
}
bool node_isNeighborhood(void* candidate, void* _this) {
	return true;
}

EDouble link_getWeight(LINK_T* _this) {
	if (_this->weight_node1_to_node2
			!= COMPONENT_NEIGHBORDISCOVERY_WEIGHTUNKNOWN) {
		return _this->weight_node1_to_node2;
	} else if (_this->weight_node2_to_node1
			!= COMPONENT_NEIGHBORDISCOVERY_WEIGHTUNKNOWN) {
		return _this->weight_node2_to_node1;
	} else
		return COMPONENT_NEIGHBORDISCOVERY_WEIGHTUNKNOWN;
}

NODE_T* link_getTarget(LINK_T* _this) {
	return _this->node2;
}

NODE_T* link_getSource(LINK_T* _this) {
	return _this->node1;
}

LinkState link_getMarked(LINK_T* _this) {
	return _this->state;
}
void link_setMarked(LINK_T* _this, LinkState value) {
	_this->state = value;
	if (value == INACTIVE) {
		if (node_equals(networkaddr_node_addr(), _this->node1)) {
			component_network_ignoredlinks_add(_this->node2);
		} else if (node_equals(networkaddr_node_addr(), _this->node2))
			component_network_ignoredlinks_add(_this->node1);
	}
	if (value == ACTIVE) {
		if (node_equals(networkaddr_node_addr(), _this->node1)) {
			component_network_ignoredlinks_remove(_this->node2);
		} else if (node_equals(networkaddr_node_addr(), _this->node2))
			component_network_ignoredlinks_remove(_this->node1);
	}
	//IF this node is not part of the edge don't ignore any of the nodes
}

int eDouble_compare(EDouble _this, EDouble other) {
	if (_this == COMPONENT_NEIGHBORDISCOVERY_WEIGHTUNKNOWN)
		return 1;
	if (other == COMPONENT_NEIGHBORDISCOVERY_WEIGHTUNKNOWN)
		return -1;
	int result = (_this < other) ? -1 : (_this > other) ? 1 : 0;
	return result;
}

bool node_equals(NODE_T* _this, NODE_T* other) {
	return networkaddr_equal(_this, other);
}

bool link_equals(LINK_T* _this, LINK_T* other) {
	return ((node_equals(_this->node1, other->node1)
			&& node_equals(_this->node2, other->node2))
			|| (node_equals(_this->node1, other->node2)
					&& node_equals(_this->node2, other->node1)));
}

bool linkState_equals(LinkState s1, LinkState s2) {
	return s1 == s2;
}

bool eBoolean_equals(EBoolean b1, EBoolean b2) {
	return b1 == b2;
}

/**
 * This function sets the state of all links to UNCLASSIFIED
 *
 * See component_neighbordiscovery_neighbors()
 * See enum LinkState
 */
void prepareLinks() {
	LINK_T* link;
	for (link = list_head(component_neighbordiscovery_neighbors());
			link != NULL; link = list_item_next(link)) {
		link->state = UNCLASSIFIED;
	}
}
// --- End of default cMoflon code// --- End of default cMoflon helpers

// --- Begin of user-defined helpers (from path 'injection/custom-helpers.c')
// --- Begin of l*-kTC-specific methods

/**
 * Returns whether the given hop counts fulfill the l*-kTC predicate
 */
EBoolean lStarKtcAlgorithm_evaluateHopcountConstraint(EInt hopCount1,
		EInt hopCount2, EInt hopCount3, EDouble stretchFactor) {
	if (min(hopCount1, min(hopCount2, hopCount3)) < 0)
		return false;
	bool result = true;
	result &= (!(hopCount1 == hopCount2) || true);
	result &= (!(hopCount1 > hopCount2)
			|| ((hopCount3 + 1) * 1.0 / max(1, hopCount1) < stretchFactor));
	result &= (!(hopCount1 < hopCount2)
			|| ((hopCount3 + 1) * 1.0 / max(1, hopCount2) < stretchFactor));
	return result;
}

EInt node_getHopcount(NODE_T* _this) {
	if (networkaddr_equal(networkaddr_node_addr(), _this))
		return my_hopcount;

	hopcount_t *item;
	for (item = list_head(list_hopcount); item != NULL;
			item = list_item_next(item)) {
		if (networkaddr_equal(item->address, _this)) {
			return item->hopcount;
		}
	}

	return -1;
}

NODE_T* lStarKtcAlgorithm_getNode(LSTARKTCALGORITHM_T* _this) {
	return _this->node;
}

EDouble lStarKtcAlgorithm_getK(LSTARKTCALGORITHM_T* _this) {
	return _this->k;
}

EDouble lStarKtcAlgorithm_getStretchFactor(LSTARKTCALGORITHM_T* _this) {
	return _this->stretchFactor;
}

// --- End of l*-kTC-specific methods// --- End of user-defined helpers

void** pattern_LStarKtcAlgorithm_0_1_IdentifyLinksToBeInactivated_blackBFFFFFF(LSTARKTCALGORITHM_T* _this) {
	NODE_T* this_node = lStarKtcAlgorithm_getNode(_this);
	if (this_node != NULL) {
		EDouble this_k = lStarKtcAlgorithm_getK(_this);
		EDouble this_stretchFactor = lStarKtcAlgorithm_getStretchFactor(_this);
		EInt this_node_hopcount = node_getHopcount(this_node);
		LINK_T* e13;
		list_t list_e13_this_node_outgoingLinks = node_getOutgoingLinks(this_node);
		for (e13 = list_head_pred(list_e13_this_node_outgoingLinks,this_node,&node_isOutgoingLinks); e13!=NULL; e13=list_item_next_pred(e13,this_node,&node_isOutgoingLinks)) {
			NODE_T* n3 = link_getTarget(e13);
			if (n3 != NULL) {
				if (!node_equals(n3, this_node)) {
					EDouble e13_weight = link_getWeight(e13);
					EInt n3_hopcount = node_getHopcount(n3);
					LINK_T* e12;
					list_t list_e12_this_node_outgoingLinks = node_getOutgoingLinks(this_node);
					for (e12 = list_head_pred(list_e12_this_node_outgoingLinks,this_node,&node_isOutgoingLinks); e12!=NULL; e12=list_item_next_pred(e12,this_node,&node_isOutgoingLinks)) {
						if (!link_equals(e12, e13)) {
							NODE_T* n2 = link_getTarget(e12);
							if (n2 != NULL) {
								if (!node_equals(n2, this_node)) {
									if (!node_equals(n2, n3)) {
										LinkState e12_marked = link_getMarked(e12);
										if(linkState_equals(e12_marked, UNCLASSIFIED)){
											EDouble e12_weight = link_getWeight(e12);
											EInt n2_hopcount = node_getHopcount(n2);
											if(lStarKtcAlgorithm_evaluateHopcountConstraint(this_node_hopcount ,n2_hopcount ,n3_hopcount ,this_stretchFactor )){
											 LINK_T* e32;
											 list_t list_e32_n3_outgoingLinks = node_getOutgoingLinks(n3);
											 for (e32 = list_head_pred(list_e32_n3_outgoingLinks,n3,&node_isOutgoingLinks); e32!=NULL; e32=list_item_next_pred(e32,n3,&node_isOutgoingLinks)) {
											 	if (!link_equals(e13, e32)) {
											 		if (!link_equals(e12, e32)) {
											 			if (node_containsIncomingLinks(n2, e32)) {
											 				EDouble e32_weight = link_getWeight(e32);
											 				 EDouble maxWeight ;

											 				 maxWeight =e13_weight <e32_weight ?e32_weight :e13_weight ;
											 				 if(e12_weight >maxWeight ){
											 				   EDouble minWeight ;

											 				   minWeight =e13_weight <e32_weight ?e13_weight :e32_weight ;
											 				    EDouble kMinWeight ;

											 				    kMinWeight =minWeight *this_k ;
											 				    if(e12_weight >kMinWeight ){
											 				     void** _result = malloc(7*sizeof(void*));
											 				     _result[0]= _this;
											 				     _result[1]= this_node;
											 				     _result[2]= n2;
											 				     _result[3]= n3;
											 				     _result[4]= e13;
											 				     _result[5]= e12;
											 				     _result[6]= e32;
											 				      
											 				     return _result; }   } 

											 			}
											 		}
											 	}
											 } }


										}	

									}
								}
							}

						}
					}


				}
			}

		}



	}

	return NULL;
}

void** pattern_LStarKtcAlgorithm_0_2_InactivateLinks_blackB(LINK_T* e12) {
	void** _result = malloc(1*sizeof(void*));
	_result[0]= e12;
	 
	return _result;
}

void** pattern_LStarKtcAlgorithm_0_2_InactivateLinks_greenB(LINK_T* e12) {
	LinkState e12_marked_prime = INACTIVE;
	link_setMarked(e12, e12_marked_prime);
	void** _result = malloc(1*sizeof(void*));
	_result[0]= e12;
	 
	return _result;
}

void** pattern_LStarKtcAlgorithm_0_3_IdentifyRemainingUnclassifiedEdges_blackBFF(LSTARKTCALGORITHM_T* _this) {
	NODE_T* this_node = lStarKtcAlgorithm_getNode(_this);
	if (this_node != NULL) {
		LINK_T* e12;
		list_t list_e12_this_node_outgoingLinks = node_getOutgoingLinks(this_node);
		for (e12 = list_head_pred(list_e12_this_node_outgoingLinks,this_node,&node_isOutgoingLinks); e12!=NULL; e12=list_item_next_pred(e12,this_node,&node_isOutgoingLinks)) {
			void** _result = malloc(3*sizeof(void*));
			_result[0]= _this;
			_result[1]= this_node;
			_result[2]= e12;
			 
			return _result;
		}
	}

	return NULL;
}

void** pattern_LStarKtcAlgorithm_0_3_IdentifyRemainingUnclassifiedEdges_greenB(LINK_T* e12) {
	LinkState e12_marked_prime = UNCLASSIFIED;
	link_setMarked(e12, e12_marked_prime);
	void** _result = malloc(1*sizeof(void*));
	_result[0]= e12;
	 
	return _result;
}

void** pattern_LStarKtcAlgorithm_0_4_ActivateEdge_blackB(LINK_T* e12) {
	void** _result = malloc(1*sizeof(void*));
	_result[0]= e12;
	 
	return _result;
}

void** pattern_LStarKtcAlgorithm_0_4_ActivateEdge_greenB(LINK_T* e12) {
	LinkState e12_marked_prime = ACTIVE;
	link_setMarked(e12, e12_marked_prime);
	void** _result = malloc(1*sizeof(void*));
	_result[0]= e12;
	 
	return _result;
}


void lStarKtcAlgorithm_run(LSTARKTCALGORITHM_T* this){
	// IdentifyLinksToBeInactivated
	void** result1_black = pattern_LStarKtcAlgorithm_0_1_IdentifyLinksToBeInactivated_blackBFFFFFF(this);
	while (result1_black != NULL) {
		// NODE_T* this_node = (NODE_T*) result1_black[1];
		// NODE_T* n2 = (NODE_T*) result1_black[2];
		// NODE_T* n3 = (NODE_T*) result1_black[3];
		// LINK_T* e13 = (LINK_T*) result1_black[4];
		LINK_T* e12 = (LINK_T*) result1_black[5];
		// LINK_T* e32 = (LINK_T*) result1_black[6];
		free(result1_black);
	
		// InactivateLinks
		void** result2_black = pattern_LStarKtcAlgorithm_0_2_InactivateLinks_blackB(e12);
		if (result2_black == NULL) {
			printf("Pattern matching in node [InactivateLinks] failed.");
			printf("Variables: [e12]");
			exit(-1);
		}
		free(result2_black);
		void** result2_green = pattern_LStarKtcAlgorithm_0_2_InactivateLinks_greenB(e12);
		free(result2_green);
	
	
		free(result1_black);
		result1_black = pattern_LStarKtcAlgorithm_0_1_IdentifyLinksToBeInactivated_blackBFFFFFF(this);
	}
	// IdentifyRemainingUnclassifiedEdges
	void** result3_black = pattern_LStarKtcAlgorithm_0_3_IdentifyRemainingUnclassifiedEdges_blackBFF(this);
	while (result3_black != NULL) {
		// NODE_T* this_node = (NODE_T*) result3_black[1];
		LINK_T* e12 = (LINK_T*) result3_black[2];
		free(result3_black);
		void** result3_green = pattern_LStarKtcAlgorithm_0_3_IdentifyRemainingUnclassifiedEdges_greenB(e12);
		free(result3_green);
	
	
		// ActivateEdge
		void** result4_black = pattern_LStarKtcAlgorithm_0_4_ActivateEdge_blackB(e12);
		if (result4_black == NULL) {
			printf("Pattern matching in node [ActivateEdge] failed.");
			printf("Variables: [e12]");
			exit(-1);
		}
		free(result4_black);
		void** result4_green = pattern_LStarKtcAlgorithm_0_4_ActivateEdge_greenB(e12);
		free(result4_green);
	
	
		free(result3_black);
		result3_black = pattern_LStarKtcAlgorithm_0_3_IdentifyRemainingUnclassifiedEdges_blackBFF(this);
	}
	return;

}
void init(){
}

#if COMPONENT_TOPOLOGYCONTROL == COMPONENT_TOPOLOGYCONTROL_CMOFLONDEMOLANGUAGE2_C
PROCESS(component_topologycontrol, "topologycontrol: cMoflonDemoLanguage2_C");
PROCESS_THREAD(component_topologycontrol, ev, data) {
	PROCESS_BEGIN();

	BOOT_COMPONENT_WAIT(component_topologycontrol);
	messagetype_hopcount = uniqueid_assign();
		component_network_packet_subscribe(messagetype_hopcount, _hopcount_recv);

		static struct etimer etimer_hopcountstart;
		etimer_set(&etimer_hopcountstart, CLOCK_SECOND * 90);

		// static struct etimer etimer_broadcast
		etimer_set(&etimer_broadcast, CLOCK_SECOND * 500); // unimportant high value, real interval is set later in time

		static struct etimer etimer_hopcountupdate;
		etimer_set(&etimer_hopcountupdate, CLOCK_SECOND * 30);static struct etimer waittime;
	etimer_set(&waittime, CLOCK_SECOND * COMPONENT_TOPOLOGYCONTROL_CMOFLONDEMOLANGUAGE2_C_UPDATEINTERVAL);
	while(1) {
		PROCESS_WAIT_EVENT();init();
		// a small delay for the base station to start broadcasting that all motes have booted and can receive this information
		if(etimer_expired(&etimer_hopcountstart)) {
					if(networkaddr_equal(component_network_address_basestation(), networkaddr_node_addr())) {
						printf("DEBUG: [topologycontrol-lktc] updated hopcount to 0 hops\n");
						my_hopcount = 0;

						etimer_set(&etimer_broadcast, random(CLOCK_SECOND * COMPONENT_TOPOLOGYCONTROL_CMOFLONDEMOLANGUAGE2_C_BROADCASTHOPCOUNT_IMMEDIATE_MIN, CLOCK_SECOND * COMPONENT_TOPOLOGYCONTROL_CMOFLONDEMOLANGUAGE2_C_BROADCASTHOPCOUNT_IMMEDIATE_MAX));
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
						etimer_set(&etimer_broadcast, random(CLOCK_SECOND * COMPONENT_TOPOLOGYCONTROL_CMOFLONDEMOLANGUAGE2_C_BROADCASTHOPCOUNT_IMMEDIATE_MIN, CLOCK_SECOND * COMPONENT_TOPOLOGYCONTROL_CMOFLONDEMOLANGUAGE2_C_BROADCASTHOPCOUNT_IMMEDIATE_MAX));
					}
				}
				if(etimer_expired(&waittime)) {
					etimer_reset(&waittime);
		watchdog_stop();

		prepareLinks();
		LSTARKTCALGORITHM_T tc;
		tc.node =  networkaddr_node_addr();
		tc.k = COMPONENT_TOPOLOGYCONTROL_CMOFLONDEMOLANGUAGE2_C_K;
		tc.stretchFactor = COMPONENT_TOPOLOGYCONTROL_CMOFLONDEMOLANGUAGE2_C_STRETCHFACTOR;
		list_t neighbors= component_neighbordiscovery_neighbors();
		neighbor_t* link;
		int degree=0;
		for(link=list_head(neighbors);link!=NULL;link=list_item_next(link)){
			if(networkaddr_equal(link->node1,networkaddr_node_addr())||networkaddr_equal(link->node2,networkaddr_node_addr()))
				degree++;
		}
		printf("[topologycontrol]: DEGREE: %d\n",degree);
		unsigned long start=RTIMER_NOW();
		printf("[topologycontrol]: STATUS: Run\n");
		lStarKtcAlgorithm_run(&tc);
		printf("[topologycontrol]: TIME: %lu\n",RTIMER_NOW()-start);
		LINK_T* onehop;
		for(onehop = list_head(component_neighbordiscovery_neighbors()); onehop != NULL; onehop = list_item_next(onehop)) {
			if(networkaddr_equal(onehop->node1, networkaddr_node_addr()) && onehop->weight_node1_to_node2 == COMPONENT_NEIGHBORDISCOVERY_WEIGHTUNKNOWN) {
				component_network_ignoredlinks_add(onehop->node2);
			}
		}

		watchdog_start();
		}
	}
	PROCESS_END();
}

#endif
