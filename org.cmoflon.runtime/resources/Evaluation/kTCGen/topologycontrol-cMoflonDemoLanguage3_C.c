#include "topologycontrol-cMoflonDemoLanguage3_C.h"


#define min(a,b) (((a)<(b))?(a):(b))
#define max(a,b) (((a)>(b))?(a):(b))

void* list_head_pred(list_t list, void* _this, bool(*pred)(void*,void*)) {
	void* item;
	for (item = list_head(list); item != NULL;item=list_item_next(item)) {
		if (pred(item,_this))
			return item;
	}
	return NULL;
}

void* list_item_next_pred(void* item, void* _this, bool(*pred)(void*, void*)) {
	for (item = list_item_next(item); item != NULL; item= list_item_next(item)) {
		if (pred(item,_this)) {
			return item;
		}
	}
	return NULL;
}

void prepareLinks(){
	LINK_T* link;
	for (link = list_head(component_neighbordiscovery_neighbors()); link != NULL; link = list_item_next(link)) {
		link->state = UNCLASSIFIED;
	}
}

//Begin of non SDM implemented methods
EBoolean lstarktcalgorithm_evaluateHopcountConstraint(LSTARKTCALGORITHM_T* this, EInt hopCount1, EInt hopCount2, EInt hopCount3, EDouble stretchFactor) {
	//!a&&b
	if (min(hopCount1, min(hopCount2, hopCount3)) < 0)
		return false;
	bool result = true;
	result &= (!(hopCount1 == hopCount2) || true);
	result &= (!(hopCount1 > hopCount2) || ((hopCount3 + 1) * 1.0 / max(1, hopCount1) < stretchFactor));
	result &= (!(hopCount1 < hopCount2) || ((hopCount3 + 1) * 1.0 / max(1, hopCount2) < stretchFactor));
	return result;
}

void lmstalgorithm_prepareLMSTEntries(LMSTALGORITHM_T* this){
	LMST_T* lmst= (LMST_T*)malloc(sizeof(LMST_T));
	lmst->algo = this;
	MEMB(memb_lmstEntries, LMSTENTRY_T, MAX_MATCH_COUNT);
	memb_init(&memb_lmstEntries);
	lmst->mem = &memb_lmstEntries;
	LIST(list_lmst_entries);
	list_init(list_lmst_entries);

	// add all nodes to list
	LINK_T* item_neighbor;
	for (item_neighbor = list_head(component_neighbordiscovery_neighbors()); item_neighbor != NULL; item_neighbor = list_item_next(item_neighbor)) {
		LMSTENTRY_T *item_node = (LMSTENTRY_T*)malloc(sizeof(LMSTENTRY_T));
		bool found;

		// check for node1
		found = false;
		for (item_node = list_head(list_lmst_entries); item_node != NULL; item_node = list_item_next(item_node)) {
			if (networkaddr_equal(item_neighbor->node1, item_node->node)) {
				found = true;
				break;
			}
		}
		if (!found) {
			if ((item_node = memb_alloc(&memb_lmstEntries)) == NULL) {
				printf("ERROR[topologycontrol-lmst]: nodelist is full\n");
			}
			else {
				item_node->node = item_neighbor->node1;
				item_node->selectedLink = NULL;
				item_node->algorithm = lmst;
				if (networkaddr_equal(networkaddr_node_addr(), item_neighbor->node1))
					item_node->isInTree = true;
				else item_node->isInTree = false;
				list_add(list_lmst_entries, item_node);
			}
		}

		// check for node2
		found = false;
		for (item_node = list_head(list_lmst_entries); item_node != NULL; item_node = list_item_next(item_node)) {
			if (networkaddr_equal(item_neighbor->node2, item_node->node)) {
				found = true;
				break;
			}
		}
		if (!found) {
			if ((item_node = memb_alloc(&memb_lmstEntries)) == NULL) {
				printf("ERROR[topologycontrol-lmst]: nodelist is full\n");
			}
			else {
				item_node->node = item_neighbor->node2;
				item_node->selectedLink = NULL;
				item_node->algorithm = lmst;
				if (networkaddr_equal(networkaddr_node_addr(), item_neighbor->node2))
					item_node->isInTree = true;
				else item_node->isInTree = false;
				list_add(list_lmst_entries, item_node);
			}
		}
	}
	lmst->lmstEntries = list_lmst_entries;
	this->lmst = lmst;
};


