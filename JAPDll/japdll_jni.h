/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class gui_JAPDll */

#ifndef _Included_gui_JAPDll
#define _Included_gui_JAPDll
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     gui_JAPDll
 * Method:    setWindowOnTop_dll
 * Signature: (Ljava/lang/String;Z)V
 */
JNIEXPORT void JNICALL Java_gui_JAPDll_setWindowOnTop_1dll
  (JNIEnv *, jclass, jstring, jboolean);

JNIEXPORT jboolean JNICALL Java_gui_JAPDll_setTooltipText_1dll
  (JNIEnv *, jclass, jstring);

/*
 * Class:     gui_JAPDll
 * Method:    hideWindowInTaskbar_dll
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_gui_JAPDll_hideWindowInTaskbar_1dll
  (JNIEnv *, jclass, jstring);

/*
 * Class:     gui_JAPDll
 * Method:    setWindowIcon_dll
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_gui_JAPDll_setWindowIcon_1dll
  (JNIEnv *, jclass, jstring);

/*
 * Class:     gui_JAPDll
 * Method:    onTraffic_dll
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_gui_JAPDll_onTraffic_1dll
  (JNIEnv *, jclass);
  
/*
 * Class:     gui_JAPDll
 * Method:    popupClosed_dll
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_gui_JAPDll_popupClosed_1dll
  (JNIEnv *, jclass);

/*
 * Class:     gui_JAPDll
 * Method:    hideSystray_dll
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_gui_JAPDll_hideSystray_1dll
  (JNIEnv *, jclass);
  
/*
 * Class:     gui_JAPDll
 * Method:    showWindowFromTaskbar_dll
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_gui_JAPDll_showWindowFromTaskbar_1dll
  (JNIEnv *, jclass);

/*
 * Class:     gui_JAPDll
 * Method:    getDllVersion_dll
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_gui_JAPDll_getDllVersion_1dll
  (JNIEnv *, jclass);

/*
 * Class:     gui_JAPDll
 * Method:    getDllfileName_dll
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_gui_JAPDll_getDllFileName_1dll
  (JNIEnv *, jclass);

#ifdef __cplusplus
}
#endif
#endif
