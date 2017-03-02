// Generated using cMoflon on 2017-03-61T03:03:14
#include "topologycontrol-cMoflonDemoLanguage_C-LmstAlgorithm.h"


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

// --- Begin of user-defined helpers for LmstAlgorithm (Path: 'injection/custom-helpers_LmstAlgorithm.c')
MEMB(memb_entries, TREEENTRY_T, MAX_MATCH_COUNT);
LIST(list_tree_entries);

/**
 * Initializes the auxiliary data structures required by LMST
 */
void lmstAlgorithm_init(LMSTALGORITHM_T* this) {
	TREE_T* tree = (TREE_T*) malloc(sizeof(TREE_T));
	tree->algo = this;

	memb_init(&memb_entries);
	tree->mem = &memb_entries;

	list_init(list_tree_entries);

	// add all nodes to list
	LINK_T* item_neighbor;
	for (item_neighbor = list_head(component_neighbordiscovery_neighbors());
			item_neighbor != NULL;
			item_neighbor = list_item_next(item_neighbor)) {
		TREEENTRY_T *item_node;
		bool found = false;

		// check for node1
		for (item_node = list_head(list_tree_entries); item_node != NULL;
				item_node = list_item_next(item_node)) {
			if (networkaddr_equal(item_neighbor->node1, item_node->node)) {
				found = true;
				break;
			}
		}
		if (!found) {
			item_node = memb_alloc(&memb_entries);
			if (item_node == NULL) {
				printf("ERROR[topologycontrol][LMST]: node list is full (%s:%d)\n", __FILE__, __LINE__);
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
			item_node = memb_alloc(&memb_entries);
			if (item_node == NULL) {
				printf("ERROR[topologycontrol][LMST]: node list is full (%s:%d)\n", __FILE__, __LINE__);
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

/**
 * Clears the auxiliary data structures required by LMST
 */
void lmstAlgorithm_cleanup(LMSTALGORITHM_T* this) {
	list_t entryList = this->tree->entries;
	while(list_length(entryList) > 0) {
		memb_free(this->tree->mem, list_pop(entryList));
	}
	free(this->tree);
	this->tree = NULL;
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
		void** _result = (void**) malloc(2*sizeof(void*));
		if(_result == NULL){
			printf("ERROR[topologycontrol]: could not allocate memory (%s:%d)\n", __FILE__, __LINE__);
			return NULL;
		}else{
			_result[0]= _this;
			_result[1]= self;
		 
			return _result;
		}
	}

	return NULL;
}

void pattern_LmstAlgorithm_1_2_PrepareEntries_expressionB(LMSTALGORITHM_T* _this) {
	lmstAlgorithm_init(_this );

}

void** pattern_LmstAlgorithm_1_3_BindTree_blackBF(LMSTALGORITHM_T* _this) {
	TREE_T* lmst = lmstAlgorithm_getTree(_this);
	if (lmst != NULL) {
		void** _result = (void**) malloc(2*sizeof(void*));
		if(_result == NULL){
			printf("ERROR[topologycontrol]: could not allocate memory (%s:%d)\n", __FILE__, __LINE__);
			return NULL;
		}else{
			_result[0]= _this;
			_result[1]= lmst;
		 
			return _result;
		}
	}

	return NULL;
}

void** pattern_LmstAlgorithm_1_4_FindMinimalOutgoingLink_bindingFB(LMSTALGORITHM_T* _this) {
	LINK_T* _localVariable_0 = lmstAlgorithm_findShortestUnconnectedLink(_this);
	LINK_T* shortestUnconnectedLink = _localVariable_0;
	if (shortestUnconnectedLink != NULL) {
		void** _result = (void**) malloc(2*sizeof(void*));
		if(_result == NULL){
			printf("ERROR[topologycontrol]: could not allocate memory (%s:%d)\n", __FILE__, __LINE__);
			return NULL;
		}else{
			_result[0]= shortestUnconnectedLink;
			_result[1]= _this;
		 
			return _result;
		} 
	}
	return NULL;
}

void** pattern_LmstAlgorithm_1_4_FindMinimalOutgoingLink_blackB(LINK_T* shortestUnconnectedLink) {
	void** _result = (void**) malloc(1*sizeof(void*));
	if(_result == NULL){
		printf("ERROR[topologycontrol]: could not allocate memory (%s:%d)\n", __FILE__, __LINE__);
		return NULL;
	}else{
		_result[0]= shortestUnconnectedLink;
	 
		return _result;
	}
}

void** pattern_LmstAlgorithm_1_4_FindMinimalOutgoingLink_bindingAndBlackFB(LMSTALGORITHM_T* _this) {
	void** result_pattern_LmstAlgorithm_1_4_FindMinimalOutgoingLink_binding = pattern_LmstAlgorithm_1_4_FindMinimalOutgoingLink_bindingFB(_this);
	if (result_pattern_LmstAlgorithm_1_4_FindMinimalOutgoingLink_binding != NULL) {
		LINK_T* shortestUnconnectedLink = (LINK_T*) result_pattern_LmstAlgorithm_1_4_FindMinimalOutgoingLink_binding[0];
		  
		free(result_pattern_LmstAlgorithm_1_4_FindMinimalOutgoingLink_binding);
		void** result_pattern_LmstAlgorithm_1_4_FindMinimalOutgoingLink_black = pattern_LmstAlgorithm_1_4_FindMinimalOutgoingLink_blackB(shortestUnconnectedLink);
		if (result_pattern_LmstAlgorithm_1_4_FindMinimalOutgoingLink_black != NULL) {
			 
			free(result_pattern_LmstAlgorithm_1_4_FindMinimalOutgoingLink_black);
			void** _result = (void**) malloc(2*sizeof(void*));
			if(_result == NULL){
				printf("ERROR[topologycontrol]: could not allocate memory (%s:%d)\n", __FILE__, __LINE__);
				return NULL;
			}else{
				_result[0]= shortestUnconnectedLink;
				_result[1]= _this;
			 
				return _result;
			}
		}

	}

	return NULL;
}

void** pattern_LmstAlgorithm_1_5_UpdateLMSTEntry_blackFFFBFB(TREE_T* lmst, LINK_T* shortestUnconnectedLink) {
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
									void** _result = (void**) malloc(6*sizeof(void*));
									if(_result == NULL){
										printf("ERROR[topologycontrol]: could not allocate memory (%s:%d)\n", __FILE__, __LINE__);
										return NULL;
									}else{
										_result[0]= node1;
										_result[1]= lmstEntry2;
										_result[2]= node2;
										_result[3]= lmst;
										_result[4]= lmstEntry1;
										_result[5]= shortestUnconnectedLink;
									 
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

	return NULL;
}

void** pattern_LmstAlgorithm_1_5_UpdateLMSTEntry_greenBB(TREEENTRY_T* lmstEntry2, LINK_T* shortestUnconnectedLink) {
	treeEntry_setParent(lmstEntry2, shortestUnconnectedLink);
	EBoolean lmstEntry2_isInTree_prime = true;
	treeEntry_setIsInTree(lmstEntry2, lmstEntry2_isInTree_prime);
	void** _result = (void**) malloc(2*sizeof(void*));
	if(_result == NULL){
		printf("ERROR[topologycontrol]: could not allocate memory (%s:%d)\n", __FILE__, __LINE__);
		return NULL;
	}else{
		_result[0]= lmstEntry2;
		_result[1]= shortestUnconnectedLink;
	 
		return _result;
	}
}

void** pattern_LmstAlgorithm_1_6_MarkEdges_blackBF(NODE_T* self) {
	LINK_T* link;
	list_t list_link_self_neighborhood = node_getNeighborhood(self);
	for (link = list_head_pred(list_link_self_neighborhood,self,&node_isNeighborhood); link!=NULL; link=list_item_next_pred(link,self,&node_isNeighborhood)) {
		LinkState link_marked = link_getMarked(link);
		if(linkState_equals(link_marked, UNCLASSIFIED)){
			void** _result = (void**) malloc(2*sizeof(void*));
			if(_result == NULL){
				printf("ERROR[topologycontrol]: could not allocate memory (%s:%d)\n", __FILE__, __LINE__);
				return NULL;
			}else{
				_result[0]= self;
				_result[1]= link;
			 
				return _result;
			}
		}	

	}
	return NULL;
}

void** pattern_LmstAlgorithm_1_7_MarkAllLinksInTreeActive_blackFBB(TREE_T* lmst, LINK_T* link) {
	TREEENTRY_T* entry;
	list_t list_lmst_entry = tree_getEntries(lmst);
	for (entry = list_head_pred(list_lmst_entry,lmst,&tree_isEntries); entry!=NULL; entry=list_item_next_pred(entry,lmst,&tree_isEntries)) {
		if (link_equals(link,treeEntry_getParent(entry))) {
			void** _result = (void**) malloc(3*sizeof(void*));
			if(_result == NULL){
				printf("ERROR[topologycontrol]: could not allocate memory (%s:%d)\n", __FILE__, __LINE__);
				return NULL;
			}else{
				_result[0]= entry;
				_result[1]= lmst;
				_result[2]= link;
			 
				return _result;
			}
		}
	}
	return NULL;
}

void** pattern_LmstAlgorithm_1_7_MarkAllLinksInTreeActive_greenB(LINK_T* link) {
	LinkState link_marked_prime = ACTIVE;
	link_setMarked(link, link_marked_prime);
	void** _result = (void**) malloc(1*sizeof(void*));
	if(_result == NULL){
		printf("ERROR[topologycontrol]: could not allocate memory (%s:%d)\n", __FILE__, __LINE__);
		return NULL;
	}else{
		_result[0]= link;
	 
		return _result;
	}
}

void** pattern_LmstAlgorithm_1_8_InactivateLinks_blackB(LINK_T* link) {
	void** _result = (void**) malloc(1*sizeof(void*));
	if(_result == NULL){
		printf("ERROR[topologycontrol]: could not allocate memory (%s:%d)\n", __FILE__, __LINE__);
		return NULL;
	}else{
		_result[0]= link;
	 
		return _result;
	}
}

void** pattern_LmstAlgorithm_1_8_InactivateLinks_greenB(LINK_T* link) {
	LinkState link_marked_prime = INACTIVE;
	link_setMarked(link, link_marked_prime);
	void** _result = (void**) malloc(1*sizeof(void*));
	if(_result == NULL){
		printf("ERROR[topologycontrol]: could not allocate memory (%s:%d)\n", __FILE__, __LINE__);
		return NULL;
	}else{
		_result[0]= link;
	 
		return _result;
	}
}

void pattern_LmstAlgorithm_1_9_Cleanup_expressionB(LMSTALGORITHM_T* _this) {
	lmstAlgorithm_cleanup(_this );

}

void** pattern_LmstAlgorithm_2_1_BindTree_blackBF(LMSTALGORITHM_T* _this) {
	TREE_T* lmst = lmstAlgorithm_getTree(_this);
	if (lmst != NULL) {
		void** _result = (void**) malloc(2*sizeof(void*));
		if(_result == NULL){
			printf("ERROR[topologycontrol]: could not allocate memory (%s:%d)\n", __FILE__, __LINE__);
			return NULL;
		}else{
			_result[0]= _this;
			_result[1]= lmst;
		 
			return _result;
		}
	}

	return NULL;
}

void** pattern_LmstAlgorithm_2_2_IdentifyShortestUnconnectedLink_blackFFFBFF(TREE_T* lmst) {
	TREEENTRY_T* lmstEntry2;
	list_t list_lmst_lmstEntry2 = tree_getEntries(lmst);
	for (lmstEntry2 = list_head_pred(list_lmst_lmstEntry2,lmst,&tree_isEntries); lmstEntry2!=NULL; lmstEntry2=list_item_next_pred(lmstEntry2,lmst,&tree_isEntries)) {
		NODE_T* node2 = treeEntry_getNode(lmstEntry2);
		if (node2 != NULL) {
			EBoolean lmstEntry2_isInTree = treeEntry_isIsInTree(lmstEntry2);
			if(lmstEntry2_isInTree  == false ){
			 TREEENTRY_T* lmstEntry1;
			 list_t list_lmst_lmstEntry1 = tree_getEntries(lmst);
			 for (lmstEntry1 = list_head_pred(list_lmst_lmstEntry1,lmst,&tree_isEntries); lmstEntry1!=NULL; lmstEntry1=list_item_next_pred(lmstEntry1,lmst,&tree_isEntries)) {
			 	if (!treeEntry_equals(lmstEntry1, lmstEntry2)) {
			 		NODE_T* node1 = treeEntry_getNode(lmstEntry1);
			 		if (node1 != NULL) {
			 			if (!node_equals(node1, node2)) {
			 				EBoolean lmstEntry1_isInTree = treeEntry_isIsInTree(lmstEntry1);
			 				if(lmstEntry1_isInTree  == true ){
			 				 LINK_T* link;
			 				 list_t list_link_node2_incomingLinks = node_getIncomingLinks(node2);
			 				 for (link = list_head_pred(list_link_node2_incomingLinks,node2,&node_isIncomingLinks); link!=NULL; link=list_item_next_pred(link,node2,&node_isIncomingLinks)) {
			 				 	if (node_containsOutgoingLinks(node1, link)) {
			 				 		LinkState link_marked = link_getMarked(link);
			 				 		if (!linkState_equals(link_marked, PROCESSED)) {
			 				 			EDouble link_weight = link_getWeight(link);
			 				 			if(link_isWeightDefined(link_weight )){
			 				 			 void** _result = (void**) malloc(6*sizeof(void*));
			 				 			 if(_result == NULL){
			 				 			 	printf("ERROR[topologycontrol]: could not allocate memory (%s:%d)\n", __FILE__, __LINE__);
			 				 			 	return NULL;
			 				 			 }else{
			 				 			 	_result[0]= link;
			 				 			 	_result[1]= node1;
			 				 			 	_result[2]= node2;
			 				 			 	_result[3]= lmst;
			 				 			 	_result[4]= lmstEntry1;
			 				 			 	_result[5]= lmstEntry2;
			 				 			  
			 				 			 	return _result;
			 				 			 } }

			 				 		}

			 				 	}
			 				 } }

			 			}
			 		}

			 	}
			 } }

		}

	}
	return NULL;
}

void** pattern_LmstAlgorithm_2_2_IdentifyShortestUnconnectedLink_greenB(LINK_T* link) {
	LinkState link_marked_prime = PROCESSED;
	link_setMarked(link, link_marked_prime);
	void** _result = (void**) malloc(1*sizeof(void*));
	if(_result == NULL){
		printf("ERROR[topologycontrol]: could not allocate memory (%s:%d)\n", __FILE__, __LINE__);
		return NULL;
	}else{
		_result[0]= link;
	 
		return _result;
	}
}

void** pattern_LmstAlgorithm_2_3_TryToFindAShorterLink_blackBFFFBFF(LINK_T* link, TREE_T* lmst) {
	EDouble link_weight = link_getWeight(link);
	TREEENTRY_T* lmstEntry4;
	list_t list_lmst_lmstEntry4 = tree_getEntries(lmst);
	for (lmstEntry4 = list_head_pred(list_lmst_lmstEntry4,lmst,&tree_isEntries); lmstEntry4!=NULL; lmstEntry4=list_item_next_pred(lmstEntry4,lmst,&tree_isEntries)) {
		NODE_T* node4 = treeEntry_getNode(lmstEntry4);
		if (node4 != NULL) {
			EBoolean lmstEntry4_isInTree = treeEntry_isIsInTree(lmstEntry4);
			if(lmstEntry4_isInTree  == false ){
			 TREEENTRY_T* lmstEntry3;
			 list_t list_lmst_lmstEntry3 = tree_getEntries(lmst);
			 for (lmstEntry3 = list_head_pred(list_lmst_lmstEntry3,lmst,&tree_isEntries); lmstEntry3!=NULL; lmstEntry3=list_item_next_pred(lmstEntry3,lmst,&tree_isEntries)) {
			 	if (!treeEntry_equals(lmstEntry3, lmstEntry4)) {
			 		NODE_T* node3 = treeEntry_getNode(lmstEntry3);
			 		if (node3 != NULL) {
			 			if (!node_equals(node3, node4)) {
			 				EBoolean lmstEntry3_isInTree = treeEntry_isIsInTree(lmstEntry3);
			 				if(lmstEntry3_isInTree  == true ){
			 				 LINK_T* link2;
			 				 list_t list_link2_node4_incomingLinks = node_getIncomingLinks(node4);
			 				 for (link2 = list_head_pred(list_link2_node4_incomingLinks,node4,&node_isIncomingLinks); link2!=NULL; link2=list_item_next_pred(link2,node4,&node_isIncomingLinks)) {
			 				 	if (!link_equals(link, link2)) {
			 				 		if (node_containsOutgoingLinks(node3, link2)) {
			 				 			EDouble link2_weight = link_getWeight(link2);
			 				 			if(link_isWeightDefined(link2_weight )){
			 				 			 if(link2_weight <link_weight ){
			 				 			  void** _result = (void**) malloc(7*sizeof(void*));
			 				 			  if(_result == NULL){
			 				 			  	printf("ERROR[topologycontrol]: could not allocate memory (%s:%d)\n", __FILE__, __LINE__);
			 				 			  	return NULL;
			 				 			  }else{
			 				 			  	_result[0]= link;
			 				 			  	_result[1]= link2;
			 				 			  	_result[2]= node3;
			 				 			  	_result[3]= node4;
			 				 			  	_result[4]= lmst;
			 				 			  	_result[5]= lmstEntry3;
			 				 			  	_result[6]= lmstEntry4;
			 				 			   
			 				 			  	return _result;
			 				 			  } } }

			 				 		}
			 				 	}
			 				 } }

			 			}
			 		}

			 	}
			 } }

		}

	}

	return NULL;
}

void pattern_LmstAlgorithm_2_5_UnclassifyAllDirtyLinks2_expressionBB(LMSTALGORITHM_T* _this, TREE_T* lmst) {
	lmstAlgorithm_setAllLinksToUnclassified(_this ,lmst);

}

LINK_T* pattern_LmstAlgorithm_2_6_expressionFB(LINK_T* link) {
	LINK_T* _result = link;
	return _result;
}

void pattern_LmstAlgorithm_2_7_FindDirtyLinks2_expressionBB(LMSTALGORITHM_T* _this, TREE_T* lmst) {
	lmstAlgorithm_setAllLinksToUnclassified(_this ,lmst);

}

LINK_T* pattern_LmstAlgorithm_2_8_expressionF() {
	LINK_T* _result = NULL;
	return _result;
}

void** pattern_LmstAlgorithm_4_1_EnumerateNonUnclassifiedLinks_blackBFFF(TREE_T* tree) {
	TREEENTRY_T* entry;
	list_t list_tree_entry = tree_getEntries(tree);
	for (entry = list_head_pred(list_tree_entry,tree,&tree_isEntries); entry!=NULL; entry=list_item_next_pred(entry,tree,&tree_isEntries)) {
		NODE_T* node = treeEntry_getNode(entry);
		if (node != NULL) {
			LINK_T* outgoingLink;
			list_t list_outgoingLink_node_outgoingLinks = node_getOutgoingLinks(node);
			for (outgoingLink = list_head_pred(list_outgoingLink_node_outgoingLinks,node,&node_isOutgoingLinks); outgoingLink!=NULL; outgoingLink=list_item_next_pred(outgoingLink,node,&node_isOutgoingLinks)) {
				LinkState outgoingLink_marked = link_getMarked(outgoingLink);
				if (!linkState_equals(outgoingLink_marked, UNCLASSIFIED)) {
					void** _result = (void**) malloc(4*sizeof(void*));
					if(_result == NULL){
						printf("ERROR[topologycontrol]: could not allocate memory (%s:%d)\n", __FILE__, __LINE__);
						return NULL;
					}else{
						_result[0]= tree;
						_result[1]= entry;
						_result[2]= node;
						_result[3]= outgoingLink;
					 
						return _result;
					}
				}

			}
		}

	}
	return NULL;
}

void** pattern_LmstAlgorithm_4_2_UnclassifyLink_blackB(LINK_T* outgoingLink) {
	void** _result = (void**) malloc(1*sizeof(void*));
	if(_result == NULL){
		printf("ERROR[topologycontrol]: could not allocate memory (%s:%d)\n", __FILE__, __LINE__);
		return NULL;
	}else{
		_result[0]= outgoingLink;
	 
		return _result;
	}
}

void** pattern_LmstAlgorithm_4_2_UnclassifyLink_greenB(LINK_T* outgoingLink) {
	LinkState outgoingLink_marked_prime = UNCLASSIFIED;
	link_setMarked(outgoingLink, outgoingLink_marked_prime);
	void** _result = (void**) malloc(1*sizeof(void*));
	if(_result == NULL){
		printf("ERROR[topologycontrol]: could not allocate memory (%s:%d)\n", __FILE__, __LINE__);
		return NULL;
	}else{
		_result[0]= outgoingLink;
	 
		return _result;
	}
}

void** pattern_KtcAlgorithm_0_1_BindThisNode_blackBF(KTCALGORITHM_T* _this) {
	NODE_T* this_node = ktcAlgorithm_getNode(_this);
	if (this_node != NULL) {
		void** _result = (void**) malloc(2*sizeof(void*));
		if(_result == NULL){
			printf("ERROR[topologycontrol]: could not allocate memory (%s:%d)\n", __FILE__, __LINE__);
			return NULL;
		}else{
			_result[0]= _this;
			_result[1]= this_node;
		 
			return _result;
		}
	}

	return NULL;
}

void** pattern_KtcAlgorithm_0_2_IdentifyLinksToBeInactivated_blackBBFFFFF(KTCALGORITHM_T* _this, NODE_T* this_node) {
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
					 					 				      void** _result = (void**) malloc(7*sizeof(void*));
					 					 				      if(_result == NULL){
					 					 				      	printf("ERROR[topologycontrol]: could not allocate memory (%s:%d)\n", __FILE__, __LINE__);
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

	return NULL;
}

void** pattern_KtcAlgorithm_0_3_InactivateLinks_blackB(LINK_T* e12) {
	void** _result = (void**) malloc(1*sizeof(void*));
	if(_result == NULL){
		printf("ERROR[topologycontrol]: could not allocate memory (%s:%d)\n", __FILE__, __LINE__);
		return NULL;
	}else{
		_result[0]= e12;
	 
		return _result;
	}
}

void** pattern_KtcAlgorithm_0_3_InactivateLinks_greenB(LINK_T* e12) {
	LinkState e12_marked_prime = INACTIVE;
	link_setMarked(e12, e12_marked_prime);
	void** _result = (void**) malloc(1*sizeof(void*));
	if(_result == NULL){
		printf("ERROR[topologycontrol]: could not allocate memory (%s:%d)\n", __FILE__, __LINE__);
		return NULL;
	}else{
		_result[0]= e12;
	 
		return _result;
	}
}

void** pattern_KtcAlgorithm_0_4_IdentifyRemainingUnclassifiedEdges_blackBF(NODE_T* this_node) {
	LINK_T* e12;
	list_t list_e12_this_node_outgoingLinks = node_getOutgoingLinks(this_node);
	for (e12 = list_head_pred(list_e12_this_node_outgoingLinks,this_node,&node_isOutgoingLinks); e12!=NULL; e12=list_item_next_pred(e12,this_node,&node_isOutgoingLinks)) {
		LinkState e12_marked = link_getMarked(e12);
		if(linkState_equals(e12_marked, UNCLASSIFIED)){
			void** _result = (void**) malloc(2*sizeof(void*));
			if(_result == NULL){
				printf("ERROR[topologycontrol]: could not allocate memory (%s:%d)\n", __FILE__, __LINE__);
				return NULL;
			}else{
				_result[0]= this_node;
				_result[1]= e12;
			 
				return _result;
			}
		}	

	}
	return NULL;
}

void** pattern_KtcAlgorithm_0_5_ActivateEdge_blackB(LINK_T* e12) {
	void** _result = (void**) malloc(1*sizeof(void*));
	if(_result == NULL){
		printf("ERROR[topologycontrol]: could not allocate memory (%s:%d)\n", __FILE__, __LINE__);
		return NULL;
	}else{
		_result[0]= e12;
	 
		return _result;
	}
}

void** pattern_KtcAlgorithm_0_5_ActivateEdge_greenB(LINK_T* e12) {
	LinkState e12_marked_prime = ACTIVE;
	link_setMarked(e12, e12_marked_prime);
	void** _result = (void**) malloc(1*sizeof(void*));
	if(_result == NULL){
		printf("ERROR[topologycontrol]: could not allocate memory (%s:%d)\n", __FILE__, __LINE__);
		return NULL;
	}else{
		_result[0]= e12;
	 
		return _result;
	}
}

void** pattern_LStarKtcAlgorithm_0_1_BindThisNode_blackBF(LSTARKTCALGORITHM_T* _this) {
	NODE_T* this_node = lStarKtcAlgorithm_getNode(_this);
	if (this_node != NULL) {
		void** _result = (void**) malloc(2*sizeof(void*));
		if(_result == NULL){
			printf("ERROR[topologycontrol]: could not allocate memory (%s:%d)\n", __FILE__, __LINE__);
			return NULL;
		}else{
			_result[0]= _this;
			_result[1]= this_node;
		 
			return _result;
		}
	}

	return NULL;
}

void** pattern_LStarKtcAlgorithm_0_2_IdentifyLinksToBeInactivated_blackBBFFFFF(LSTARKTCALGORITHM_T* _this, NODE_T* this_node) {
	EDouble this_k = lStarKtcAlgorithm_getK(_this);
	EInt this_node_hopcount = node_getHopcount(this_node);
	EDouble this_stretchFactor = lStarKtcAlgorithm_getStretchFactor(_this);
	LINK_T* e13;
	list_t list_e13_this_node_outgoingLinks = node_getOutgoingLinks(this_node);
	for (e13 = list_head_pred(list_e13_this_node_outgoingLinks,this_node,&node_isOutgoingLinks); e13!=NULL; e13=list_item_next_pred(e13,this_node,&node_isOutgoingLinks)) {
		NODE_T* n3 = link_getTarget(e13);
		if (n3 != NULL) {
			if (!node_equals(n3, this_node)) {
				EDouble e13_weight = link_getWeight(e13);
				if(link_isWeightDefined(e13_weight )){
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
				 						if(link_isWeightDefined(e12_weight )){
				 						 EInt n2_hopcount = node_getHopcount(n2);
				 						 if(lStarKtcAlgorithm_evaluateHopcountConstraint(this_node_hopcount , n2_hopcount , n3_hopcount , this_stretchFactor )){
				 						  LINK_T* e32;
				 						  list_t list_e32_n3_outgoingLinks = node_getOutgoingLinks(n3);
				 						  for (e32 = list_head_pred(list_e32_n3_outgoingLinks,n3,&node_isOutgoingLinks); e32!=NULL; e32=list_item_next_pred(e32,n3,&node_isOutgoingLinks)) {
				 						  	if (!link_equals(e13, e32)) {
				 						  		if (!link_equals(e12, e32)) {
				 						  			if (node_containsIncomingLinks(n2, e32)) {
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
				 						  				      void** _result = (void**) malloc(7*sizeof(void*));
				 						  				      if(_result == NULL){
				 						  				      	printf("ERROR[topologycontrol]: could not allocate memory (%s:%d)\n", __FILE__, __LINE__);
				 						  				      	return NULL;
				 						  				      }else{
				 						  				      	_result[0]= _this;
				 						  				      	_result[1]= this_node;
				 						  				      	_result[2]= n2;
				 						  				      	_result[3]= n3;
				 						  				      	_result[4]= e13;
				 						  				      	_result[5]= e12;
				 						  				      	_result[6]= e32;
				 						  				       
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
				 		}

				 	}
				 }
				 }

			}
		}

	}



	return NULL;
}

void** pattern_LStarKtcAlgorithm_0_3_InactivateLinks_blackB(LINK_T* e12) {
	void** _result = (void**) malloc(1*sizeof(void*));
	if(_result == NULL){
		printf("ERROR[topologycontrol]: could not allocate memory (%s:%d)\n", __FILE__, __LINE__);
		return NULL;
	}else{
		_result[0]= e12;
	 
		return _result;
	}
}

void** pattern_LStarKtcAlgorithm_0_3_InactivateLinks_greenB(LINK_T* e12) {
	LinkState e12_marked_prime = INACTIVE;
	link_setMarked(e12, e12_marked_prime);
	void** _result = (void**) malloc(1*sizeof(void*));
	if(_result == NULL){
		printf("ERROR[topologycontrol]: could not allocate memory (%s:%d)\n", __FILE__, __LINE__);
		return NULL;
	}else{
		_result[0]= e12;
	 
		return _result;
	}
}

void** pattern_LStarKtcAlgorithm_0_4_IdentifyRemainingUnclassifiedEdges_blackBF(NODE_T* this_node) {
	LINK_T* e12;
	list_t list_e12_this_node_outgoingLinks = node_getOutgoingLinks(this_node);
	for (e12 = list_head_pred(list_e12_this_node_outgoingLinks,this_node,&node_isOutgoingLinks); e12!=NULL; e12=list_item_next_pred(e12,this_node,&node_isOutgoingLinks)) {
		LinkState e12_marked = link_getMarked(e12);
		if(linkState_equals(e12_marked, UNCLASSIFIED)){
			void** _result = (void**) malloc(2*sizeof(void*));
			if(_result == NULL){
				printf("ERROR[topologycontrol]: could not allocate memory (%s:%d)\n", __FILE__, __LINE__);
				return NULL;
			}else{
				_result[0]= this_node;
				_result[1]= e12;
			 
				return _result;
			}
		}	

	}
	return NULL;
}

void** pattern_LStarKtcAlgorithm_0_5_ActivateEdge_blackB(LINK_T* e12) {
	void** _result = (void**) malloc(1*sizeof(void*));
	if(_result == NULL){
		printf("ERROR[topologycontrol]: could not allocate memory (%s:%d)\n", __FILE__, __LINE__);
		return NULL;
	}else{
		_result[0]= e12;
	 
		return _result;
	}
}

void** pattern_LStarKtcAlgorithm_0_5_ActivateEdge_greenB(LINK_T* e12) {
	LinkState e12_marked_prime = ACTIVE;
	link_setMarked(e12, e12_marked_prime);
	void** _result = (void**) malloc(1*sizeof(void*));
	if(_result == NULL){
		printf("ERROR[topologycontrol]: could not allocate memory (%s:%d)\n", __FILE__, __LINE__);
		return NULL;
	}else{
		_result[0]= e12;
	 
		return _result;
	}
}


void lmstAlgorithm_run(LMSTALGORITHM_T* this){
	// BindThis
	void** result1_black = pattern_LmstAlgorithm_1_1_BindThis_blackBF(this);
	if (result1_black == NULL) {
		printf("ERROR[topologycontrol]: Pattern matching in node [BindThis] failed (%s:%d)\n", __FILE__, __LINE__);
		printf("Variables: [this]\n");
		exit(-1);
	}
	NODE_T* self = (NODE_T*) result1_black[1];
	free(result1_black);
	// PrepareEntries
	pattern_LmstAlgorithm_1_2_PrepareEntries_expressionB(this);
	
	// BindTree
	void** result3_black = pattern_LmstAlgorithm_1_3_BindTree_blackBF(this);
	if (result3_black == NULL) {
		printf("ERROR[topologycontrol]: Pattern matching in node [BindTree] failed (%s:%d)\n", __FILE__, __LINE__);
		printf("Variables: [this]\n");
		exit(-1);
	}
	TREE_T* lmst = (TREE_T*) result3_black[1];
	free(result3_black);
	// FindMinimalOutgoingLink
	void** result4_bindingAndBlack = pattern_LmstAlgorithm_1_4_FindMinimalOutgoingLink_bindingAndBlackFB(this);
	while (result4_bindingAndBlack != NULL) {
		LINK_T* shortestUnconnectedLink = (LINK_T*) result4_bindingAndBlack[0];
		free(result4_bindingAndBlack);
		// UpdateLMSTEntry
		void** result5_black = pattern_LmstAlgorithm_1_5_UpdateLMSTEntry_blackFFFBFB(lmst, shortestUnconnectedLink);
		if (result5_black != NULL) {
			// NODE_T* node1 = (NODE_T*) result5_black[0];
			TREEENTRY_T* lmstEntry2 = (TREEENTRY_T*) result5_black[1];
			// NODE_T* node2 = (NODE_T*) result5_black[2];
			// TREEENTRY_T* lmstEntry1 = (TREEENTRY_T*) result5_black[4];
			free(result5_black);
			void** result5_green = pattern_LmstAlgorithm_1_5_UpdateLMSTEntry_greenBB(lmstEntry2, shortestUnconnectedLink);
			free(result5_green);
	
		} else {
		}
	
		result4_bindingAndBlack = pattern_LmstAlgorithm_1_4_FindMinimalOutgoingLink_bindingAndBlackFB(this);
	}
	// MarkEdges
	void** result6_black = pattern_LmstAlgorithm_1_6_MarkEdges_blackBF(self);
	while (result6_black != NULL) {
		LINK_T* link = (LINK_T*) result6_black[1];
		free(result6_black);
		// MarkAllLinksInTreeActive
		void** result7_black = pattern_LmstAlgorithm_1_7_MarkAllLinksInTreeActive_blackFBB(lmst, link);
		if (result7_black != NULL) {
			// TREEENTRY_T* entry = (TREEENTRY_T*) result7_black[0];
			free(result7_black);
			void** result7_green = pattern_LmstAlgorithm_1_7_MarkAllLinksInTreeActive_greenB(link);
			free(result7_green);
	
		} else {
			// InactivateLinks
			void** result8_black = pattern_LmstAlgorithm_1_8_InactivateLinks_blackB(link);
			if (result8_black != NULL) {
				free(result8_black);
				void** result8_green = pattern_LmstAlgorithm_1_8_InactivateLinks_greenB(link);
				free(result8_green);
	
			} else {
			}
	
		}
	
		result6_black = pattern_LmstAlgorithm_1_6_MarkEdges_blackBF(self);
	}
	// Cleanup
	pattern_LmstAlgorithm_1_9_Cleanup_expressionB(this);
	return;

}

LINK_T* lmstAlgorithm_findShortestUnconnectedLink(LMSTALGORITHM_T* this){
	// BindTree
	void** result1_black = pattern_LmstAlgorithm_2_1_BindTree_blackBF(this);
	if (result1_black == NULL) {
		printf("ERROR[topologycontrol]: Pattern matching in node [BindTree] failed (%s:%d)\n", __FILE__, __LINE__);
		printf("Variables: [this]\n");
		exit(-1);
	}
	TREE_T* lmst = (TREE_T*) result1_black[1];
	free(result1_black);
	// IdentifyShortestUnconnectedLink
	void** result2_black = pattern_LmstAlgorithm_2_2_IdentifyShortestUnconnectedLink_blackFFFBFF(lmst);
	while (result2_black != NULL) {
		LINK_T* link = (LINK_T*) result2_black[0];
		// NODE_T* node1 = (NODE_T*) result2_black[1];
		// NODE_T* node2 = (NODE_T*) result2_black[2];
		// TREEENTRY_T* lmstEntry1 = (TREEENTRY_T*) result2_black[4];
		// TREEENTRY_T* lmstEntry2 = (TREEENTRY_T*) result2_black[5];
		free(result2_black);
		void** result2_green = pattern_LmstAlgorithm_2_2_IdentifyShortestUnconnectedLink_greenB(link);
		free(result2_green);
	
		// TryToFindAShorterLink
		void** result3_black = pattern_LmstAlgorithm_2_3_TryToFindAShorterLink_blackBFFFBFF(link, lmst);
		if (result3_black != NULL) {
			// LINK_T* link2 = (LINK_T*) result3_black[1];
			// NODE_T* node3 = (NODE_T*) result3_black[2];
			// NODE_T* node4 = (NODE_T*) result3_black[3];
			// TREEENTRY_T* lmstEntry3 = (TREEENTRY_T*) result3_black[5];
			// TREEENTRY_T* lmstEntry4 = (TREEENTRY_T*) result3_black[6];
			free(result3_black);
			// EmptyStoryNodeToAvoidTailControlledLoop story node is empty (else branch was ignored)
	
		} else {
			// UnclassifyAllDirtyLinks2
			pattern_LmstAlgorithm_2_5_UnclassifyAllDirtyLinks2_expressionBB(this, lmst);
			return pattern_LmstAlgorithm_2_6_expressionFB(link);
		}
	
		result2_black = pattern_LmstAlgorithm_2_2_IdentifyShortestUnconnectedLink_blackFFFBFF(lmst);
	}
	// FindDirtyLinks2
	pattern_LmstAlgorithm_2_7_FindDirtyLinks2_expressionBB(this, lmst);
	return pattern_LmstAlgorithm_2_8_expressionF();

}

void lmstAlgorithm_setAllLinksToUnclassified(LMSTALGORITHM_T* this, TREE_T* tree){
	// EnumerateNonUnclassifiedLinks
	void** result1_black = pattern_LmstAlgorithm_4_1_EnumerateNonUnclassifiedLinks_blackBFFF(tree);
	while (result1_black != NULL) {
		// TREEENTRY_T* entry = (TREEENTRY_T*) result1_black[1];
		// NODE_T* node = (NODE_T*) result1_black[2];
		LINK_T* outgoingLink = (LINK_T*) result1_black[3];
		free(result1_black);
		// UnclassifyLink
		void** result2_black = pattern_LmstAlgorithm_4_2_UnclassifyLink_blackB(outgoingLink);
		if (result2_black != NULL) {
			free(result2_black);
			void** result2_green = pattern_LmstAlgorithm_4_2_UnclassifyLink_greenB(outgoingLink);
			free(result2_green);
	
		} else {
		}
	
		result1_black = pattern_LmstAlgorithm_4_1_EnumerateNonUnclassifiedLinks_blackBFFF(tree);
	}
	return;

}

void ktcAlgorithm_run(KTCALGORITHM_T* this){
	// BindThisNode
	void** result1_black = pattern_KtcAlgorithm_0_1_BindThisNode_blackBF(this);
	if (result1_black == NULL) {
		printf("ERROR[topologycontrol]: Pattern matching in node [BindThisNode] failed (%s:%d)\n", __FILE__, __LINE__);
		printf("Variables: [this]\n");
		exit(-1);
	}
	NODE_T* this_node = (NODE_T*) result1_black[1];
	free(result1_black);
	// IdentifyLinksToBeInactivated
	void** result2_black = pattern_KtcAlgorithm_0_2_IdentifyLinksToBeInactivated_blackBBFFFFF(this, this_node);
	while (result2_black != NULL) {
		LINK_T* e12 = (LINK_T*) result2_black[2];
		// NODE_T* n2 = (NODE_T*) result2_black[3];
		// NODE_T* n3 = (NODE_T*) result2_black[4];
		// LINK_T* e32 = (LINK_T*) result2_black[5];
		// LINK_T* e13 = (LINK_T*) result2_black[6];
		free(result2_black);
		// InactivateLinks
		void** result3_black = pattern_KtcAlgorithm_0_3_InactivateLinks_blackB(e12);
		if (result3_black != NULL) {
			free(result3_black);
			void** result3_green = pattern_KtcAlgorithm_0_3_InactivateLinks_greenB(e12);
			free(result3_green);
	
		} else {
		}
	
		result2_black = pattern_KtcAlgorithm_0_2_IdentifyLinksToBeInactivated_blackBBFFFFF(this, this_node);
	}
	// IdentifyRemainingUnclassifiedEdges
	void** result4_black = pattern_KtcAlgorithm_0_4_IdentifyRemainingUnclassifiedEdges_blackBF(this_node);
	while (result4_black != NULL) {
		LINK_T* e12 = (LINK_T*) result4_black[1];
		free(result4_black);
		// ActivateEdge
		void** result5_black = pattern_KtcAlgorithm_0_5_ActivateEdge_blackB(e12);
		if (result5_black != NULL) {
			free(result5_black);
			void** result5_green = pattern_KtcAlgorithm_0_5_ActivateEdge_greenB(e12);
			free(result5_green);
	
		} else {
		}
	
		result4_black = pattern_KtcAlgorithm_0_4_IdentifyRemainingUnclassifiedEdges_blackBF(this_node);
	}
	return;

}

void lStarKtcAlgorithm_run(LSTARKTCALGORITHM_T* this){
	// BindThisNode
	void** result1_black = pattern_LStarKtcAlgorithm_0_1_BindThisNode_blackBF(this);
	if (result1_black == NULL) {
		printf("ERROR[topologycontrol]: Pattern matching in node [BindThisNode] failed (%s:%d)\n", __FILE__, __LINE__);
		printf("Variables: [this]\n");
		exit(-1);
	}
	NODE_T* this_node = (NODE_T*) result1_black[1];
	free(result1_black);
	// IdentifyLinksToBeInactivated
	void** result2_black = pattern_LStarKtcAlgorithm_0_2_IdentifyLinksToBeInactivated_blackBBFFFFF(this, this_node);
	while (result2_black != NULL) {
		// NODE_T* n2 = (NODE_T*) result2_black[2];
		// NODE_T* n3 = (NODE_T*) result2_black[3];
		// LINK_T* e13 = (LINK_T*) result2_black[4];
		LINK_T* e12 = (LINK_T*) result2_black[5];
		// LINK_T* e32 = (LINK_T*) result2_black[6];
		free(result2_black);
		// InactivateLinks
		void** result3_black = pattern_LStarKtcAlgorithm_0_3_InactivateLinks_blackB(e12);
		if (result3_black != NULL) {
			free(result3_black);
			void** result3_green = pattern_LStarKtcAlgorithm_0_3_InactivateLinks_greenB(e12);
			free(result3_green);
	
		} else {
		}
	
		result2_black = pattern_LStarKtcAlgorithm_0_2_IdentifyLinksToBeInactivated_blackBBFFFFF(this, this_node);
	}
	// IdentifyRemainingUnclassifiedEdges
	void** result4_black = pattern_LStarKtcAlgorithm_0_4_IdentifyRemainingUnclassifiedEdges_blackBF(this_node);
	while (result4_black != NULL) {
		LINK_T* e12 = (LINK_T*) result4_black[1];
		free(result4_black);
		// ActivateEdge
		void** result5_black = pattern_LStarKtcAlgorithm_0_5_ActivateEdge_blackB(e12);
		if (result5_black != NULL) {
			free(result5_black);
			void** result5_green = pattern_LStarKtcAlgorithm_0_5_ActivateEdge_greenB(e12);
			free(result5_green);
	
		} else {
		}
	
		result4_black = pattern_LStarKtcAlgorithm_0_4_IdentifyRemainingUnclassifiedEdges_blackBF(this_node);
	}
	return;

}
void init(){
}

#if COMPONENT_TOPOLOGYCONTROL == COMPONENT_TOPOLOGYCONTROL_CMOFLONDEMOLANGUAGE_C_LMSTALGORITHM
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
		lmstAlgorithm_run(&tc);
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
