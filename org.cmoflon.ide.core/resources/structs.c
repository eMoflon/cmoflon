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

struct TREE_T;

typedef struct {
	NODE_T* node;
	struct TREE_T* lmst;
}LMSTALGORITHM_T;

typedef struct TREE_T{
	LMSTALGORITHM_T* algo;
	list_t lmstEntries;
	struct memb* mem;
}TREE_T;

typedef struct {
	struct TREEENTRY_T* next;
	NODE_T* node;
	LINK_T* selectedLink;
	TREE_T* algorithm;
	bool isInTree;
}TREEENTRY_T;
