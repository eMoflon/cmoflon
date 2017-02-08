#ifndef __TOPOLOGYCONTROL__CMOFLONDEMOLANGUAGE2_C_H_
#define __TOPOLOGYCONTROL__CMOFLONDEMOLANGUAGE2_C_H_

#include <stdlib.h>
#include <stdio.h>
#include <float.h>
#include "contiki.h"
#include "contiki-lib.h"
#include "../../app-conf.h"
#include "../../lib/boot.h"
#include "../../lib/components.h"
#include "../../lib/utilities.h"
#include "../../lib/neighbors.h"
#include "../../lib/networkaddr.h"
#include "dev/watchdog.h"

#ifndef COMPONENT_TOPOLOGYCONTROL_CMOFLONDEMOLANGUAGE2_C_UPDATEINTERVAL
#define COMPONENT_TOPOLOGYCONTROL_CMOFLONDEMOLANGUAGE2_C_UPDATEINTERVAL 300
#endif

#ifndef MAX_MATCH_COUNT
#define MAX_MATCH_COUNT 20
#endif
typedef struct match{
	struct match_t* next;
	void** match;
}match_t;

typedef neighbor_t LINK_T;

typedef networkaddr_t NODE_T;

typedef bool EBoolean;

typedef double EDouble;

typedef int EInt;

typedef struct {
	NODE_T* node;
}TOPOLOGYCONTROLALGORITHM_T;

typedef struct  {
	EDouble k;
	NODE_T* node;
}KTCALGORITHM_T;

typedef struct  {
	NODE_T* node;
}MAXPOWERALGORITHM_T;

typedef struct {
	EDouble k;
	EDouble stretchFactor;
	NODE_T* node;
}LSTARKTCALGORITHM_T;

struct lmst_t;

typedef struct {
	NODE_T* node;
	struct lmst_t* lmst;
}LMSTALGORITHM_T;

typedef struct lmst_t{
	LMSTALGORITHM_T* algo;
	list_t lmstEntries;
	struct memb* mem;
}LMST_T;

typedef struct {
	struct LMSTENTRY_T* next;
	NODE_T* node;
	LINK_T* selectedLink;
	LMST_T* algorithm;
	bool isInTree;
}LMSTENTRY_T;
//Begin of non SDM implemented methods
void lmstalgorithm_prepareLMSTEntries(LMSTALGORITHM_T* this);
void lmstalgorithm_run(LMSTALGORITHM_T* this);
LINK_T* lmstalgorithm_findShortestUnconnectedLink(LMSTALGORITHM_T* this);
void lmstalgorithm_cleanupLMST(LMSTALGORITHM_T* this);
//End of non SDM implemented methods

//Begin of declarations for hopcount
EInt node_getHopcount(NODE_T* _this);
void node_setHopcount(NODE_T* _this, EInt value);
//End of declarations for hopcount

//Begin of declarations for incomingLinks
list_t node_getIncomingLinks(NODE_T* _this);
void node_addIncomingLinks(NODE_T* _this, LINK_T* value);
void node_removeIncomingLinks(NODE_T* _this, LINK_T* item);
bool node_containsIncomingLinks(NODE_T* _this, LINK_T* value);
bool node_isIncomingLinks(void* candidate, void* _this);
//End of declarations for incomingLinks

//Begin of declarations for outgoingLinks
list_t node_getOutgoingLinks(NODE_T* _this);
void node_addOutgoingLinks(NODE_T* _this, LINK_T* value);
void node_removeOutgoingLinks(NODE_T* _this, LINK_T* item);
bool node_containsOutgoingLinks(NODE_T* _this, LINK_T* value);
bool node_isOutgoingLinks(void* candidate, void* _this);
//End of declarations for outgoingLinks

//Begin of declarations for neighborhood
list_t node_getNeighborhood(NODE_T* _this);
void node_addNeighborhood(NODE_T* _this, LINK_T* value);
void node_removeNeighborhood(NODE_T* _this, LINK_T* item);
bool node_containsNeighborhood(NODE_T* _this, LINK_T* value);
bool node_isNeighborhood(void* candidate, void* _this);
//End of declarations for neighborhood

//Begin of declarations for weight
EDouble link_getWeight(LINK_T* _this);
void link_setWeight(LINK_T* _this, EDouble value);
//End of declarations for weight

