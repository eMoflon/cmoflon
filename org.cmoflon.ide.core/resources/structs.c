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
}LMSTALGORITHM_T;

typedef LMSTALGORITHM_T LMST_T;

typedef struct {
	NODE_T* node;
	LINK_T* selectedLink;
	LMST_T algorithm;
}LMSTENTRY_T;
