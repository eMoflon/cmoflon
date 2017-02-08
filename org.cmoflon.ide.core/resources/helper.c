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