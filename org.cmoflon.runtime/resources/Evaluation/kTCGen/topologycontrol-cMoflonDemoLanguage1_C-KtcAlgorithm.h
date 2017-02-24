#ifndef __TOPOLOGYCONTROL__KTCALGORITHM_H_
#define __TOPOLOGYCONTROL__KTCALGORITHM_H_

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

#ifndef COMPONENT_TOPOLOGYCONTROL_KTCALGORITHM_K
#define COMPONENT_TOPOLOGYCONTROL_KTCALGORITHM_K 1.2
#endif

#ifndef COMPONENT_TOPOLOGYCONTROL_KTCALGORITHM_UPDATEINTERVAL
#define COMPONENT_TOPOLOGYCONTROL_KTCALGORITHM_UPDATEINTERVAL 300
#endif

#ifndef MAX_MATCH_COUNT
#define MAX_MATCH_COUNT 20
#endif
typedef struct match{
	struct match_t* next;
	void** match;
}match_t;

typedef networkaddr_t NODE_T;


typedef neighbor_t LINK_T;


typedef bool EBoolean;

typedef double EDouble;

typedef float EFloat;

typedef int EInt;

typedef long ELong;

typedef char EChar;

typedef short EShort;

typedef char EByte;

typedef const char* EString;

// --- Begin of default cMoflon type definitions
typedef struct {
	NODE_T* node;
}TOPOLOGYCONTROLALGORITHM_T;

// --- End of default cMoflon type definitions

// --- Begin of user-defined algorithm-independent type definitions (Path: 'injection/custom-typedefs.c')
// Algorithm-independent type definitions.
// --- End of user-defined algorithm-independent type definitions

// --- Begin of user-defined type definitions for KtcAlgorithm(Path: 'injection/custom-typedefs_KtcAlgorithm.c')
typedef struct LSTARKTCALGORITHM_T LSTARKTCALGORITHM_T;
typedef struct LMSTALGORITHM_T LMSTALGORITHM_T;
typedef struct TREE_T TREE_T;
typedef struct TREEENTRY_T TREEENTRY_T;

typedef struct  {
	EDouble k;
	NODE_T* node;
}KTCALGORITHM_T;
// --- End of user-defined type definitions for KtcAlgorithm

//Begin of non SDM implemented methods
void ktcAlgorithm_run(KTCALGORITHM_T* this);
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
EDouble ktcAlgorithm_getK(KTCALGORITHM_T* _this);
void ktcAlgorithm_setK(KTCALGORITHM_T* _this, EDouble value);
//End of declarations for k

//Begin of declarations for node
NODE_T* ktcAlgorithm_getNode(KTCALGORITHM_T* _this);
void ktcAlgorithm_setNode(KTCALGORITHM_T* _this, NODE_T* value);
//End of declarations for node

//Begin of compare declarations
int eBoolean_compare(EBoolean _this, EBoolean other);
int eDouble_compare(EDouble _this, EDouble other);
int eFloat_compare(EFloat _this, EFloat other);
int eInt_compare(EInt _this, EInt other);
int eLong_compare(ELong _this, ELong other);
int eChar_compare(EChar _this, EChar other);
int eShort_compare(EShort _this, EShort other);
int eByte_compare(EByte _this, EByte other);
int eString_compare(EString _this, EString other);
int node_compare(NODE_T* _this, NODE_T* other);
int link_compare(LINK_T* _this, LINK_T* other);
int topologyControlAlgorithm_compare(TOPOLOGYCONTROLALGORITHM_T* _this, TOPOLOGYCONTROLALGORITHM_T* other);
int ktcAlgorithm_compare(KTCALGORITHM_T* _this, KTCALGORITHM_T* other);
//End of compare declarations

//Begin of equals declarations
bool eBoolean_equals(EBoolean _this, EBoolean other);
bool eDouble_equals(EDouble _this, EDouble other);
bool eFloat_equals(EFloat _this, EFloat other);
bool eInt_equals(EInt _this, EInt other);
bool eLong_equals(ELong _this, ELong other);
bool eChar_equals(EChar _this, EChar other);
bool eShort_equals(EShort _this, EShort other);
bool eByte_equals(EByte _this, EByte other);
bool eString_equals(EString _this, EString other);
bool node_equals(NODE_T* _this, NODE_T* other);
bool link_equals(LINK_T* _this, LINK_T* other);
bool topologyControlAlgorithm_equals(TOPOLOGYCONTROLALGORITHM_T* _this, TOPOLOGYCONTROLALGORITHM_T* other);
bool ktcAlgorithm_equals(KTCALGORITHM_T* _this, KTCALGORITHM_T* other);
//End of equals declarations

#endif /* __TOPOLOGYCONTROL__KTCALGORITHM_H_ */
