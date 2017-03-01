#include "topologycontrol-cMoflonDemoLanguage1_C-KtcAlgorithm.h"


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

bool link_isWeightDefined(EDouble weight)
{
	return weight != COMPONENT_NEIGHBORDISCOVERY_WEIGHTUNKNOWN;
}

EDouble link_getWeight(LINK_T* _this) {
		return _this->weight_node1_to_node2;
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

// --- Begin of user-defined algorithm-independent helpers (Path: 'injection/custom-helpers.c')
// Custom helpers
// --- End of user-defined algorithm-independent helpers

// --- Begin of user-defined helpers for KtcAlgorithm (Path: 'injection/custom-helpers_KtcAlgorithm.c')
EDouble ktcAlgorithm_getK(KTCALGORITHM_T* _this) {
	return _this->k;
}

NODE_T* ktcAlgorithm_getNode(KTCALGORITHM_T* _this) {
	return _this->node;
}
// --- End of user-defined helpers for KtcAlgorithm

void** pattern_KtcAlgorithm_0_1_IdentifyLinksToBeInactivated_blackBFFFFFF(KTCALGORITHM_T* _this) {
	NODE_T* this_node = ktcAlgorithm_getNode(_this);
	if (this_node != NULL) {
		EDouble this_k = ktcAlgorithm_getK(_this);
		LINK_T* e12;
		list_t list_e12_this_node_outgoingLinks = node_getOutgoingLinks(this_node);
		for (e12 = list_head_pred(list_e12_this_node_outgoingLinks,this_node,&node_isOutgoingLinks); e12!=NULL; e12=list_item_next_pred(e12,this_node,&node_isOutgoingLinks)) {
			NODE_T* n2 = link_getTarget(e12);
			if (n2 != NULL) {
				if (!node_equals(n2, this_node)) {
					LinkState e12_marked = link_getMarked(e12);
					if(linkState_equals(e12_marked, UNCLASSIFIED)){
						EDouble e12_weight = link_getWeight(e12);
						if(link_isWeightDefined(e12_weight )){
						 LINK_T* e13;
						 list_t list_e13_this_node_outgoingLinks = node_getOutgoingLinks(this_node);
						 for (e13 = list_head_pred(list_e13_this_node_outgoingLinks,this_node,&node_isOutgoingLinks); e13!=NULL; e13=list_item_next_pred(e13,this_node,&node_isOutgoingLinks)) {
						 	if (!link_equals(e12, e13)) {
						 		NODE_T* n3 = link_getTarget(e13);
						 		if (n3 != NULL) {
						 			if (!node_equals(n2, n3)) {
						 				if (!node_equals(n3, this_node)) {
						 					EDouble e13_weight = link_getWeight(e13);
						 					if(link_isWeightDefined(e13_weight )){
						 					 LINK_T* e32;
						 					 list_t list_e32_n2_incomingLinks = node_getIncomingLinks(n2);
						 					 for (e32 = list_head_pred(list_e32_n2_incomingLinks,n2,&node_isIncomingLinks); e32!=NULL; e32=list_item_next_pred(e32,n2,&node_isIncomingLinks)) {
						 					 	if (!link_equals(e12, e32)) {
						 					 		if (!link_equals(e13, e32)) {
						 					 			if (node_containsOutgoingLinks(n3, e32)) {
						 					 				EDouble e32_weight = link_getWeight(e32);
						 					 				if(link_isWeightDefined(e32_weight )){
						 					 				  EDouble maxWeight ;

						 					 				  maxWeight =e13_weight <e32_weight ?e32_weight :e13_weight ;
						 					 				  if(e12_weight >maxWeight ){
						 					 				    EDouble minWeight ;

						 					 				    minWeight =e13_weight <e32_weight ?e13_weight :e32_weight ;
						 					 				     EDouble kMinWeight ;

						 					 				     kMinWeight =minWeight *this_k ;
						 					 				     if(e12_weight >kMinWeight ){
						 					 				      void** _result;
						 					 				      if((_result = malloc(7*sizeof(void*)))==NULL){
						 					 				      	printf("ERROR[topologycontrol]: could not allocate memory\n");
						 					 				      	return NULL;
						 					 				      }else{
						 					 				      	_result[0]= _this;
						 					 				      	_result[1]= this_node;
						 					 				      	_result[2]= e12;
						 					 				      	_result[3]= n2;
						 					 				      	_result[4]= n3;
						 					 				      	_result[5]= e32;
						 					 				      	_result[6]= e13;
						 					 				       
						 					 				      	return _result;
						 					 				      } }   }  }

						 					 			}
						 					 		}
						 					 	}
						 					 } }

						 				}
						 			}
						 		}

						 	}
						 } }

					}	

				}
			}

		}

	}

	return NULL;
}

void** pattern_KtcAlgorithm_0_2_InactivateLinks_blackB(LINK_T* e12) {
	void** _result;
	if((_result = malloc(1*sizeof(void*)))==NULL){
		printf("ERROR[topologycontrol]: could not allocate memory\n");
		return NULL;
	}else{
		_result[0]= e12;
	 
		return _result;
	}
}

void** pattern_KtcAlgorithm_0_2_InactivateLinks_greenB(LINK_T* e12) {
	LinkState e12_marked_prime = INACTIVE;
	link_setMarked(e12, e12_marked_prime);
	void** _result;
	if((_result = malloc(1*sizeof(void*)))==NULL){
		printf("ERROR[topologycontrol]: could not allocate memory\n");
		return NULL;
	}else{
		_result[0]= e12;
	 
		return _result;
	}
}

void** pattern_KtcAlgorithm_0_3_IdentifyRemainingUnclassifiedEdges_blackBFF(KTCALGORITHM_T* _this) {
	NODE_T* this_node = ktcAlgorithm_getNode(_this);
	if (this_node != NULL) {
		LINK_T* e12;
		list_t list_e12_this_node_outgoingLinks = node_getOutgoingLinks(this_node);
		for (e12 = list_head_pred(list_e12_this_node_outgoingLinks,this_node,&node_isOutgoingLinks); e12!=NULL; e12=list_item_next_pred(e12,this_node,&node_isOutgoingLinks)) {
			LinkState e12_marked = link_getMarked(e12);
			if(linkState_equals(e12_marked, UNCLASSIFIED)){
				void** _result;
				if((_result = malloc(3*sizeof(void*)))==NULL){
					printf("ERROR[topologycontrol]: could not allocate memory\n");
					return NULL;
				}else{
					_result[0]= _this;
					_result[1]= this_node;
					_result[2]= e12;
				 
					return _result;
				}
			}	

		}
	}

	return NULL;
}

void** pattern_KtcAlgorithm_0_4_ActivateEdge_blackB(LINK_T* e12) {
	void** _result;
	if((_result = malloc(1*sizeof(void*)))==NULL){
		printf("ERROR[topologycontrol]: could not allocate memory\n");
		return NULL;
	}else{
		_result[0]= e12;
	 
		return _result;
	}
}

void** pattern_KtcAlgorithm_0_4_ActivateEdge_greenB(LINK_T* e12) {
	LinkState e12_marked_prime = ACTIVE;
	link_setMarked(e12, e12_marked_prime);
	void** _result;
	if((_result = malloc(1*sizeof(void*)))==NULL){
		printf("ERROR[topologycontrol]: could not allocate memory\n");
		return NULL;
	}else{
		_result[0]= e12;
	 
		return _result;
	}
}


void ktcAlgorithm_run(KTCALGORITHM_T* this){
	// IdentifyLinksToBeInactivated
	void** result1_black = pattern_KtcAlgorithm_0_1_IdentifyLinksToBeInactivated_blackBFFFFFF(this);
	while (result1_black != NULL) {
		// NODE_T* this_node = (NODE_T*) result1_black[1];
		LINK_T* e12 = (LINK_T*) result1_black[2];
		// NODE_T* n2 = (NODE_T*) result1_black[3];
		// NODE_T* n3 = (NODE_T*) result1_black[4];
		// LINK_T* e32 = (LINK_T*) result1_black[5];
		// LINK_T* e13 = (LINK_T*) result1_black[6];
		free(result1_black);
	
		// InactivateLinks
		void** result2_black = pattern_KtcAlgorithm_0_2_InactivateLinks_blackB(e12);
		if (result2_black == NULL) {
			printf("Pattern matching in node [InactivateLinks] failed.");
			printf("Variables: [e12]");
			exit(-1);
		}
		free(result2_black);
		void** result2_green = pattern_KtcAlgorithm_0_2_InactivateLinks_greenB(e12);
		free(result2_green);
	
	
		result1_black = pattern_KtcAlgorithm_0_1_IdentifyLinksToBeInactivated_blackBFFFFFF(this);
	}
	// IdentifyRemainingUnclassifiedEdges
	void** result3_black = pattern_KtcAlgorithm_0_3_IdentifyRemainingUnclassifiedEdges_blackBFF(this);
	while (result3_black != NULL) {
		// NODE_T* this_node = (NODE_T*) result3_black[1];
		LINK_T* e12 = (LINK_T*) result3_black[2];
		free(result3_black);
	
		// ActivateEdge
		void** result4_black = pattern_KtcAlgorithm_0_4_ActivateEdge_blackB(e12);
		if (result4_black == NULL) {
			printf("Pattern matching in node [ActivateEdge] failed.");
			printf("Variables: [e12]");
			exit(-1);
		}
		free(result4_black);
		void** result4_green = pattern_KtcAlgorithm_0_4_ActivateEdge_greenB(e12);
		free(result4_green);
	
	
		result3_black = pattern_KtcAlgorithm_0_3_IdentifyRemainingUnclassifiedEdges_blackBFF(this);
	}
	return;

}
void init(){
}

#if COMPONENT_TOPOLOGYCONTROL == COMPONENT_TOPOLOGYCONTROL_CMOFLONDEMOLANGUAGE1_C_KTCALGORITHM
PROCESS(component_topologycontrol, "topologycontrol: KtcAlgorithm");
PROCESS_THREAD(component_topologycontrol, ev, data) {
	PROCESS_BEGIN();

	BOOT_COMPONENT_WAIT(component_topologycontrol);
	static struct etimer waittime;
	etimer_set(&waittime, CLOCK_SECOND * COMPONENT_TOPOLOGYCONTROL_KTCALGORITHM_UPDATEINTERVAL);
	while(1) {
		PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&waittime));
				etimer_reset(&waittime);init();
		// a small delay for the base station to start broadcasting that all motes have booted and can receive this information
		watchdog_stop();

		prepareLinks();
		KTCALGORITHM_T tc;
		tc.node =  networkaddr_node_addr();
		tc.k = COMPONENT_TOPOLOGYCONTROL_KTCALGORITHM_K;
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
		ktcAlgorithm_run(&tc);
		unsigned long finish=RTIMER_NOW();
		unsigned long runtime= finish>start? finish-start:start-finish;
		printf("[topologycontrol]: TIME: %lu\n",runtime);
		LINK_T* onehop;
		for(onehop = list_head(component_neighbordiscovery_neighbors()); onehop != NULL; onehop = list_item_next(onehop)) {
			if(networkaddr_equal(onehop->node1, networkaddr_node_addr()) && onehop->weight_node1_to_node2 == COMPONENT_NEIGHBORDISCOVERY_WEIGHTUNKNOWN) {
				component_network_ignoredlinks_add(onehop->node2);
			}
		}

		watchdog_start();
	}
	PROCESS_END();
}

#endif