void lmstalgorithm_cleanupLMST(LMSTALGORITHM_T* this) {
	list_t lmst = this->lmst->lmstEntries;
	// add all nodes to list
	LMSTENTRY_T* item_neighbor;
	for (item_neighbor = list_head(lmst); item_neighbor != NULL; item_neighbor = list_item_next(item_neighbor)) {
		free(item_neighbor);
		memb_free(this->lmst->mem, list_pop(lmst));
	}
	free(lmst);
}
//End of non SDM implemented methods

//Begin of declarations for hopcount
EInt node_getHopcount(NODE_T* _this){
	if (networkaddr_equal(networkaddr_node_addr(), _this))
		return my_hopcount;

	hopcount_t *item;
	for (item = list_head(list_hopcount); item != NULL; item = list_item_next(item)) {
		if (networkaddr_equal(item->address, _this)) {
			return item->hopcount;
		}
	}

	return -1;
}
//End of declarations for hopcount

//Begin of declarations for incomingLinks
list_t node_getIncomingLinks(NODE_T* _this) {
	return component_neighbordiscovery_neighbors();
}

bool node_containsIncomingLinks(NODE_T* _this, LINK_T* value) {
	LINK_T* link;
	for (link = list_head_pred(component_neighbordiscovery_neighbors(),_this,&node_isIncomingLinks); link != NULL; link = list_item_next_pred(link,_this,&node_isIncomingLinks)) {
		if (link_equals(value, link))
			return true;
	}
	return false;
}

bool node_isIncomingLinks(void* candidate, void* _this) {
	if (node_equals((NODE_T*)_this, ((LINK_T*)candidate)->node2) && ((LINK_T*)candidate)->weight_node2_to_node1 != COMPONENT_NEIGHBORDISCOVERY_WEIGHTUNKNOWN) {
		return true;
	}
	else return false;
}
//End of declarations for incomingLinks

//Begin of declarations for outgoingLinks
list_t node_getOutgoingLinks(NODE_T* _this) {
	return component_neighbordiscovery_neighbors();
}

bool node_containsOutgoingLinks(NODE_T* _this, LINK_T* value) {
	LINK_T* link;
	for (link = list_head_pred(component_neighbordiscovery_neighbors(),_this,&node_isOutgoingLinks); link != NULL; link = list_item_next_pred(link, _this, &node_isOutgoingLinks)) {
		if (link_equals(value, link))
			return true;
	}
	return false;
}

bool node_isOutgoingLinks(void* candidate, void* _this) {
	if (node_equals((NODE_T*)_this, ((LINK_T*)candidate)->node1) && ((LINK_T*)candidate)->weight_node1_to_node2 != COMPONENT_NEIGHBORDISCOVERY_WEIGHTUNKNOWN) {
		return true;
	}
	else return false;
}

//End of declarations for outgoingLinks

//Begin of declarations for neighborhood
list_t node_getNeighborhood(NODE_T* _this) {
	return component_neighbordiscovery_neighbors();
}
bool node_isNeighborhood(void* candidate, void* _this) {
	return true;
}
//End of declarations for neighborhood

//Begin of declarations for incidentLinks
list_t node_getIncidentLinks(NODE_T* _this) {
	return component_neighbordiscovery_neighbors();
}
bool node_containsIncidentLinks(NODE_T* _this, LINK_T* value) {
	if (node_equals(_this, value->node1) || node_equals(_this, value->node2))
		return true;
	else return false;
}
bool node_isIncidentLinks(void* candidate, void* _this) {
	if (node_equals((NODE_T*)_this, ((LINK_T*)candidate)->node1) || node_equals((NODE_T*)_this, ((LINK_T*)candidate)->node2))
		return true;
	else return false;
};
//End of declarations for incidentLinks

//Begin of declarations for marked
LinkState link_getMarked(LINK_T* _this) {
	return _this->state;
}
void link_setMarked(LINK_T* _this, LinkState value) {
	_this->state = value;
	if (value==INACTIVE) {
		if (node_equals(networkaddr_node_addr(), _this->node1)) {
			component_network_ignoredlinks_add(_this->node2);
		}
		else
			if (node_equals(networkaddr_node_addr(), _this->node2))
				component_network_ignoredlinks_add(_this->node1);
	}
	if (value == ACTIVE) {
		if (node_equals(networkaddr_node_addr(), _this->node1)) {
			component_network_ignoredlinks_remove(_this->node2);
		}
		else
			if (node_equals(networkaddr_node_addr(), _this->node2))
				component_network_ignoredlinks_remove(_this->node1);
	}
	//IF this node is not part of the edge don't ignore any of the nodes
}
//End of declarations for marked

