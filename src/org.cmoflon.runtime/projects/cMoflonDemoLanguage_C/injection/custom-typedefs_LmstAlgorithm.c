typedef struct KTCALGORITHM_T KTCALGORITHM_T;
typedef struct LSTARKTCALGORITHM_T LSTARKTCALGORITHM_T;

// Forward declaration
struct TREE_T;

typedef struct {
	NODE_T* node;
	struct TREE_T* tree;
}LMSTALGORITHM_T;

typedef struct TREE_T{
	LMSTALGORITHM_T* algo;
	list_t entries;
	struct memb* mem;
}TREE_T;

typedef struct {
	struct TREEENTRY_T* next;
	NODE_T* node;
	LINK_T* parent;
	TREE_T* tree;
	bool isInTree;
}TREEENTRY_T;
