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