//Begin of declarations for weight
EDouble link_getWeight(LINK_T* _this) {
	if (_this->weight_node1_to_node2 != COMPONENT_NEIGHBORDISCOVERY_WEIGHTUNKNOWN) {
		return _this->weight_node1_to_node2;
	}
	else if (_this->weight_node2_to_node1 != COMPONENT_NEIGHBORDISCOVERY_WEIGHTUNKNOWN) {
		return _this->weight_node2_to_node1;
	}
	else return COMPONENT_NEIGHBORDISCOVERY_WEIGHTUNKNOWN;
}
//End of declarations for weight

//Begin of declarations for target
NODE_T* link_getTarget(LINK_T* _this) {
	return _this->node2;
}
//End of declarations for target

//Begin of declarations for source
NODE_T* link_getSource(LINK_T* _this) {
	return _this->node1;
}
//End of declarations for source

//Begin of declarations for lmst
LMST_T* lmstentry_getLmst(LMSTENTRY_T* _this) {
	return _this->algorithm;
}
//End of declarations for lmst

//Begin of declarations for node
NODE_T* lmstentry_getNode(LMSTENTRY_T* _this) {
	return _this->node;
}
void lmstentry_setNode(LMSTENTRY_T* _this, NODE_T* value) {
	_this->node = value;
	return;
}
//End of declarations for node

//Begin of declarations for selectedLink
LINK_T* lmstentry_getSelectedLink(LMSTENTRY_T* _this) {
	return _this->selectedLink;
}
void lmstentry_setSelectedLink(LMSTENTRY_T* _this, LINK_T* value) {
	_this->selectedLink=value;
}
//End of declarations for selectedLink

//Begin of declarations for isInTree
bool lmstentry_isIsInTree(LMSTENTRY_T* _this) {
	return _this->isInTree;
}
void lmstentry_setIsInTree(LMSTENTRY_T* _this, EBoolean value) {
	_this->isInTree = value;
}
//End of declarations for isInTree


//Begin of declarations for lmstEntries
list_t lmst_getLmstEntries(LMST_T* _this) {
	return _this->lmstEntries;
}
bool lmst_isLmstEntries(void* candidate, void* _this) {
	return true;
}
//End of declarations for lmstEntries

//Begin of declarations for node
NODE_T* maxpoweralgorithm_getNode(MAXPOWERALGORITHM_T* _this) {
	return _this->node;
};
//End of declarations for node

//Begin of declarations for k
EDouble ktcalgorithm_getK(KTCALGORITHM_T* _this) {
	return _this->k;
};
//End of declarations for k

//Begin of declarations for node
NODE_T* ktcalgorithm_getNode(KTCALGORITHM_T* _this) {
	return _this->node;
};
//End of declarations for node

//Begin of declarations for k
EDouble lstarktcalgorithm_getK(LSTARKTCALGORITHM_T* _this) {
	return _this->k;
};
//End of declarations for k

//Begin of declarations for node
NODE_T* lstarktcalgorithm_getNode(LSTARKTCALGORITHM_T* _this) {
	return _this->node;
};
//End of declarations for node

//Begin of declarations for stretchFactor
EDouble lstarktcalgorithm_getStretchFactor(LSTARKTCALGORITHM_T* _this) {
	return _this->stretchFactor;
};
//End of declarations for stretchFactor

//Begin of declarations for node
NODE_T* lmstalgorithm_getNode(LMSTALGORITHM_T* _this) {
	return _this->node;
};
//End of declarations for node

//Begin of declarations for lmst
LMST_T* lmstalgorithm_getLmst(LMSTALGORITHM_T* _this) {
	return _this->lmst;
}
//End of declarations for lmst

//Begin of compare declarations
int edouble_compare(EDouble _this, EDouble other) {
	if (_this == COMPONENT_NEIGHBORDISCOVERY_WEIGHTUNKNOWN)
		return 1;
	if (other == COMPONENT_NEIGHBORDISCOVERY_WEIGHTUNKNOWN)
		return -1;
	int result = (_this<other) ? -1 : (_this>other) ? 1 : 0;
	return result;
}
//End of compare declarations

