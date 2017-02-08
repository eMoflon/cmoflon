#ifndef __TOPOLOGYCONTROL__CMOFLONDEMOLANGUAGE_C_H_
#define __TOPOLOGYCONTROL__CMOFLONDEMOLANGUAGE_C_H_

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

#ifndef COMPONENT_TOPOLOGYCONTROL_CMOFLONDEMOLANGUAGE_C_UPDATEINTERVAL
#define COMPONENT_TOPOLOGYCONTROL_CMOFLONDEMOLANGUAGE_C_UPDATEINTERVAL 300
#endif

#ifndef COMPONENT_TOPOLOGYCONTROL_CMOFLONDEMOLANGUAGE_C_K
#define COMPONENT_TOPOLOGYCONTROL_CMOFLONDEMOLANGUAGE_C_K 1.2
#endif

#ifndef COMPONENT_TOPOLOGYCONTROL_CMOFLONDEMOLANGUAGE_C_BROADCASTHOPCOUNT_IMMEDIATE_MIN
#define COMPONENT_TOPOLOGYCONTROL_CMOFLONDEMOLANGUAGE_C_BROADCASTHOPCOUNT_IMMEDIATE_MIN 1
#endif

#ifndef COMPONENT_TOPOLOGYCONTROL_CMOFLONDEMOLANGUAGE_C_BROADCASTHOPCOUNT_SMALLDELAY_MIN
#define COMPONENT_TOPOLOGYCONTROL_CMOFLONDEMOLANGUAGE_C_BROADCASTHOPCOUNT_SMALLDELAY_MIN 55
#endif

#ifndef COMPONENT_TOPOLOGYCONTROL_CMOFLONDEMOLANGUAGE_C_BROADCASTHOPCOUNT_PERIODIC_MIN
#define COMPONENT_TOPOLOGYCONTROL_CMOFLONDEMOLANGUAGE_C_BROADCASTHOPCOUNT_PERIODIC_MIN 270
#endif

#ifndef COMPONENT_TOPOLOGYCONTROL_CMOFLONDEMOLANGUAGE_C_BROADCASTHOPCOUNT_IMMEDIATE_MAX
#define COMPONENT_TOPOLOGYCONTROL_CMOFLONDEMOLANGUAGE_C_BROADCASTHOPCOUNT_IMMEDIATE_MAX 10
#endif

#ifndef COMPONENT_TOPOLOGYCONTROL_CMOFLONDEMOLANGUAGE_C_BROADCASTHOPCOUNT_SMALLDELAY_MAX
#define COMPONENT_TOPOLOGYCONTROL_CMOFLONDEMOLANGUAGE_C_BROADCASTHOPCOUNT_SMALLDELAY_MAX 65
#endif

#ifndef COMPONENT_TOPOLOGYCONTROL_CMOFLONDEMOLANGUAGE_C_STRETCHFACTOR
#define COMPONENT_TOPOLOGYCONTROL_CMOFLONDEMOLANGUAGE_C_STRETCHFACTOR 1.5
#endif

#ifndef COMPONENT_TOPOLOGYCONTROL_CMOFLONDEMOLANGUAGE_C_BROADCASTHOPCOUNT_PERIODIC_MAX
#define COMPONENT_TOPOLOGYCONTROL_CMOFLONDEMOLANGUAGE_C_BROADCASTHOPCOUNT_PERIODIC_MAX 330
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
void lstarktcalgorithm_run(LSTARKTCALGORITHM_T* this);
EBoolean lstarktcalgorithm_evaluateHopcountConstraint(LSTARKTCALGORITHM_T* this, EInt hopCount1, EInt hopCount2, EInt hopCount3, EDouble stretchFactor);
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

//Begin of declarations for k
EDouble lstarktcalgorithm_getK(LSTARKTCALGORITHM_T* _this);
void lstarktcalgorithm_setK(LSTARKTCALGORITHM_T* _this, EDouble value);
//End of declarations for k

//Begin of declarations for stretchFactor
EDouble lstarktcalgorithm_getStretchFactor(LSTARKTCALGORITHM_T* _this);
void lstarktcalgorithm_setStretchFactor(LSTARKTCALGORITHM_T* _this, EDouble value);
//End of declarations for stretchFactor

//Begin of declarations for node
NODE_T* lstarktcalgorithm_getNode(LSTARKTCALGORITHM_T* _this);
void lstarktcalgorithm_setNode(LSTARKTCALGORITHM_T* _this, NODE_T* value);
//End of declarations for node

//Begin of compare declarations
int eboolean_compare(EBoolean _this, EBoolean other);
int edouble_compare(EDouble _this, EDouble other);
int eint_compare(EInt _this, EInt other);
int node_compare(NODE_T* _this, NODE_T* other);
int link_compare(LINK_T* _this, LINK_T* other);
int topologycontrolalgorithm_compare(TOPOLOGYCONTROLALGORITHM_T* _this, TOPOLOGYCONTROLALGORITHM_T* other);
int lstarktcalgorithm_compare(LSTARKTCALGORITHM_T* _this, LSTARKTCALGORITHM_T* other);
//End of compare declarations

//Begin of equals declarations
bool eboolean_equals(EBoolean _this, EBoolean other);
bool edouble_equals(EDouble _this, EDouble other);
bool eint_equals(EInt _this, EInt other);
bool node_equals(NODE_T* _this, NODE_T* other);
bool link_equals(LINK_T* _this, LINK_T* other);
bool topologycontrolalgorithm_equals(TOPOLOGYCONTROLALGORITHM_T* _this, TOPOLOGYCONTROLALGORITHM_T* other);
bool lstarktcalgorithm_equals(LSTARKTCALGORITHM_T* _this, LSTARKTCALGORITHM_T* other);
//End of equals declarations

#endif /* __TOPOLOGYCONTROL__CMOFLONDEMOLANGUAGE_C_H_ */
