#include "topologycontrol-cMoflonDemoLanguage3_C-LmstAlgorithm.h"


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
// Algorithm-independent helper definitions.
// --- End of user-defined algorithm-independent helpers

// --- Begin of user-defined helpers for LmstAlgorithm (Path: 'injection/custom-helpers_LmstAlgorithm.c')
/**
 * Initializes the auxiliary data structures required by LMST
 */
void lmstAlgorithm_init(LMSTALGORITHM_T* this) {
	TREE_T* tree = (TREE_T*) malloc(sizeof(TREE_T));
	tree->algo = this;
	MEMB(memb_entries, TREEENTRY_T, MAX_MATCH_COUNT);
	memb_init(&memb_entries);
	tree->mem = &memb_entries;
	LIST(list_tree_entries);
	list_init(list_tree_entries);

	// add all nodes to list
	LINK_T* item_neighbor;
	for (item_neighbor = list_head(component_neighbordiscovery_neighbors());
			item_neighbor != NULL;
			item_neighbor = list_item_next(item_neighbor)) {
		TREEENTRY_T *item_node = (TREEENTRY_T*) malloc(sizeof(TREEENTRY_T));
		bool found;

		// check for node1
		found = false;
		for (item_node = list_head(list_tree_entries); item_node != NULL;
				item_node = list_item_next(item_node)) {
			if (networkaddr_equal(item_neighbor->node1, item_node->node)) {
				found = true;
				break;
			}
		}
		if (!found) {
			if ((item_node = memb_alloc(&memb_entries)) == NULL) {
				printf("ERROR[topologycontrol-lmst]: nodelist is full\n");
			} else {
				item_node->node = item_neighbor->node1;
				item_node->parent = NULL;
				item_node->tree = tree;
				if (networkaddr_equal(networkaddr_node_addr(),
						item_neighbor->node1))
					item_node->isInTree = true;
				else
					item_node->isInTree = false;
				list_add(list_tree_entries, item_node);
			}
		}

		// check for node2
		found = false;
		for (item_node = list_head(list_tree_entries); item_node != NULL;
				item_node = list_item_next(item_node)) {
			if (networkaddr_equal(item_neighbor->node2, item_node->node)) {
				found = true;
				break;
			}
		}
		if (!found) {
			if ((item_node = memb_alloc(&memb_entries)) == NULL) {
				printf("ERROR[topologycontrol-lmst]: nodelist is full\n");
			} else {
				item_node->node = item_neighbor->node2;
				item_node->parent = NULL;
				item_node->tree = tree;
				if (networkaddr_equal(networkaddr_node_addr(),
						item_neighbor->node2))
					item_node->isInTree = true;
				else
					item_node->isInTree = false;
				list_add(list_tree_entries, item_node);
			}
		}
	}
	tree->entries = list_tree_entries;
	this->tree = tree;
}
;

/**
 * Clears the auxiliary data structures required by LMST
 */
void lmstAlgorithm_cleanup(LMSTALGORITHM_T* this) {
	list_t entryList = this->tree->entries;
	// add all nodes to list
	TREEENTRY_T* item_neighbor;
	for (item_neighbor = list_head(entryList); item_neighbor != NULL; item_neighbor =
			list_item_next(item_neighbor)) {
		free(item_neighbor);
		memb_free(this->tree->mem, list_pop(entryList));
	}
	free(entryList);
}

NODE_T* lmstAlgorithm_getNode(LMSTALGORITHM_T* _this) {
	return _this->node;
}

TREE_T* lmstAlgorithm_getTree(LMSTALGORITHM_T* _this) {
	return _this->tree;
}
list_t tree_getEntries(TREE_T* _this) {
	return _this->entries;
}
bool tree_isEntries(void* candidate, void* _this) {
	return true;
}

TREE_T* treeEntry_getTree(TREEENTRY_T* _this) {
	return _this->tree;
}

NODE_T* treeEntry_getNode(TREEENTRY_T* _this) {
	return _this->node;
}
void treeEntry_setNode(TREEENTRY_T* _this, NODE_T* value) {
	_this->node = value;
}

