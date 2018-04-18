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
// TODO: this should be included or activated via flags only
void init(){
	list_t neighbors=component_neighbordiscovery_neighbors();
	neighbor_t* onehop;
	int count=0;
	int realcount=0;
	for(onehop = list_head(neighbors); onehop != NULL; onehop = list_item_next(onehop)) {
		networkaddr_t* self=networkaddr_node_addr();
		if(networkaddr_equal(onehop->node1, self)==0){
			if(onehop->weight_node1_to_node2 != COMPONENT_NEIGHBORDISCOVERY_WEIGHTUNKNOWN && onehop->weight_node2_to_node1 != COMPONENT_NEIGHBORDISCOVERY_WEIGHTUNKNOWN){
				neighbor_t* sender= (neighbor_t*)malloc(sizeof(neighbor_t));
				count++;
				if(sender  == NULL) {
					printf("ERROR[TC-LMST]: Could not allocate memory\n");
				} else {
					realcount++;
					sender->node1 = networkaddr_reference_alloc(onehop->node2);
					sender->node2 = networkaddr_reference_alloc(onehop->node1);
					sender->weight_node1_to_node2 = onehop->weight_node2_to_node1;
					sender->weight_node2_to_node1 = onehop->weight_node1_to_node2;
					sender->ttl_node1_to_node2 = onehop->ttl_node2_to_node1;
					sender->ttl_node2_to_node1 = onehop->ttl_node1_to_node2;
					#ifdef TOPOLOGYCONTROL_LINKS_HAVE_STATES
					sender->state=UNCLASSIFIED;
					#endif
					list_add(list_duplicates,sender);
				}
			}
		}
	}
	for(onehop=list_head(list_duplicates);onehop!=NULL;onehop=list_item_next(onehop)){
		list_add(neighbors, onehop);
	}
	printf("Duplicated %d out of %d links.\n",realcount,count);
}

void cleanup(){
	list_t neighbors=component_neighbordiscovery_neighbors();
	neighbor_t* list_item;
	for(list_item=list_head(list_duplicates);list_item!=NULL;list_item=list_item_next(list_item)){
		list_remove(neighbors,list_item);
	}
	while(list_length(list_duplicates) > 0) {
		free(list_pop(list_duplicates));
	}
}
// --- End of default cMoflon code
