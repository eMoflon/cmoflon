// --- Begin of default cMoflon code
#define min(a,b) (((a)<(b))?(a):(b))
#define max(a,b) (((a)>(b))?(a):(b))

/**
 * This function returns the first element ('item') in the given 'list' for which pred(item, _this) returns true
 */
static void* list_head_pred(list_t list, void* _this, bool (*pred)(void*, void*)) {
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
static void* list_item_next_pred(void* item, void* _this, bool (*pred)(void*, void*)) {
	for (item = list_item_next(item); item != NULL;
			item = list_item_next(item)) {
		if (pred(item, _this)) {
			return item;
		}
	}
	return NULL;
}

static list_t node_getIncomingLinks(NODE_T* _this) {
	return component_neighbordiscovery_neighbors();
}

static bool node_containsIncomingLinks(NODE_T* _this, LINK_T* value) {
	LINK_T* link;
	for (link = list_head_pred(component_neighbordiscovery_neighbors(), _this,
			&node_isIncomingLinks); link != NULL;
			link = list_item_next_pred(link, _this, &node_isIncomingLinks)) {
		if (link_equals(value, link))
			return true;
	}
	return false;
}

static bool node_isIncomingLinks(void* candidate, void* _this) {
	if (node_equals((NODE_T*) _this, ((LINK_T*) candidate)->node2)
			&& ((LINK_T*) candidate)->weight_node2_to_node1
					!= COMPONENT_NEIGHBORDISCOVERY_WEIGHTUNKNOWN) {
		return true;
	} else
		return false;
}

static list_t node_getOutgoingLinks(NODE_T* _this) {
	return component_neighbordiscovery_neighbors();
}

static bool node_containsOutgoingLinks(NODE_T* _this, LINK_T* value) {
	LINK_T* link;
	for (link = list_head_pred(component_neighbordiscovery_neighbors(), _this,
			&node_isOutgoingLinks); link != NULL;
			link = list_item_next_pred(link, _this, &node_isOutgoingLinks)) {
		if (link_equals(value, link))
			return true;
	}
	return false;
}

static bool node_isOutgoingLinks(void* candidate, void* _this) {
	if (node_equals((NODE_T*) _this, ((LINK_T*) candidate)->node1)
			&& ((LINK_T*) candidate)->weight_node1_to_node2
					!= COMPONENT_NEIGHBORDISCOVERY_WEIGHTUNKNOWN) {
		return true;
	} else
		return false;
}

static list_t node_getNeighborhood(NODE_T* _this) {
	return component_neighbordiscovery_neighbors();
}
static bool node_isNeighborhood(void* candidate, void* _this) {
	return true;
}

static bool link_isWeightDefined(EDouble weight)
{
	return weight != COMPONENT_NEIGHBORDISCOVERY_WEIGHTUNKNOWN;
}

static EDouble link_getWeight(LINK_T* _this) {
		return _this->weight_node1_to_node2;
}

static NODE_T* link_getTarget(LINK_T* _this) {
	return _this->node2;
}

static NODE_T* link_getSource(LINK_T* _this) {
	return _this->node1;
}

static LinkState link_getMarked(LINK_T* _this) {
	return _this->state;
}
static void link_setMarked(LINK_T* _this, LinkState value) {
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

/*
 * Returns 0 if both values are equal.
 * Returns a value greater than 0 if _this is true and other is false
 * Returns a value smaller than 0 if _this is false and other is true
 */
static int eBoolean_compare(EBoolean _this, EBoolean other) {
  return _this == other ? 0 : (_this ? 1 : -1);
}

static int eDouble_compare(EDouble _this, EDouble other) {
  return _this == other ? 0 : (_this > other ? 1 : -1);
}

static int eFloat_compare(EFloat _this, EFloat other) {
  return _this == other ? 0 : (_this > other ? 1 : -1);
}

static int eInt_compare(EInt _this, EInt other) {
  return _this == other ? 0 : (_this > other ? 1 : -1);
}

static int eLong_compare(ELong _this, ELong other) {
  return _this == other ? 0 : (_this > other ? 1 : -1);
}

static int eChar_compare(EChar _this, EChar other) {
  return _this == other ? 0 : (_this > other ? 1 : -1);
}
static int eShort_compare(EShort _this, EShort other) {
  return _this == other ? 0 : (_this > other ? 1 : -1);
}

static int eByte_compare(EByte _this, EByte other) {
  return _this == other ? 0 : (_this > other ? 1 : -1);
}

static int eString_compare(EString _this, EString other) {
  return strcmp(_this, other);
}

static bool node_equals(NODE_T* _this, NODE_T* other) {
	return networkaddr_equal(_this, other);
}

static bool link_equals(LINK_T* _this, LINK_T* other) {
	return ((node_equals(_this->node1, other->node1)
			&& node_equals(_this->node2, other->node2))
			|| (node_equals(_this->node1, other->node2)
					&& node_equals(_this->node2, other->node1)));
}

static bool linkState_equals(LinkState s1, LinkState s2) {
	return s1 == s2;
}

static bool eBoolean_equals(EBoolean b1, EBoolean b2) {
	return b1 == b2;
}

static bool eDouble_equals(EDouble _this, EDouble other) {
  return _this == other;
}

static bool eFloat_equals(EFloat _this, EFloat other) {
  return _this == other;
}

static bool eInt_equals(EInt _this, EInt other) {
  return _this == other;
}

static bool eLong_equals(ELong _this, ELong other) {
  return _this == other;
}

static bool eChar_equals(EChar _this, EChar other) {
  return _this == other;
}

static bool eShort_equals(EShort _this, EShort other) {
  return _this == other;
}

static bool eByte_equals(EByte _this, EByte other) {
  return _this == other;
}

static bool eString_equals(EString _this, EString other) {
  return strcmp(_this, other) == 0;
}

/**
 * This function sets the state of all links to UNCLASSIFIED
 *
 * See component_neighbordiscovery_neighbors()
 * See enum LinkState
 */
static void prepareLinks() {
	LINK_T* link;
	for (link = list_head(component_neighbordiscovery_neighbors());
			link != NULL; link = list_item_next(link)) {
		link->state = UNCLASSIFIED;
	}
}
// --- End of default cMoflon code
