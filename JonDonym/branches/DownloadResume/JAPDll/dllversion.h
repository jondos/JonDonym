#define VER_MINOR 7
#define VER_MAJOR 4

// Usually you do not change things below this line...
#define L_V 0,VER_MAJOR,VER_MINOR,0
#define S_V(arg) STR(arg)
#define STR(arg) #arg
#define JAPDLL_VERSION_intern 00.VER_MAJOR.00VER_MINOR
#define JAPDLL_VERSION S_V(L_V)
