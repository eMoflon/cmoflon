// --- Begin of kTC-specific methods
EDouble ktcalgorithm_getK(KTCALGORITHM_T* _this) {
	return _this->k;
}

NODE_T* ktcalgorithm_getNode(KTCALGORITHM_T* _this) {
	return _this->node;
}
// --- End of kTC-specific methods

// --- Begin of l*-kTC-specific methods

/**
 * Returns whether the given hop counts fulfill the l*-kTC predicate
 */
EBoolean lstarktcalgorithm_evaluateHopcountConstraint(EInt hopCount1,
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

NODE_T* lstarktcalgorithm_getNode(LSTARKTCALGORITHM_T* _this) {
	return _this->node;
}

EDouble lstarktcalgorithm_getK(LSTARKTCALGORITHM_T* _this) {
	return _this->k;
}

EDouble lstarktcalgorithm_getStretchFactor(LSTARKTCALGORITHM_T* _this) {
	return _this->stretchFactor;
}

// --- End of l*-kTC-specific methods

// --- Begin of LMST-specific methods

/**
 * Initializes the auxiliary data structures required by LMST
 */
void lmstalgorithm_init(LMSTALGORITHM_T* this) {
	TREE_T* tree = (TREE_T*) malloc(sizeof(TREE_T));
	tree->algo = this;
	MEMB(memb_entries, TREEENTRY_T, MAX_MATCH_COUNT);
	memb_init(&memb_entries);
	tree->mem = &entries;
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
				item_node->algorithm = tree;
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
				item_node->algorithm = lmst;
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
	this->lmst = tree;
}
;

/**
 * Clears the auxiliary data structures required by LMST
 */
void lmstalgorithm_cleanup(LMSTALGORITHM_T* this) {
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

NODE_T* lmstalgorithm_getNode(LMSTALGORITHM_T* _this) {
	return _this->node;
}

TREE_T* lmstalgorithm_getTree(LMSTALGORITHM_T* _this) {
	return _this->tree;
}
list_t tree_getEntries(TREE_T* _this) {
	return _this->entries;
}
bool tree_isEntries(void* candidate, void* _this) {
	return true;
}

TREE_T* treeentry_getTree(TREEENTRY_T* _this) {
	return _this->algorithm;
}

NODE_T* treeentry_getNode(TREEENTRY_T* _this) {
	return _this->node;
}
void treeentry_setNode(TREEENTRY_T* _this, NODE_T* value) {
	_this->node = value;
	return;
}

LINK_T* treeentry_getSelectedLink(TREEENTRY_T* _this) {
	return _this->parent;
}
void treeentry_setSelectedLink(TREEENTRY_T* _this, LINK_T* value) {
	_this->parent = value;
}

bool treeentry_isIsInTree(TREEENTRY_T* _this) {
	return _this->isInTree;
}
void treeentry_setIsInTree(TREEENTRY_T* _this, EBoolean value) {
	_this->isInTree = value;
}

bool treeentry_equals(TREEENTRY_T* _this, TREEENTRY_T* other) {
	bool result = true;
	result &= node_equals(_this->node, other->node);
	result &= link_equals(_this->parent, other->parent);
	return result;
}
// --- End of LMST-specific methods
