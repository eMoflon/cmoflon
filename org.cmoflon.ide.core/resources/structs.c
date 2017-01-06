typedef enum linkstate { UNCLASSIFIED, ACTIVE, INACTIVE, PROCESSED }LinkState;
typedef struct KTCALGORITHM_T {
	double k;
	NODE_T* node;
};

typedef struct MAXPOWERALGORITHM_T {
	EDouble k;
	NODE_T* node;
};

typedef struct {
	neighbor_t* link;
	LinkState state;
}LINK_T;