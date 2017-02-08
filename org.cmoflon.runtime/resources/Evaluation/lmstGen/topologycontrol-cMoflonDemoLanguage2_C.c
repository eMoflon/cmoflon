#include "topologycontrol-cMoflonDemoLanguage2_C.h"


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
void** pattern_LMSTAlgorithm_1_1_BindThis_blackBF(LMSTALGORITHM_T* _this) {
	NODE_T* self = lmstalgorithm_getNode(_this);
	if (self != NULL) {
		void** _result = malloc(2*sizeof(void*));
		_result[0]= _this;
		_result[1]= self;
		 
		return _result;
	}

	return NULL;
}

void pattern_LMSTAlgorithm_1_2_PrepareEntries_expressionB(LMSTALGORITHM_T* _this) {
	lmstalgorithm_prepareLMSTEntries(_this );

}

void** pattern_LMSTAlgorithm_1_3_FindMinimalOutgoingLink_bindingFB(LMSTALGORITHM_T* _this) {
	LINK_T* _localVariable_0 = lmstalgorithm_findShortestUnconnectedLink(_this);
	LINK_T* shortestUnconnectedLink = _localVariable_0;
	if (shortestUnconnectedLink != NULL) {
		void** _result = malloc(2*sizeof(void*));
		_result[0]= shortestUnconnectedLink;
		_result[1]= _this;
		 
		return _result; 
	}
	return NULL;
}

void** pattern_LMSTAlgorithm_1_3_FindMinimalOutgoingLink_blackB(LINK_T* shortestUnconnectedLink) {
	void** _result = malloc(1*sizeof(void*));
	_result[0]= shortestUnconnectedLink;
	 
	return _result;
}

void** pattern_LMSTAlgorithm_1_3_FindMinimalOutgoingLink_bindingAndBlackFB(LMSTALGORITHM_T* _this) {
	void** result_pattern_LMSTAlgorithm_1_3_FindMinimalOutgoingLink_binding = pattern_LMSTAlgorithm_1_3_FindMinimalOutgoingLink_bindingFB(_this);
	if (result_pattern_LMSTAlgorithm_1_3_FindMinimalOutgoingLink_binding != NULL) {
		LINK_T* shortestUnconnectedLink = (LINK_T*) result_pattern_LMSTAlgorithm_1_3_FindMinimalOutgoingLink_binding[0];
		  
		void** result_pattern_LMSTAlgorithm_1_3_FindMinimalOutgoingLink_black = pattern_LMSTAlgorithm_1_3_FindMinimalOutgoingLink_blackB(shortestUnconnectedLink);
		if (result_pattern_LMSTAlgorithm_1_3_FindMinimalOutgoingLink_black != NULL) {
			 
			void** _result = malloc(2*sizeof(void*));
			_result[0]= shortestUnconnectedLink;
			_result[1]= _this;
			 
			return _result;
		}
		free(result_pattern_LMSTAlgorithm_1_3_FindMinimalOutgoingLink_black);

	}
	free(result_pattern_LMSTAlgorithm_1_3_FindMinimalOutgoingLink_binding);

	return NULL;
}