LINK_T* treeEntry_getParent(TREEENTRY_T* _this) {
	return _this->parent;
}
void treeEntry_setParent(TREEENTRY_T* _this, LINK_T* value) {
	_this->parent = value;
}

bool treeEntry_isIsInTree(TREEENTRY_T* _this) {
	return _this->isInTree;
}
void treeEntry_setIsInTree(TREEENTRY_T* _this, EBoolean value) {
	_this->isInTree = value;
}

bool treeEntry_equals(TREEENTRY_T* _this, TREEENTRY_T* other) {
	bool result = true;
	result &= node_equals(_this->node, other->node);
	result &= link_equals(_this->parent, other->parent);
	return result;
}
// --- End of user-defined helpers for LmstAlgorithm

void** pattern_LmstAlgorithm_1_1_BindThis_blackBF(LMSTALGORITHM_T* _this) {
	NODE_T* self = lmstAlgorithm_getNode(_this);
	if (self != NULL) {
		void** _result = malloc(2*sizeof(void*));
		_result[0]= _this;
		_result[1]= self;
		 
		return _result;
	}

	return NULL;
}

void pattern_LmstAlgorithm_1_2_PrepareEntries_expressionB(LMSTALGORITHM_T* _this) {
	lmstAlgorithm_init(_this );

}

void** pattern_LmstAlgorithm_1_3_FindMinimalOutgoingLink_bindingFB(LMSTALGORITHM_T* _this) {
	LINK_T* _localVariable_0 = lmstAlgorithm_findShortestUnconnectedLink(_this);
	LINK_T* shortestUnconnectedLink = _localVariable_0;
	if (shortestUnconnectedLink != NULL) {
		void** _result = malloc(2*sizeof(void*));
		_result[0]= shortestUnconnectedLink;
		_result[1]= _this;
		 
		return _result; 
	}
	return NULL;
}

void** pattern_LmstAlgorithm_1_3_FindMinimalOutgoingLink_blackB(LINK_T* shortestUnconnectedLink) {
	void** _result = malloc(1*sizeof(void*));
	_result[0]= shortestUnconnectedLink;
	 
	return _result;
}

void** pattern_LmstAlgorithm_1_3_FindMinimalOutgoingLink_bindingAndBlackFB(LMSTALGORITHM_T* _this) {
	void** result_pattern_LmstAlgorithm_1_3_FindMinimalOutgoingLink_binding = pattern_LmstAlgorithm_1_3_FindMinimalOutgoingLink_bindingFB(_this);
	if (result_pattern_LmstAlgorithm_1_3_FindMinimalOutgoingLink_binding != NULL) {
		LINK_T* shortestUnconnectedLink = (LINK_T*) result_pattern_LmstAlgorithm_1_3_FindMinimalOutgoingLink_binding[0];
		  
		void** result_pattern_LmstAlgorithm_1_3_FindMinimalOutgoingLink_black = pattern_LmstAlgorithm_1_3_FindMinimalOutgoingLink_blackB(shortestUnconnectedLink);
		if (result_pattern_LmstAlgorithm_1_3_FindMinimalOutgoingLink_black != NULL) {
			 
			void** _result = malloc(2*sizeof(void*));
			_result[0]= shortestUnconnectedLink;
			_result[1]= _this;
			 
			return _result;
		}
		free(result_pattern_LmstAlgorithm_1_3_FindMinimalOutgoingLink_black);

	}
	free(result_pattern_LmstAlgorithm_1_3_FindMinimalOutgoingLink_binding);

	return NULL;
}

