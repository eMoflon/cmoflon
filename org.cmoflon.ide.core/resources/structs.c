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

typedef struct {
	NODE_T* node;
	LMST_T* lmst;
}LMSTALGORITHM_T;

typedef struct {
	LMSTALGORITHM_T* algo;
	list_t lmstEntries;
	struct memb* mem;
}LMST_T;

typedef struct {
	struct LMSTENTRY_T* next;
	NODE_T* node;
	LINK_T* selectedLink;
	LMST_T* algorithm;
}LMSTENTRY_T;
