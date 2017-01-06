//Begin of non SDM implemented methods
EDouble link_getScaledWeight(LINK_T* _this, EDouble k) {
	double result = (link_getWeight(_this) == COMPONENT_NEIGHBORDISCOVERY_WEIGHTUNKNOWN) ? COMPONENT_NEIGHBORDISCOVERY_WEIGHTUNKNOWN : link_getWeight(_this)*k;
	return result;
}
//End of non SDM implemented methods

//Begin of declarations for incomingLinks
list_t node_getIncomingLinks(NODE_T* _this) {
	list_t neighbors = component_neighbordiscovery_neighbors();
	LIST(incoming_links);
	list_init(incoming_links);
	neighbor_t* link;
	for (link = list_head(neighbors); link != NULL; link = list_item_next(link)) {
		if ((node_equals(link->link->node2, _this) || node_equals(link->link->node1, _this)) && (link->link->weight_node1_to_node2 != COMPONENT_NEIGHBORDISCOVERY_WEIGHTUNKNOWN) && (link->link->weight_node2_to_node1 != COMPONENT_NEIGHBORDISCOVERY_WEIGHTUNKNOWN)) {
			LINK_T* temp;
			temp->state = UNCLASSIFIED;
			if ((temp = memb_alloc(&memb_local)) == NULL)
				printf("[topologycontrol]:Status ERROR: memb_local is full\n");
			else {
				if (node_equals(link->node2, _this)) {
					temp->link->next = NULL;
					temp->link->node1 = link->node1;
					temp->link->node2 = link->node2;
					temp->link->weight_node1_to_node2 = link->weight_node1_to_node2;
					temp->link->weight_node2_to_node1 = link->weight_node2_to_node1;
					temp->link->ttl_node1_to_node2 = link->ttl_node1_to_node2;
					temp->link->ttl_node2_to_node1 = link->ttl_node2_to_node1;
					list_add(incoming_links, temp);
				}
				else {
					temp->link->->next = NULL;
					temp->link->->node1 = link->node2;
					temp->link->->node2 = link->node1;
					temp->link->->weight_node1_to_node2 = link->weight_node2_to_node1;
					temp->link->->weight_node2_to_node1 = link->weight_node1_to_node2;
					temp->link->->ttl_node1_to_node2 = link->ttl_node2_to_node1;
					temp->link->->ttl_node2_to_node1 = link->ttl_node1_to_node2;
					list_add(incoming_links, temp);
				}
			}
		}
	}
	return incoming_links;
}

bool node_containsIncomingLinks(NODE_T* _this, LINK_T* value) {
	list_t incoming_links = node_getIncomingLinks(_this);
	LINK_T* link;
	bool result = false;
	for (link = list_head(incoming_links); link != NULL;) {
		if (link_equals(value, link))
			result = true;
		link = list_item_next(link);
		memb_free(&memb_local, list_pop(incoming_links));
	}
	return result;
}
//End of declarations for incomingLinks

//Begin of declarations for outgoingLinks
list_t node_getOutgoingLinks(NODE_T* _this) {
	list_t neighbors = component_neighbordiscovery_neighbors();
	LIST(outgoing_links);
	list_init(outgoing_links);
	LINK_T* link;
	for (link = list_head(neighbors); link != NULL; link = list_item_next(link)) {
		if ((node_equals(link->link->node1, _this) || node_equals(link->link->node2, _this)) && (link->link->weight_node1_to_node2 != COMPONENT_NEIGHBORDISCOVERY_WEIGHTUNKNOWN) && (link->link->weight_node2_to_node1 != COMPONENT_NEIGHBORDISCOVERY_WEIGHTUNKNOWN)) {
			LINK_T* temp;
			temp->state = UNCLASSIFIED;
			if ((temp = memb_alloc(&memb_local)) == NULL)
				printf("[topologycontrol]:Status Error: memb_local is full\n");
			else {
				if (node_equals(link->node1, _this)) {
					temp->link->next = NULL;
					temp->link->node1 = link->node1;
					temp->link->node2 = link->node2;
					temp->link->weight_node1_to_node2 = link->weight_node1_to_node2;
					temp->link->weight_node2_to_node1 = link->weight_node2_to_node1;
					temp->link->ttl_node1_to_node2 = link->ttl_node1_to_node2;
					temp->link->ttl_node2_to_node1 = link->ttl_node2_to_node1;
					list_add(outgoing_links, temp);
				}
				else {
					temp->link->next = NULL;
					temp->link->node1 = link->node2;
					temp->link->node2 = link->node1;
					temp->link->weight_node1_to_node2 = link->weight_node2_to_node1;
					temp->link->weight_node2_to_node1 = link->weight_node1_to_node2;
					temp->link->ttl_node1_to_node2 = link->ttl_node2_to_node1;
					temp->link->ttl_node2_to_node1 = link->ttl_node1_to_node2;
					list_add(outgoing_links, temp);
				}
			}
		}
	}
	return outgoing_links;
}

bool node_containsOutgoingLinks(NODE_T* _this, LINK_T* value) {
	list_t outgoing_links = node_getOutgoingLinks(_this);
	LINK_T* link;
	bool result = false;
	for (link = list_head(outgoing_links); link != NULL;) {
		if (link_equals(value, link))
			result = true;
		link = list_item_next(link);
		memb_free(&memb_local, list_pop(outgoing_links));
	}
	return result;
}
//End of declarations for outgoingLinks

//Begin of declarations for marked
void link_setMarked(LINK_T* _this, EBoolean value) {
	printf("MARKING\n");
	if (value) {
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
//Begin of declarations for marked
LinkState link_getMarked(LINK_T* _this) {
	return this->state;
}
void link_setMarked(LINK_T* _this, LinkState value) {
	printf("MARKING\n");
	if (value==INACTIVE) {
		if (node_equals(networkaddr_node_addr(), _this->link->node1)) {
			component_network_ignoredlinks_add(_this->link->node2);
		}
		else
			if (node_equals(networkaddr_node_addr(), _this->link->node2))
				component_network_ignoredlinks_add(_this->link->node1);
	}
	//IF this node is not part of the edge don't ignore any of the nodes
}
//End of declarations for marked

//Begin of declarations for weight
EDouble link_getWeight(LINK_T* _this) {
	if (_this->link->weight_node2_to_node1 == COMPONENT_NEIGHBORDISCOVERY_WEIGHTUNKNOWN || _this->link->weight_node1_to_node2 == COMPONENT_NEIGHBORDISCOVERY_WEIGHTUNKNOWN) {
		return COMPONENT_NEIGHBORDISCOVERY_WEIGHTUNKNOWN;
	}
	else {
		return _this->link->weight_node1_to_node2;
	}
}
//End of declarations for weight

//Begin of declarations for target
NODE_T* link_getTarget(LINK_T* _this) {
	return _this->link->node2;
}
//End of declarations for target

//Begin of declarations for source
NODE_T* link_getSource(LINK_T* _this) {
	return _this->link->node1;
}
//End of declarations for source

//Begin of declarations for node
NODE_T* maxpoweralgorithm_getNode(MAXPOWERALGORITHM_T* _this) {
	return _this->node;
};
//End of declarations for node

//Begin of declarations for k
EDOUBLE_T* ktcalgorithm_getK(KTCALGORITHM_T* _this) {
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
	return((node_equals(_this->link->node1, other->link->node1) && node_equals(_this->link->node2, other->link->node2)) || (node_equals(_this->link->node1, other->link->node2) && node_equals(_this->link->node2, other->link->node1)));

}
//End of equals declarations