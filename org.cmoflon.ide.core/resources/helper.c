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

void prepareLinks() {
	list_t neighbors = component_neighbordiscovery_neighbors();
	list_init(local_links);
	LINK_T* link;
	for (link = list_head(neighbors); link != NULL; link = list_item_next(link)) {
		LINK_T* temp;
		if ((temp = memb_alloc(&memb_local_links)) == NULL)
			printf("[topologycontrol]:Status Error: memb_local is full\n");
		temp->node1 = link->node1;
		temp->node2 = link->node2;
		temp->weight_node1_to_node2 = link->weight_node1_to_node2;
		temp->weight_node2_to_node1 = link->weight_node2_to_node1;
		temp->ttl_node1_to_node2 = link->ttl_node1_to_node2;
		temp->ttl_node2_to_node1 = link->ttl_node2_to_node1;
		temp->state = UNCLASSIFIED;
		list_add(local_links, temp);
	}
}

void freeLinks(){
	LINK_T* link;
	for (link = list_head(local_links); link != NULL; link = list_item_next(link)) {
		memb_free(&memb_local_links, list_pop(local_links));
	}
}

//Begin of non SDM implemented methods
EDouble link_getScaledWeight(LINK_T* _this, EDouble k) {
	double result = (link_getWeight(_this) == COMPONENT_NEIGHBORDISCOVERY_WEIGHTUNKNOWN) ? COMPONENT_NEIGHBORDISCOVERY_WEIGHTUNKNOWN : link_getWeight(_this)*k;
	return result;
}
//End of non SDM implemented methods

//Begin of declarations for incomingLinks
list_t node_getIncomingLinks(NODE_T* _this) {
	return local_links;
}

bool node_containsIncomingLinks(NODE_T* _this, LINK_T* value) {
	LINK_T* link;
	for (link = list_head_pred(local_links,_this,&node_isIncomingLinks); link != NULL; link = list_item_next_pred(link,_this,&node_isIncomingLinks)) {
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
	return local_links;
}

bool node_containsOutgoingLinks(NODE_T* _this, LINK_T* value) {
	LINK_T* link;
	for (link = list_head_pred(local_links,_this,&node_isOutgoingLinks); link != NULL; link = list_item_next_pred(link, _this, &node_isOutgoingLinks)) {
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

//Begin of declarations for marked
LinkState link_getMarked(LINK_T* _this) {
	return _this->state;
}
void link_setMarked(LINK_T* _this, LinkState value) {
	printf("MARKING\n"); \
	_this->state = value;
	if (value==INACTIVE) {
		if (node_equals(networkaddr_node_addr(), _this->node1)) {
			component_network_ignoredlinks_add(_this->node2);
		}
		else
			if (node_equals(networkaddr_node_addr(), _this->node2))
				component_network_ignoredlinks_add(_this->node1);
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
//End of equals declarations