//Begin of equals declarations
bool node_equals(NODE_T* _this, NODE_T* other) {
	return networkaddr_equal(_this, other);
}
bool link_equals(LINK_T* _this, LINK_T* other) {
	return((node_equals(_this->node1, other->node1) && node_equals(_this->node2, other->node2)) || (node_equals(_this->node1, other->node2) && node_equals(_this->node2, other->node1)));

}
bool linkstate_equals(LinkState s1, LinkState s2) {
	return s1 == s2;
}

bool lmstentry_equals(LMSTENTRY_T* _this, LMSTENTRY_T* other) {
	bool result = true;
	result&=node_equals(_this->node, other->node);
	result&=link_equals(_this->selectedLink, other->selectedLink);
	return result;
}

bool eboolean_equals(EBoolean b1, EBoolean b2) {
	return b1 == b2;
}
//End of equals declarations
void** pattern_kTCAlgorithm_0_1_IdentifyLinksToBeInactivated_blackBFFFFFF(KTCALGORITHM_T* _this) {
	NODE_T* this_node = ktcalgorithm_getNode(_this);
	if (this_node != NULL) {
		EDouble this_k = ktcalgorithm_getK(_this);
		LINK_T* e12;
		list_t list_e12_this_node_outgoinglinks = node_getOutgoingLinks(this_node);
		for (e12 = list_head_pred(list_e12_this_node_outgoinglinks,this_node,&node_isOutgoingLinks); e12!=NULL; e12=list_item_next_pred(e12,this_node,&node_isOutgoingLinks)) {
			NODE_T* n2 = link_getTarget(e12);
			if (n2 != NULL) {
				if (!node_equals(n2, this_node)) {
					LinkState e12_marked = link_getMarked(e12);
					if(linkstate_equals(e12_marked, UNCLASSIFIED)){
						EDouble e12_weight = link_getWeight(e12);
						LINK_T* e13;
						list_t list_e13_this_node_outgoinglinks = node_getOutgoingLinks(this_node);
						for (e13 = list_head_pred(list_e13_this_node_outgoinglinks,this_node,&node_isOutgoingLinks); e13!=NULL; e13=list_item_next_pred(e13,this_node,&node_isOutgoingLinks)) {
							if (!link_equals(e12, e13)) {
								NODE_T* n3 = link_getTarget(e13);
								if (n3 != NULL) {
									if (!node_equals(n2, n3)) {
										if (!node_equals(n3, this_node)) {
											EDouble e13_weight = link_getWeight(e13);
											LINK_T* e32;
											list_t list_e32_n2_incominglinks = node_getIncomingLinks(n2);
											for (e32 = list_head_pred(list_e32_n2_incominglinks,n2,&node_isIncomingLinks); e32!=NULL; e32=list_item_next_pred(e32,n2,&node_isIncomingLinks)) {
												if (!link_equals(e12, e32)) {
													if (!link_equals(e13, e32)) {
														if (node_containsOutgoingLinks(n3, e32)) {
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
															     _result[2]= e12;
															     _result[3]= n2;
															     _result[4]= n3;
															     _result[5]= e32;
															     _result[6]= e13;
															      
															     return _result; }   } 

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

				}
			}

		}

	}

	return NULL;
}

void** pattern_kTCAlgorithm_0_2_InactivateLinks_blackB(LINK_T* e12) {
	void** _result = malloc(1*sizeof(void*));
	_result[0]= e12;
	 
	return _result;
}

void** pattern_kTCAlgorithm_0_2_InactivateLinks_greenB(LINK_T* e12) {
	LinkState e12_marked_prime = INACTIVE;
	link_setMarked(e12, e12_marked_prime);
	void** _result = malloc(1*sizeof(void*));
	_result[0]= e12;
	 
	return _result;
}

void** pattern_kTCAlgorithm_0_3_IdentifyRemainingUnclassifiedEdges_blackBFF(KTCALGORITHM_T* _this) {
	NODE_T* this_node = ktcalgorithm_getNode(_this);
	if (this_node != NULL) {
		LINK_T* e12;
		list_t list_e12_this_node_outgoinglinks = node_getOutgoingLinks(this_node);
		for (e12 = list_head_pred(list_e12_this_node_outgoinglinks,this_node,&node_isOutgoingLinks); e12!=NULL; e12=list_item_next_pred(e12,this_node,&node_isOutgoingLinks)) {
			LinkState e12_marked = link_getMarked(e12);
			if(linkstate_equals(e12_marked, UNCLASSIFIED)){
				void** _result = malloc(3*sizeof(void*));
				_result[0]= _this;
				_result[1]= this_node;
				_result[2]= e12;
				 
				return _result;
			}	

		}
	}

	return NULL;
}

void** pattern_kTCAlgorithm_0_4_ActivateEdge_blackB(LINK_T* e12) {
	void** _result = malloc(1*sizeof(void*));
	_result[0]= e12;
	 
	return _result;
}

void** pattern_kTCAlgorithm_0_4_ActivateEdge_greenB(LINK_T* e12) {
	LinkState e12_marked_prime = ACTIVE;
	link_setMarked(e12, e12_marked_prime);
	void** _result = malloc(1*sizeof(void*));
	_result[0]= e12;
	 
	return _result;
}


void ktcalgorithm_run(KTCALGORITHM_T* this){
	// IdentifyLinksToBeInactivated	void** result1_black = pattern_kTCAlgorithm_0_1_IdentifyLinksToBeInactivated_blackBFFFFFF(this);	while (result1_black != NULL) {		// NODE_T* this_node = (NODE_T*) result1_black[1];		LINK_T* e12 = (LINK_T*) result1_black[2];		// NODE_T* n2 = (NODE_T*) result1_black[3];		// NODE_T* n3 = (NODE_T*) result1_black[4];		// LINK_T* e32 = (LINK_T*) result1_black[5];		// LINK_T* e13 = (LINK_T*) result1_black[6];		free(result1_black);			// InactivateLinks		void** result2_black = pattern_kTCAlgorithm_0_2_InactivateLinks_blackB(e12);		if (result2_black == NULL) {			printf("Pattern matching in node [InactivateLinks] failed.");			printf("Variables: [e12]");			exit(-1);		}		free(result2_black);		void** result2_green = pattern_kTCAlgorithm_0_2_InactivateLinks_greenB(e12);		free(result2_green);				free(result1_black);		result1_black = pattern_kTCAlgorithm_0_1_IdentifyLinksToBeInactivated_blackBFFFFFF(this);	}	// IdentifyRemainingUnclassifiedEdges	void** result3_black = pattern_kTCAlgorithm_0_3_IdentifyRemainingUnclassifiedEdges_blackBFF(this);	while (result3_black != NULL) {		// NODE_T* this_node = (NODE_T*) result3_black[1];		LINK_T* e12 = (LINK_T*) result3_black[2];		free(result3_black);			// ActivateEdge		void** result4_black = pattern_kTCAlgorithm_0_4_ActivateEdge_blackB(e12);		if (result4_black == NULL) {			printf("Pattern matching in node [ActivateEdge] failed.");			printf("Variables: [e12]");			exit(-1);		}		free(result4_black);		void** result4_green = pattern_kTCAlgorithm_0_4_ActivateEdge_greenB(e12);		free(result4_green);				free(result3_black);		result3_black = pattern_kTCAlgorithm_0_3_IdentifyRemainingUnclassifiedEdges_blackBFF(this);	}	return;
}
void init(){
}

#if COMPONENT_TOPOLOGYCONTROL == COMPONENT_TOPOLOGYCONTROL_CMOFLONDEMOLANGUAGE3_C
PROCESS(component_topologycontrol, "topologycontrol: cMoflonDemoLanguage3_C");
PROCESS_THREAD(component_topologycontrol, ev, data) {
	PROCESS_BEGIN();

	BOOT_COMPONENT_WAIT(component_topologycontrol);
	static struct etimer waittime;
	etimer_set(&waittime, CLOCK_SECOND * COMPONENT_TOPOLOGYCONTROL_CMOFLONDEMOLANGUAGE3_C_UPDATEINTERVAL);
	while(1) {
		PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&waittime));
				etimer_reset(&waittime);init();
		// a small delay for the base station to start broadcasting that all motes have booted and can receive this information
		watchdog_stop();

		prepareLinks();
		KTCALGORITHM_T tc;
		tc.node =  networkaddr_node_addr();
		tc.k = COMPONENT_TOPOLOGYCONTROL_CMOFLONDEMOLANGUAGE3_C_K;
		ktcalgorithm_run(&tc);
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