//Begin of declarations for marked
LinkState link_getMarked(LINK_T* _this);
void link_setMarked(LINK_T* _this, LinkState value);
//End of declarations for marked

//Begin of declarations for target
NODE_T* link_getTarget(LINK_T* _this);
void link_setTarget(LINK_T* _this, NODE_T* value);
//End of declarations for target

//Begin of declarations for source
NODE_T* link_getSource(LINK_T* _this);
void link_setSource(LINK_T* _this, NODE_T* value);
//End of declarations for source

//Begin of declarations for node
NODE_T* lmstalgorithm_getNode(LMSTALGORITHM_T* _this);
void lmstalgorithm_setNode(LMSTALGORITHM_T* _this, NODE_T* value);
//End of declarations for node

//Begin of declarations for lmst
LMST_T* lmstalgorithm_getLmst(LMSTALGORITHM_T* _this);
void lmstalgorithm_setLmst(LMSTALGORITHM_T* _this, LMST_T* value);
//End of declarations for lmst

//Begin of declarations for algorithm
LMSTALGORITHM_T* lmst_getAlgorithm(LMST_T* _this);
void lmst_setAlgorithm(LMST_T* _this, LMSTALGORITHM_T* value);
//End of declarations for algorithm

//Begin of declarations for lmstEntries
list_t lmst_getLmstEntries(LMST_T* _this);
void lmst_addLmstEntries(LMST_T* _this, LMSTENTRY_T* value);
void lmst_removeLmstEntries(LMST_T* _this, LMSTENTRY_T* item);
bool lmst_containsLmstEntries(LMST_T* _this, LMSTENTRY_T* value);
bool lmst_isLmstEntries(void* candidate, void* _this);
//End of declarations for lmstEntries

//Begin of declarations for isInTree
bool lmstentry_isIsInTree(LMSTENTRY_T* _this);
void lmstentry_setIsInTree(LMSTENTRY_T* _this, EBoolean value);
//End of declarations for isInTree

//Begin of declarations for lmst
LMST_T* lmstentry_getLmst(LMSTENTRY_T* _this);
void lmstentry_setLmst(LMSTENTRY_T* _this, LMST_T* value);
//End of declarations for lmst

//Begin of declarations for node
NODE_T* lmstentry_getNode(LMSTENTRY_T* _this);
void lmstentry_setNode(LMSTENTRY_T* _this, NODE_T* value);
//End of declarations for node

//Begin of declarations for selectedLink
LINK_T* lmstentry_getSelectedLink(LMSTENTRY_T* _this);
void lmstentry_setSelectedLink(LMSTENTRY_T* _this, LINK_T* value);
//End of declarations for selectedLink

//Begin of compare declarations
int eboolean_compare(EBoolean _this, EBoolean other);
int edouble_compare(EDouble _this, EDouble other);
int eint_compare(EInt _this, EInt other);
int node_compare(NODE_T* _this, NODE_T* other);
int link_compare(LINK_T* _this, LINK_T* other);
int topologycontrolalgorithm_compare(TOPOLOGYCONTROLALGORITHM_T* _this, TOPOLOGYCONTROLALGORITHM_T* other);
int lmstalgorithm_compare(LMSTALGORITHM_T* _this, LMSTALGORITHM_T* other);
int lmst_compare(LMST_T* _this, LMST_T* other);
int lmstentry_compare(LMSTENTRY_T* _this, LMSTENTRY_T* other);
//End of compare declarations

//Begin of equals declarations
bool eboolean_equals(EBoolean _this, EBoolean other);
bool edouble_equals(EDouble _this, EDouble other);
bool eint_equals(EInt _this, EInt other);
bool node_equals(NODE_T* _this, NODE_T* other);
bool link_equals(LINK_T* _this, LINK_T* other);
bool topologycontrolalgorithm_equals(TOPOLOGYCONTROLALGORITHM_T* _this, TOPOLOGYCONTROLALGORITHM_T* other);
bool lmstalgorithm_equals(LMSTALGORITHM_T* _this, LMSTALGORITHM_T* other);
bool lmst_equals(LMST_T* _this, LMST_T* other);
bool lmstentry_equals(LMSTENTRY_T* _this, LMSTENTRY_T* other);
//End of equals declarations

#endif /* __TOPOLOGYCONTROL__CMOFLONDEMOLANGUAGE2_C_H_ */