void** pattern_LMSTAlgorithm_1_4_UpdateLMSTEntry_blackBFFFFFB(LMSTALGORITHM_T* _this, LINK_T* shortestUnconnectedLink) {
	LMST_T* lmst = lmstalgorithm_getLmst(_this);
	if (lmst != NULL) {
		NODE_T* node1 = link_getSource(shortestUnconnectedLink);
		if (node1 != NULL) {
			NODE_T* node2 = link_getTarget(shortestUnconnectedLink);
			if (node2 != NULL) {
				if (!node_equals(node1, node2)) {
					LMSTENTRY_T* lmstEntry2;
					list_t list_lmst_lmstEntry2 = lmst_getLmstEntries(lmst);
					for (lmstEntry2 = list_head_pred(list_lmst_lmstEntry2,lmst,&lmst_isLmstEntries); lmstEntry2!=NULL; lmstEntry2=list_item_next_pred(lmstEntry2,lmst,&lmst_isLmstEntries)) {
						if (node_equals(node2,lmstentry_getNode(lmstEntry2))) {
							LMSTENTRY_T* lmstEntry1;
							list_t list_lmst_lmstEntry1 = lmst_getLmstEntries(lmst);
							for (lmstEntry1 = list_head_pred(list_lmst_lmstEntry1,lmst,&lmst_isLmstEntries); lmstEntry1!=NULL; lmstEntry1=list_item_next_pred(lmstEntry1,lmst,&lmst_isLmstEntries)) {
								if (!lmstentry_equals(lmstEntry1, lmstEntry2)) {
									if (node_equals(node1,lmstentry_getNode(lmstEntry1))) {
										EBoolean lmstEntry1_isInTree = lmstentry_isIsInTree(lmstEntry1);
										if(eboolean_equals(lmstEntry1_isInTree, true)){
											void** _result = malloc(7*sizeof(void*));
											_result[0]= _this;
											_result[1]= node1;
											_result[2]= lmstEntry2;
											_result[3]= node2;
											_result[4]= lmst;
											_result[5]= lmstEntry1;
											_result[6]= shortestUnconnectedLink;
											 
											return _result;
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

void** pattern_LMSTAlgorithm_1_4_UpdateLMSTEntry_greenBB(LMSTENTRY_T* lmstEntry2, LINK_T* shortestUnconnectedLink) {
	lmstentry_setSelectedLink(lmstEntry2, shortestUnconnectedLink);
	EBoolean lmstEntry2_isInTree_prime = true;
	lmstentry_setIsInTree(lmstEntry2, lmstEntry2_isInTree_prime);
	void** _result = malloc(2*sizeof(void*));
	_result[0]= lmstEntry2;
	_result[1]= shortestUnconnectedLink;
	 
	return _result;
}

void** pattern_LMSTAlgorithm_1_5_MarkEdges_blackBFFF(LMSTALGORITHM_T* _this) {
	LMST_T* lmst = lmstalgorithm_getLmst(_this);
	if (lmst != NULL) {
		LMSTENTRY_T* entry;
		list_t list_lmst_entry = lmst_getLmstEntries(lmst);
		for (entry = list_head_pred(list_lmst_entry,lmst,&lmst_isLmstEntries); entry!=NULL; entry=list_item_next_pred(entry,lmst,&lmst_isLmstEntries)) {
			LINK_T* selected = lmstentry_getSelectedLink(entry);
			if (selected != NULL) {
				LinkState selected_marked = link_getMarked(selected);
				if (!linkstate_equals(selected_marked, ACTIVE)) {
					void** _result = malloc(4*sizeof(void*));
					_result[0]= _this;
					_result[1]= lmst;
					_result[2]= entry;
					_result[3]= selected;
					 
					return _result;
				}

			}

		}
	}

	return NULL;
}

void** pattern_LMSTAlgorithm_1_6_MarkAllLinksInTreeActive_blackB(LINK_T* selected) {
	void** _result = malloc(1*sizeof(void*));
	_result[0]= selected;
	 
	return _result;
}

void** pattern_LMSTAlgorithm_1_6_MarkAllLinksInTreeActive_greenB(LINK_T* selected) {
	LinkState selected_marked_prime = ACTIVE;
	link_setMarked(selected, selected_marked_prime);
	void** _result = malloc(1*sizeof(void*));
	_result[0]= selected;
	 
	return _result;
}

void** pattern_LMSTAlgorithm_1_7_InactivateLinks_blackBFFFF(LMSTALGORITHM_T* _this) {
	LMST_T* lmst = lmstalgorithm_getLmst(_this);
	if (lmst != NULL) {
		LMSTENTRY_T* entry;
		list_t list_lmst_entry = lmst_getLmstEntries(lmst);
		for (entry = list_head_pred(list_lmst_entry,lmst,&lmst_isLmstEntries); entry!=NULL; entry=list_item_next_pred(entry,lmst,&lmst_isLmstEntries)) {
			NODE_T* node = lmstentry_getNode(entry);
			if (node != NULL) {
				LINK_T* link;
				list_t list_link_node_neighborhood = node_getNeighborhood(node);
				for (link = list_head_pred(list_link_node_neighborhood,node,&node_isNeighborhood); link!=NULL; link=list_item_next_pred(link,node,&node_isNeighborhood)) {
					LinkState link_marked = link_getMarked(link);
					if(linkstate_equals(link_marked, UNCLASSIFIED)){
						void** _result = malloc(5*sizeof(void*));
						_result[0]= _this;
						_result[1]= lmst;
						_result[2]= entry;
						_result[3]= node;
						_result[4]= link;
						 
						return _result;
					}	

				}
			}

		}
	}

	return NULL;
}

void** pattern_LMSTAlgorithm_1_8_InactivateLinks_blackB(LINK_T* link) {
	void** _result = malloc(1*sizeof(void*));
	_result[0]= link;
	 
	return _result;
}

void** pattern_LMSTAlgorithm_1_8_InactivateLinks_greenB(LINK_T* link) {
	LinkState link_marked_prime = INACTIVE;
	link_setMarked(link, link_marked_prime);
	void** _result = malloc(1*sizeof(void*));
	_result[0]= link;
	 
	return _result;
}

void pattern_LMSTAlgorithm_1_9_Cleanup_expressionB(LMSTALGORITHM_T* _this) {
	lmstalgorithm_cleanupLMST(_this );

}

void** pattern_LMSTAlgorithm_2_1_IdentifyShortestUnconnectedLink_blackBFFFFFF(LMSTALGORITHM_T* _this) {
	LMST_T* lmst = lmstalgorithm_getLmst(_this);
	if (lmst != NULL) {
		LMSTENTRY_T* lmstEntry2;
		list_t list_lmst_lmstEntry2 = lmst_getLmstEntries(lmst);
		for (lmstEntry2 = list_head_pred(list_lmst_lmstEntry2,lmst,&lmst_isLmstEntries); lmstEntry2!=NULL; lmstEntry2=list_item_next_pred(lmstEntry2,lmst,&lmst_isLmstEntries)) {
			NODE_T* node2 = lmstentry_getNode(lmstEntry2);
			if (node2 != NULL) {
				EBoolean lmstEntry2_isInTree = lmstentry_isIsInTree(lmstEntry2);
				if(eboolean_equals(lmstEntry2_isInTree, false)){
					LMSTENTRY_T* lmstEntry1;
					list_t list_lmst_lmstEntry1 = lmst_getLmstEntries(lmst);
					for (lmstEntry1 = list_head_pred(list_lmst_lmstEntry1,lmst,&lmst_isLmstEntries); lmstEntry1!=NULL; lmstEntry1=list_item_next_pred(lmstEntry1,lmst,&lmst_isLmstEntries)) {
						if (!lmstentry_equals(lmstEntry1, lmstEntry2)) {
							NODE_T* node1 = lmstentry_getNode(lmstEntry1);
							if (node1 != NULL) {
								if (!node_equals(node1, node2)) {
									EBoolean lmstEntry1_isInTree = lmstentry_isIsInTree(lmstEntry1);
									if(eboolean_equals(lmstEntry1_isInTree, true)){
										LINK_T* link;
										list_t list_link_node2_incominglinks = node_getIncomingLinks(node2);
										for (link = list_head_pred(list_link_node2_incominglinks,node2,&node_isIncomingLinks); link!=NULL; link=list_item_next_pred(link,node2,&node_isIncomingLinks)) {
											if (node_containsOutgoingLinks(node1, link)) {
												LinkState link_marked = link_getMarked(link);
												if (!linkstate_equals(link_marked, PROCESSED)) {
													void** _result = malloc(7*sizeof(void*));
													_result[0]= _this;
													_result[1]= link;
													_result[2]= node1;
													_result[3]= node2;
													_result[4]= lmst;
													_result[5]= lmstEntry1;
													_result[6]= lmstEntry2;
													 
													return _result;
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

void** pattern_LMSTAlgorithm_2_1_IdentifyShortestUnconnectedLink_greenB(LINK_T* link) {
	LinkState link_marked_prime = PROCESSED;
	link_setMarked(link, link_marked_prime);
	void** _result = malloc(1*sizeof(void*));
	_result[0]= link;
	 
	return _result;
}

void** pattern_LMSTAlgorithm_2_2_TryToFindAShorterLink_blackBFFFBFF(LINK_T* link, LMST_T* lmst) {
	EDouble link_weight = link_getWeight(link);
	LMSTENTRY_T* lmstEntry4;
	list_t list_lmst_lmstEntry4 = lmst_getLmstEntries(lmst);
	for (lmstEntry4 = list_head_pred(list_lmst_lmstEntry4,lmst,&lmst_isLmstEntries); lmstEntry4!=NULL; lmstEntry4=list_item_next_pred(lmstEntry4,lmst,&lmst_isLmstEntries)) {
		NODE_T* node4 = lmstentry_getNode(lmstEntry4);
		if (node4 != NULL) {
			EBoolean lmstEntry4_isInTree = lmstentry_isIsInTree(lmstEntry4);
			if(eboolean_equals(lmstEntry4_isInTree, false)){
				LMSTENTRY_T* lmstEntry3;
				list_t list_lmst_lmstEntry3 = lmst_getLmstEntries(lmst);
				for (lmstEntry3 = list_head_pred(list_lmst_lmstEntry3,lmst,&lmst_isLmstEntries); lmstEntry3!=NULL; lmstEntry3=list_item_next_pred(lmstEntry3,lmst,&lmst_isLmstEntries)) {
					if (!lmstentry_equals(lmstEntry3, lmstEntry4)) {
						NODE_T* node3 = lmstentry_getNode(lmstEntry3);
						if (node3 != NULL) {
							if (!node_equals(node3, node4)) {
								EBoolean lmstEntry3_isInTree = lmstentry_isIsInTree(lmstEntry3);
								if(eboolean_equals(lmstEntry3_isInTree, true)){
									LINK_T* link2;
									list_t list_link2_node4_incominglinks = node_getIncomingLinks(node4);
									for (link2 = list_head_pred(list_link2_node4_incominglinks,node4,&node_isIncomingLinks); link2!=NULL; link2=list_item_next_pred(link2,node4,&node_isIncomingLinks)) {
										if (!link_equals(link, link2)) {
											if (node_containsOutgoingLinks(node3, link2)) {
												EDouble link2_weight = link_getWeight(link2);
												if (edouble_compare(link2_weight, link_weight) < 0) {
													void** _result = malloc(7*sizeof(void*));
													_result[0]= link;
													_result[1]= link2;
													_result[2]= node3;
													_result[3]= node4;
													_result[4]= lmst;
													_result[5]= lmstEntry3;
													_result[6]= lmstEntry4;
													 
													return _result;
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

void** pattern_LMSTAlgorithm_2_4_ResetLinks_blackFBFF(LMST_T* lmst) {
	LMSTENTRY_T* entry;
	list_t list_lmst_entry = lmst_getLmstEntries(lmst);
	for (entry = list_head_pred(list_lmst_entry,lmst,&lmst_isLmstEntries); entry!=NULL; entry=list_item_next_pred(entry,lmst,&lmst_isLmstEntries)) {
		NODE_T* node = lmstentry_getNode(entry);
		if (node != NULL) {
			LINK_T* linkDirty;
			list_t list_linkDirty_node_neighborhood = node_getNeighborhood(node);
			for (linkDirty = list_head_pred(list_linkDirty_node_neighborhood,node,&node_isNeighborhood); linkDirty!=NULL; linkDirty=list_item_next_pred(linkDirty,node,&node_isNeighborhood)) {
				LinkState linkDirty_marked = link_getMarked(linkDirty);
				if (!linkstate_equals(linkDirty_marked, UNCLASSIFIED)) {
					void** _result = malloc(4*sizeof(void*));
					_result[0]= linkDirty;
					_result[1]= lmst;
					_result[2]= entry;
					_result[3]= node;
					 
					return _result;
				}

			}
		}

	}
	return NULL;
}

void** pattern_LMSTAlgorithm_2_5_SetUnclassified_blackB(LINK_T* linkDirty) {
	void** _result = malloc(1*sizeof(void*));
	_result[0]= linkDirty;
	 
	return _result;
}

void** pattern_LMSTAlgorithm_2_5_SetUnclassified_greenB(LINK_T* linkDirty) {
	LinkState linkDirty_marked_prime = UNCLASSIFIED;
	link_setMarked(linkDirty, linkDirty_marked_prime);
	void** _result = malloc(1*sizeof(void*));
	_result[0]= linkDirty;
	 
	return _result;
}

LINK_T* pattern_LMSTAlgorithm_2_6_expressionFB(LINK_T* link) {
	LINK_T* _result = link;
	return _result;
}

LINK_T* pattern_LMSTAlgorithm_2_7_expressionF() {
	LINK_T* _result = NULL;
	return _result;
}


void lmstalgorithm_run(LMSTALGORITHM_T* this){
		// BindThis	void** result1_black = pattern_LMSTAlgorithm_1_1_BindThis_blackBF(this);	if (result1_black == NULL) {		printf("Pattern matching in node [BindThis] failed.");		printf("Variables: [this]");		exit(-1);	}	// NODE_T* self = (NODE_T*) result1_black[1];	free(result1_black);	// PrepareEntries	pattern_LMSTAlgorithm_1_2_PrepareEntries_expressionB(this);	// FindMinimalOutgoingLink	void** result3_bindingAndBlack = pattern_LMSTAlgorithm_1_3_FindMinimalOutgoingLink_bindingAndBlackFB(this);	while (result3_bindingAndBlack != NULL) {		LINK_T* shortestUnconnectedLink = (LINK_T*) result3_bindingAndBlack[0];		free(result3_bindingAndBlack);			// UpdateLMSTEntry		void** result4_black = pattern_LMSTAlgorithm_1_4_UpdateLMSTEntry_blackBFFFFFB(this, shortestUnconnectedLink);		if (result4_black == NULL) {			printf("Pattern matching in node [UpdateLMSTEntry] failed.");			printf("Variables: [this] , [shortestUnconnectedLink]");			exit(-1);		}		// NODE_T* node1 = (NODE_T*) result4_black[1];		LMSTENTRY_T* lmstEntry2 = (LMSTENTRY_T*) result4_black[2];		// NODE_T* node2 = (NODE_T*) result4_black[3];		// LMST_T* lmst = (LMST_T*) result4_black[4];		// LMSTENTRY_T* lmstEntry1 = (LMSTENTRY_T*) result4_black[5];		free(result4_black);		void** result4_green = pattern_LMSTAlgorithm_1_4_UpdateLMSTEntry_greenBB(lmstEntry2, shortestUnconnectedLink);		free(result4_green);				free(result3_bindingAndBlack);		result3_bindingAndBlack = pattern_LMSTAlgorithm_1_3_FindMinimalOutgoingLink_bindingAndBlackFB(this);	}	// MarkEdges	void** result5_black = pattern_LMSTAlgorithm_1_5_MarkEdges_blackBFFF(this);	while (result5_black != NULL) {		// LMST_T* lmst = (LMST_T*) result5_black[1];		// LMSTENTRY_T* entry = (LMSTENTRY_T*) result5_black[2];		LINK_T* selected = (LINK_T*) result5_black[3];		free(result5_black);			// MarkAllLinksInTreeActive		void** result6_black = pattern_LMSTAlgorithm_1_6_MarkAllLinksInTreeActive_blackB(selected);		if (result6_black == NULL) {			printf("Pattern matching in node [MarkAllLinksInTreeActive] failed.");			printf("Variables: [selected]");			exit(-1);		}		free(result6_black);		void** result6_green = pattern_LMSTAlgorithm_1_6_MarkAllLinksInTreeActive_greenB(selected);		free(result6_green);				free(result5_black);		result5_black = pattern_LMSTAlgorithm_1_5_MarkEdges_blackBFFF(this);	}	// InactivateLinks	void** result7_black = pattern_LMSTAlgorithm_1_7_InactivateLinks_blackBFFFF(this);	while (result7_black != NULL) {		// LMST_T* lmst = (LMST_T*) result7_black[1];		// LMSTENTRY_T* entry = (LMSTENTRY_T*) result7_black[2];		// NODE_T* node = (NODE_T*) result7_black[3];		LINK_T* link = (LINK_T*) result7_black[4];		free(result7_black);			// InactivateLinks		void** result8_black = pattern_LMSTAlgorithm_1_8_InactivateLinks_blackB(link);		if (result8_black == NULL) {			printf("Pattern matching in node [InactivateLinks] failed.");			printf("Variables: [link]");			exit(-1);		}		free(result8_black);		void** result8_green = pattern_LMSTAlgorithm_1_8_InactivateLinks_greenB(link);		free(result8_green);				free(result7_black);		result7_black = pattern_LMSTAlgorithm_1_7_InactivateLinks_blackBFFFF(this);	}	// Cleanup	pattern_LMSTAlgorithm_1_9_Cleanup_expressionB(this);	return;
}

LINK_T* lmstalgorithm_findShortestUnconnectedLink(LMSTALGORITHM_T* this){
	// IdentifyShortestUnconnectedLink	void** result1_black = pattern_LMSTAlgorithm_2_1_IdentifyShortestUnconnectedLink_blackBFFFFFF(this);	while (result1_black != NULL) {		LINK_T* link = (LINK_T*) result1_black[1];		// NODE_T* node1 = (NODE_T*) result1_black[2];		// NODE_T* node2 = (NODE_T*) result1_black[3];		LMST_T* lmst = (LMST_T*) result1_black[4];		// LMSTENTRY_T* lmstEntry1 = (LMSTENTRY_T*) result1_black[5];		// LMSTENTRY_T* lmstEntry2 = (LMSTENTRY_T*) result1_black[6];		free(result1_black);		void** result1_green = pattern_LMSTAlgorithm_2_1_IdentifyShortestUnconnectedLink_greenB(link);		free(result1_green);			// TryToFindAShorterLink		void** result2_black = pattern_LMSTAlgorithm_2_2_TryToFindAShorterLink_blackBFFFBFF(link, lmst);		if (result2_black != NULL) {			// LINK_T* link2 = (LINK_T*) result2_black[1];			// NODE_T* node3 = (NODE_T*) result2_black[2];			// NODE_T* node4 = (NODE_T*) result2_black[3];			// LMSTENTRY_T* lmstEntry3 = (LMSTENTRY_T*) result2_black[5];			// LMSTENTRY_T* lmstEntry4 = (LMSTENTRY_T*) result2_black[6];			free(result2_black);			// EmptyStoryNodeToAvoidTailControlledLoop story node is empty			} else {			// ResetLinks			void** result4_black = pattern_LMSTAlgorithm_2_4_ResetLinks_blackFBFF(lmst);			while (result4_black != NULL) {				LINK_T* linkDirty = (LINK_T*) result4_black[0];				// LMSTENTRY_T* entry = (LMSTENTRY_T*) result4_black[2];				// NODE_T* node = (NODE_T*) result4_black[3];				free(result4_black);					// SetUnclassified				void** result5_black = pattern_LMSTAlgorithm_2_5_SetUnclassified_blackB(linkDirty);				if (result5_black == NULL) {					printf("Pattern matching in node [SetUnclassified] failed.");					printf("Variables: [linkDirty]");					exit(-1);				}				free(result5_black);				void** result5_green = pattern_LMSTAlgorithm_2_5_SetUnclassified_greenB(linkDirty);				free(result5_green);						free(result4_black);				result4_black = pattern_LMSTAlgorithm_2_4_ResetLinks_blackFBFF(lmst);			}			return pattern_LMSTAlgorithm_2_6_expressionFB(link);		}			free(result1_black);		result1_black = pattern_LMSTAlgorithm_2_1_IdentifyShortestUnconnectedLink_blackBFFFFFF(this);	}	return pattern_LMSTAlgorithm_2_7_expressionF();
}
void init(){
}

#if COMPONENT_TOPOLOGYCONTROL == COMPONENT_TOPOLOGYCONTROL_CMOFLONDEMOLANGUAGE2_C
PROCESS(component_topologycontrol, "topologycontrol: cMoflonDemoLanguage2_C");
PROCESS_THREAD(component_topologycontrol, ev, data) {
	PROCESS_BEGIN();

	BOOT_COMPONENT_WAIT(component_topologycontrol);
	static struct etimer waittime;
	etimer_set(&waittime, CLOCK_SECOND * COMPONENT_TOPOLOGYCONTROL_CMOFLONDEMOLANGUAGE2_C_UPDATEINTERVAL);
	while(1) {
		PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&waittime));
				etimer_reset(&waittime);init();
		// a small delay for the base station to start broadcasting that all motes have booted and can receive this information
		watchdog_stop();

		prepareLinks();
		LMSTALGORITHM_T tc;
		tc.node =  networkaddr_node_addr();
		lmstalgorithm_run(&tc);
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