void** pattern_LmstAlgorithm_1_4_UpdateLMSTEntry_blackBFFFFFB(LMSTALGORITHM_T* _this, LINK_T* shortestUnconnectedLink) {
	TREE_T* lmst = lmstAlgorithm_getTree(_this);
	if (lmst != NULL) {
		NODE_T* node1 = link_getSource(shortestUnconnectedLink);
		if (node1 != NULL) {
			NODE_T* node2 = link_getTarget(shortestUnconnectedLink);
			if (node2 != NULL) {
				if (!node_equals(node1, node2)) {
					TREEENTRY_T* lmstEntry2;
					list_t list_lmst_lmstEntry2 = tree_getEntries(lmst);
					for (lmstEntry2 = list_head_pred(list_lmst_lmstEntry2,lmst,&tree_isEntries); lmstEntry2!=NULL; lmstEntry2=list_item_next_pred(lmstEntry2,lmst,&tree_isEntries)) {
						if (node_equals(node2,treeEntry_getNode(lmstEntry2))) {
							TREEENTRY_T* lmstEntry1;
							list_t list_lmst_lmstEntry1 = tree_getEntries(lmst);
							for (lmstEntry1 = list_head_pred(list_lmst_lmstEntry1,lmst,&tree_isEntries); lmstEntry1!=NULL; lmstEntry1=list_item_next_pred(lmstEntry1,lmst,&tree_isEntries)) {
								if (!treeEntry_equals(lmstEntry1, lmstEntry2)) {
									if (node_equals(node1,treeEntry_getNode(lmstEntry1))) {
										EBoolean lmstEntry1_isInTree = treeEntry_isIsInTree(lmstEntry1);
										if(eBoolean_equals(lmstEntry1_isInTree, true)){
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

void** pattern_LmstAlgorithm_1_4_UpdateLMSTEntry_greenBB(TREEENTRY_T* lmstEntry2, LINK_T* shortestUnconnectedLink) {
	treeEntry_setParent(lmstEntry2, shortestUnconnectedLink);
	EBoolean lmstEntry2_isInTree_prime = true;
	treeEntry_setIsInTree(lmstEntry2, lmstEntry2_isInTree_prime);
	void** _result = malloc(2*sizeof(void*));
	_result[0]= lmstEntry2;
	_result[1]= shortestUnconnectedLink;
	 
	return _result;
}

void** pattern_LmstAlgorithm_1_5_MarkEdges_blackBFFF(LMSTALGORITHM_T* _this) {
	TREE_T* lmst = lmstAlgorithm_getTree(_this);
	if (lmst != NULL) {
		TREEENTRY_T* entry;
		list_t list_lmst_entry = tree_getEntries(lmst);
		for (entry = list_head_pred(list_lmst_entry,lmst,&tree_isEntries); entry!=NULL; entry=list_item_next_pred(entry,lmst,&tree_isEntries)) {
			LINK_T* selected = treeEntry_getParent(entry);
			if (selected != NULL) {
				LinkState selected_marked = link_getMarked(selected);
				if (!linkState_equals(selected_marked, ACTIVE)) {
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

void** pattern_LmstAlgorithm_1_6_MarkAllLinksInTreeActive_blackB(LINK_T* selected) {
	void** _result = malloc(1*sizeof(void*));
	_result[0]= selected;
	 
	return _result;
}

void** pattern_LmstAlgorithm_1_6_MarkAllLinksInTreeActive_greenB(LINK_T* selected) {
	LinkState selected_marked_prime = ACTIVE;
	link_setMarked(selected, selected_marked_prime);
	void** _result = malloc(1*sizeof(void*));
	_result[0]= selected;
	 
	return _result;
}

void** pattern_LmstAlgorithm_1_7_InactivateLinks_blackBFFFF(LMSTALGORITHM_T* _this) {
	TREE_T* lmst = lmstAlgorithm_getTree(_this);
	if (lmst != NULL) {
		TREEENTRY_T* entry;
		list_t list_lmst_entry = tree_getEntries(lmst);
		for (entry = list_head_pred(list_lmst_entry,lmst,&tree_isEntries); entry!=NULL; entry=list_item_next_pred(entry,lmst,&tree_isEntries)) {
			NODE_T* node = treeEntry_getNode(entry);
			if (node != NULL) {
				LINK_T* link;
				list_t list_link_node_neighborhood = node_getNeighborhood(node);
				for (link = list_head_pred(list_link_node_neighborhood,node,&node_isNeighborhood); link!=NULL; link=list_item_next_pred(link,node,&node_isNeighborhood)) {
					LinkState link_marked = link_getMarked(link);
					if(linkState_equals(link_marked, UNCLASSIFIED)){
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

void** pattern_LmstAlgorithm_1_8_InactivateLinks_blackB(LINK_T* link) {
	void** _result = malloc(1*sizeof(void*));
	_result[0]= link;
	 
	return _result;
}

void** pattern_LmstAlgorithm_1_8_InactivateLinks_greenB(LINK_T* link) {
	LinkState link_marked_prime = INACTIVE;
	link_setMarked(link, link_marked_prime);
	void** _result = malloc(1*sizeof(void*));
	_result[0]= link;
	 
	return _result;
}

void pattern_LmstAlgorithm_1_9_Cleanup_expressionB(LMSTALGORITHM_T* _this) {
	lmstAlgorithm_cleanup(_this );

}

void** pattern_LmstAlgorithm_2_1_IdentifyShortestUnconnectedLink_blackBFFFFFF(LMSTALGORITHM_T* _this) {
	TREE_T* lmst = lmstAlgorithm_getTree(_this);
	if (lmst != NULL) {
		TREEENTRY_T* lmstEntry2;
		list_t list_lmst_lmstEntry2 = tree_getEntries(lmst);
		for (lmstEntry2 = list_head_pred(list_lmst_lmstEntry2,lmst,&tree_isEntries); lmstEntry2!=NULL; lmstEntry2=list_item_next_pred(lmstEntry2,lmst,&tree_isEntries)) {
			NODE_T* node2 = treeEntry_getNode(lmstEntry2);
			if (node2 != NULL) {
				EBoolean lmstEntry2_isInTree = treeEntry_isIsInTree(lmstEntry2);
				if(eBoolean_equals(lmstEntry2_isInTree, false)){
					TREEENTRY_T* lmstEntry1;
					list_t list_lmst_lmstEntry1 = tree_getEntries(lmst);
					for (lmstEntry1 = list_head_pred(list_lmst_lmstEntry1,lmst,&tree_isEntries); lmstEntry1!=NULL; lmstEntry1=list_item_next_pred(lmstEntry1,lmst,&tree_isEntries)) {
						if (!treeEntry_equals(lmstEntry1, lmstEntry2)) {
							NODE_T* node1 = treeEntry_getNode(lmstEntry1);
							if (node1 != NULL) {
								if (!node_equals(node1, node2)) {
									EBoolean lmstEntry1_isInTree = treeEntry_isIsInTree(lmstEntry1);
									if(eBoolean_equals(lmstEntry1_isInTree, true)){
										LINK_T* link;
										list_t list_link_node2_incomingLinks = node_getIncomingLinks(node2);
										for (link = list_head_pred(list_link_node2_incomingLinks,node2,&node_isIncomingLinks); link!=NULL; link=list_item_next_pred(link,node2,&node_isIncomingLinks)) {
											if (node_containsOutgoingLinks(node1, link)) {
												LinkState link_marked = link_getMarked(link);
												if (!linkState_equals(link_marked, PROCESSED)) {
													EDouble link_weight = link_getWeight(link);
													if(link_isWeightDefined(link_weight )){
													 void** _result = malloc(7*sizeof(void*));
													 _result[0]= _this;
													 _result[1]= link;
													 _result[2]= node1;
													 _result[3]= node2;
													 _result[4]= lmst;
													 _result[5]= lmstEntry1;
													 _result[6]= lmstEntry2;
													  
													 return _result; }

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

void** pattern_LmstAlgorithm_2_1_IdentifyShortestUnconnectedLink_greenB(LINK_T* link) {
	LinkState link_marked_prime = PROCESSED;
	link_setMarked(link, link_marked_prime);
	void** _result = malloc(1*sizeof(void*));
	_result[0]= link;
	 
	return _result;
}

void** pattern_LmstAlgorithm_2_2_TryToFindAShorterLink_blackBFFFBFF(LINK_T* link, TREE_T* lmst) {
	EDouble link_weight = link_getWeight(link);
	TREEENTRY_T* lmstEntry4;
	list_t list_lmst_lmstEntry4 = tree_getEntries(lmst);
	for (lmstEntry4 = list_head_pred(list_lmst_lmstEntry4,lmst,&tree_isEntries); lmstEntry4!=NULL; lmstEntry4=list_item_next_pred(lmstEntry4,lmst,&tree_isEntries)) {
		NODE_T* node4 = treeEntry_getNode(lmstEntry4);
		if (node4 != NULL) {
			EBoolean lmstEntry4_isInTree = treeEntry_isIsInTree(lmstEntry4);
			if(eBoolean_equals(lmstEntry4_isInTree, false)){
				TREEENTRY_T* lmstEntry3;
				list_t list_lmst_lmstEntry3 = tree_getEntries(lmst);
				for (lmstEntry3 = list_head_pred(list_lmst_lmstEntry3,lmst,&tree_isEntries); lmstEntry3!=NULL; lmstEntry3=list_item_next_pred(lmstEntry3,lmst,&tree_isEntries)) {
					if (!treeEntry_equals(lmstEntry3, lmstEntry4)) {
						NODE_T* node3 = treeEntry_getNode(lmstEntry3);
						if (node3 != NULL) {
							if (!node_equals(node3, node4)) {
								EBoolean lmstEntry3_isInTree = treeEntry_isIsInTree(lmstEntry3);
								if(eBoolean_equals(lmstEntry3_isInTree, true)){
									LINK_T* link2;
									list_t list_link2_node4_incomingLinks = node_getIncomingLinks(node4);
									for (link2 = list_head_pred(list_link2_node4_incomingLinks,node4,&node_isIncomingLinks); link2!=NULL; link2=list_item_next_pred(link2,node4,&node_isIncomingLinks)) {
										if (!link_equals(link, link2)) {
											if (node_containsOutgoingLinks(node3, link2)) {
												EDouble link2_weight = link_getWeight(link2);
												if(link_isWeightDefined(link2_weight )){
												 if(link2_weight <link_weight ){
												  void** _result = malloc(7*sizeof(void*));
												  _result[0]= link;
												  _result[1]= link2;
												  _result[2]= node3;
												  _result[3]= node4;
												  _result[4]= lmst;
												  _result[5]= lmstEntry3;
												  _result[6]= lmstEntry4;
												   
												  return _result; } }

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

void** pattern_LmstAlgorithm_2_4_FindDirtyLink_blackFBFF(TREE_T* lmst) {
	TREEENTRY_T* entry;
	list_t list_lmst_entry = tree_getEntries(lmst);
	for (entry = list_head_pred(list_lmst_entry,lmst,&tree_isEntries); entry!=NULL; entry=list_item_next_pred(entry,lmst,&tree_isEntries)) {
		NODE_T* node = treeEntry_getNode(entry);
		if (node != NULL) {
			LINK_T* linkDirty;
			list_t list_linkDirty_node_neighborhood = node_getNeighborhood(node);
			for (linkDirty = list_head_pred(list_linkDirty_node_neighborhood,node,&node_isNeighborhood); linkDirty!=NULL; linkDirty=list_item_next_pred(linkDirty,node,&node_isNeighborhood)) {
				LinkState linkDirty_marked = link_getMarked(linkDirty);
				if (!linkState_equals(linkDirty_marked, UNCLASSIFIED)) {
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

void** pattern_LmstAlgorithm_2_5_SetUnclassified_blackB(LINK_T* linkDirty) {
	void** _result = malloc(1*sizeof(void*));
	_result[0]= linkDirty;
	 
	return _result;
}

void** pattern_LmstAlgorithm_2_5_SetUnclassified_greenB(LINK_T* linkDirty) {
	LinkState linkDirty_marked_prime = UNCLASSIFIED;
	link_setMarked(linkDirty, linkDirty_marked_prime);
	void** _result = malloc(1*sizeof(void*));
	_result[0]= linkDirty;
	 
	return _result;
}

LINK_T* pattern_LmstAlgorithm_2_6_expressionFB(LINK_T* link) {
	LINK_T* _result = link;
	return _result;
}

void** pattern_LmstAlgorithm_2_7_FindDirtyLinks2_blackFBFFF(LMSTALGORITHM_T* _this) {
	TREE_T* lmst = lmstAlgorithm_getTree(_this);
	if (lmst != NULL) {
		TREEENTRY_T* entry;
		list_t list_lmst_entry = tree_getEntries(lmst);
		for (entry = list_head_pred(list_lmst_entry,lmst,&tree_isEntries); entry!=NULL; entry=list_item_next_pred(entry,lmst,&tree_isEntries)) {
			NODE_T* node = treeEntry_getNode(entry);
			if (node != NULL) {
				LINK_T* linkDirty;
				list_t list_linkDirty_node_neighborhood = node_getNeighborhood(node);
				for (linkDirty = list_head_pred(list_linkDirty_node_neighborhood,node,&node_isNeighborhood); linkDirty!=NULL; linkDirty=list_item_next_pred(linkDirty,node,&node_isNeighborhood)) {
					LinkState linkDirty_marked = link_getMarked(linkDirty);
					if (!linkState_equals(linkDirty_marked, UNCLASSIFIED)) {
						void** _result = malloc(5*sizeof(void*));
						_result[0]= lmst;
						_result[1]= _this;
						_result[2]= linkDirty;
						_result[3]= entry;
						_result[4]= node;
						 
						return _result;
					}

				}
			}

		}
	}

	return NULL;
}

void** pattern_LmstAlgorithm_2_8_SetUnclassified2_blackB(LINK_T* linkDirty) {
	void** _result = malloc(1*sizeof(void*));
	_result[0]= linkDirty;
	 
	return _result;
}

void** pattern_LmstAlgorithm_2_8_SetUnclassified2_greenB(LINK_T* linkDirty) {
	LinkState linkDirty_marked_prime = UNCLASSIFIED;
	link_setMarked(linkDirty, linkDirty_marked_prime);
	void** _result = malloc(1*sizeof(void*));
	_result[0]= linkDirty;
	 
	return _result;
}

LINK_T* pattern_LmstAlgorithm_2_9_expressionF() {
	LINK_T* _result = NULL;
	return _result;
}


void lmstAlgorithm_run(LMSTALGORITHM_T* this){
	// BindThis
	void** result1_black = pattern_LmstAlgorithm_1_1_BindThis_blackBF(this);
	if (result1_black == NULL) {
		printf("Pattern matching in node [BindThis] failed.");
		printf("Variables: [this]");
		exit(-1);
	}
	// NODE_T* self = (NODE_T*) result1_black[1];
	free(result1_black);
	// PrepareEntries
	pattern_LmstAlgorithm_1_2_PrepareEntries_expressionB(this);
	// FindMinimalOutgoingLink
	void** result3_bindingAndBlack = pattern_LmstAlgorithm_1_3_FindMinimalOutgoingLink_bindingAndBlackFB(this);
	while (result3_bindingAndBlack != NULL) {
		LINK_T* shortestUnconnectedLink = (LINK_T*) result3_bindingAndBlack[0];
		free(result3_bindingAndBlack);
	
		// UpdateLMSTEntry
		void** result4_black = pattern_LmstAlgorithm_1_4_UpdateLMSTEntry_blackBFFFFFB(this, shortestUnconnectedLink);
		if (result4_black == NULL) {
			printf("Pattern matching in node [UpdateLMSTEntry] failed.");
			printf("Variables: [this] , [shortestUnconnectedLink]");
			exit(-1);
		}
		// NODE_T* node1 = (NODE_T*) result4_black[1];
		TREEENTRY_T* lmstEntry2 = (TREEENTRY_T*) result4_black[2];
		// NODE_T* node2 = (NODE_T*) result4_black[3];
		// TREE_T* lmst = (TREE_T*) result4_black[4];
		// TREEENTRY_T* lmstEntry1 = (TREEENTRY_T*) result4_black[5];
		free(result4_black);
		void** result4_green = pattern_LmstAlgorithm_1_4_UpdateLMSTEntry_greenBB(lmstEntry2, shortestUnconnectedLink);
		free(result4_green);
	
	
		free(result3_bindingAndBlack);
		result3_bindingAndBlack = pattern_LmstAlgorithm_1_3_FindMinimalOutgoingLink_bindingAndBlackFB(this);
	}
	// MarkEdges
	void** result5_black = pattern_LmstAlgorithm_1_5_MarkEdges_blackBFFF(this);
	while (result5_black != NULL) {
		// TREE_T* lmst = (TREE_T*) result5_black[1];
		// TREEENTRY_T* entry = (TREEENTRY_T*) result5_black[2];
		LINK_T* selected = (LINK_T*) result5_black[3];
		free(result5_black);
	
		// MarkAllLinksInTreeActive
		void** result6_black = pattern_LmstAlgorithm_1_6_MarkAllLinksInTreeActive_blackB(selected);
		if (result6_black == NULL) {
			printf("Pattern matching in node [MarkAllLinksInTreeActive] failed.");
			printf("Variables: [selected]");
			exit(-1);
		}
		free(result6_black);
		void** result6_green = pattern_LmstAlgorithm_1_6_MarkAllLinksInTreeActive_greenB(selected);
		free(result6_green);
	
	
		free(result5_black);
		result5_black = pattern_LmstAlgorithm_1_5_MarkEdges_blackBFFF(this);
	}
	// InactivateLinks
	void** result7_black = pattern_LmstAlgorithm_1_7_InactivateLinks_blackBFFFF(this);
	while (result7_black != NULL) {
		// TREE_T* lmst = (TREE_T*) result7_black[1];
		// TREEENTRY_T* entry = (TREEENTRY_T*) result7_black[2];
		// NODE_T* node = (NODE_T*) result7_black[3];
		LINK_T* link = (LINK_T*) result7_black[4];
		free(result7_black);
	
		// InactivateLinks
		void** result8_black = pattern_LmstAlgorithm_1_8_InactivateLinks_blackB(link);
		if (result8_black == NULL) {
			printf("Pattern matching in node [InactivateLinks] failed.");
			printf("Variables: [link]");
			exit(-1);
		}
		free(result8_black);
		void** result8_green = pattern_LmstAlgorithm_1_8_InactivateLinks_greenB(link);
		free(result8_green);
	
	
		free(result7_black);
		result7_black = pattern_LmstAlgorithm_1_7_InactivateLinks_blackBFFFF(this);
	}
	// Cleanup
	pattern_LmstAlgorithm_1_9_Cleanup_expressionB(this);
	return;

}

LINK_T* lmstAlgorithm_findShortestUnconnectedLink(LMSTALGORITHM_T* this){
	// IdentifyShortestUnconnectedLink
	void** result1_black = pattern_LmstAlgorithm_2_1_IdentifyShortestUnconnectedLink_blackBFFFFFF(this);
	while (result1_black != NULL) {
		LINK_T* link = (LINK_T*) result1_black[1];
		// NODE_T* node1 = (NODE_T*) result1_black[2];
		// NODE_T* node2 = (NODE_T*) result1_black[3];
		TREE_T* lmst = (TREE_T*) result1_black[4];
		// TREEENTRY_T* lmstEntry1 = (TREEENTRY_T*) result1_black[5];
		// TREEENTRY_T* lmstEntry2 = (TREEENTRY_T*) result1_black[6];
		free(result1_black);
		void** result1_green = pattern_LmstAlgorithm_2_1_IdentifyShortestUnconnectedLink_greenB(link);
		free(result1_green);
	
		// TryToFindAShorterLink
		void** result2_black = pattern_LmstAlgorithm_2_2_TryToFindAShorterLink_blackBFFFBFF(link, lmst);
		if (result2_black != NULL) {
			// LINK_T* link2 = (LINK_T*) result2_black[1];
			// NODE_T* node3 = (NODE_T*) result2_black[2];
			// NODE_T* node4 = (NODE_T*) result2_black[3];
			// TREEENTRY_T* lmstEntry3 = (TREEENTRY_T*) result2_black[5];
			// TREEENTRY_T* lmstEntry4 = (TREEENTRY_T*) result2_black[6];
			free(result2_black);
			// EmptyStoryNodeToAvoidTailControlledLoop story node is empty
	
		} else {
			// FindDirtyLink
			void** result4_black = pattern_LmstAlgorithm_2_4_FindDirtyLink_blackFBFF(lmst);
			while (result4_black != NULL) {
				LINK_T* linkDirty = (LINK_T*) result4_black[0];
				// TREEENTRY_T* entry = (TREEENTRY_T*) result4_black[2];
				// NODE_T* node = (NODE_T*) result4_black[3];
				free(result4_black);
	
				// SetUnclassified
				void** result5_black = pattern_LmstAlgorithm_2_5_SetUnclassified_blackB(linkDirty);
				if (result5_black == NULL) {
					printf("Pattern matching in node [SetUnclassified] failed.");
					printf("Variables: [linkDirty]");
					exit(-1);
				}
				free(result5_black);
				void** result5_green = pattern_LmstAlgorithm_2_5_SetUnclassified_greenB(linkDirty);
				free(result5_green);
	
	
				free(result4_black);
				result4_black = pattern_LmstAlgorithm_2_4_FindDirtyLink_blackFBFF(lmst);
			}
			return pattern_LmstAlgorithm_2_6_expressionFB(link);
		}
	
		free(result1_black);
		result1_black = pattern_LmstAlgorithm_2_1_IdentifyShortestUnconnectedLink_blackBFFFFFF(this);
	}
	// FindDirtyLinks2
	void** result7_black = pattern_LmstAlgorithm_2_7_FindDirtyLinks2_blackFBFFF(this);
	while (result7_black != NULL) {
		// TREE_T* lmst = (TREE_T*) result7_black[0];
		LINK_T* linkDirty = (LINK_T*) result7_black[2];
		// TREEENTRY_T* entry = (TREEENTRY_T*) result7_black[3];
		// NODE_T* node = (NODE_T*) result7_black[4];
		free(result7_black);
	
		// SetUnclassified2
		void** result8_black = pattern_LmstAlgorithm_2_8_SetUnclassified2_blackB(linkDirty);
		if (result8_black == NULL) {
			printf("Pattern matching in node [SetUnclassified2] failed.");
			printf("Variables: [linkDirty]");
			exit(-1);
		}
		free(result8_black);
		void** result8_green = pattern_LmstAlgorithm_2_8_SetUnclassified2_greenB(linkDirty);
		free(result8_green);
	
	
		free(result7_black);
		result7_black = pattern_LmstAlgorithm_2_7_FindDirtyLinks2_blackFBFFF(this);
	}
	return pattern_LmstAlgorithm_2_9_expressionF();

}
void init(){
}

#if COMPONENT_TOPOLOGYCONTROL == COMPONENT_TOPOLOGYCONTROL_CMOFLONDEMOLANGUAGE3_C_LMSTALGORITHM
PROCESS(component_topologycontrol, "topologycontrol: LmstAlgorithm");
PROCESS_THREAD(component_topologycontrol, ev, data) {
	PROCESS_BEGIN();

	BOOT_COMPONENT_WAIT(component_topologycontrol);
	static struct etimer waittime;
	etimer_set(&waittime, CLOCK_SECOND * COMPONENT_TOPOLOGYCONTROL_LMSTALGORITHM_UPDATEINTERVAL);
	while(1) {
		PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&waittime));
				etimer_reset(&waittime);init();
		// a small delay for the base station to start broadcasting that all motes have booted and can receive this information
		watchdog_stop();

		prepareLinks();
		LMSTALGORITHM_T tc;
		tc.node =  networkaddr_node_addr();
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
		lmstAlgorithm_run(&tc);
		printf("[topologycontrol]: TIME: %lu\n",RTIMER_NOW()-start);
		watchdog_start();
	}
	PROCESS_END();
}

#endif
