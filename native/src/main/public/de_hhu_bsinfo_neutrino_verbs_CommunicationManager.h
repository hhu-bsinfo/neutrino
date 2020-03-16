/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class de_hhu_bsinfo_neutrino_verbs_CommunicationManager */

#ifndef _Included_de_hhu_bsinfo_neutrino_verbs_CommunicationManager
#define _Included_de_hhu_bsinfo_neutrino_verbs_CommunicationManager
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     de_hhu_bsinfo_neutrino_verbs_CommunicationManager
 * Method:    getAddressInfo0
 * Signature: (Ljava/lang/String;Ljava/lang/String;JJ)V
 */
JNIEXPORT void JNICALL Java_de_hhu_bsinfo_neutrino_verbs_CommunicationManager_getAddressInfo0
  (JNIEnv *, jclass, jstring, jstring, jlong, jlong);

/*
 * Class:     de_hhu_bsinfo_neutrino_verbs_CommunicationManager
 * Method:    freeAddressInfo0
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_de_hhu_bsinfo_neutrino_verbs_CommunicationManager_freeAddressInfo0
  (JNIEnv *, jclass, jlong, jlong);

/*
 * Class:     de_hhu_bsinfo_neutrino_verbs_CommunicationManager
 * Method:    createEndpoint0
 * Signature: (JJJJ)V
 */
JNIEXPORT void JNICALL Java_de_hhu_bsinfo_neutrino_verbs_CommunicationManager_createEndpoint0
  (JNIEnv *, jclass, jlong, jlong, jlong, jlong);

/*
 * Class:     de_hhu_bsinfo_neutrino_verbs_CommunicationManager
 * Method:    destroyEndpoint0
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_de_hhu_bsinfo_neutrino_verbs_CommunicationManager_destroyEndpoint0
  (JNIEnv *, jclass, jlong, jlong);

/*
 * Class:     de_hhu_bsinfo_neutrino_verbs_CommunicationManager
 * Method:    listen0
 * Signature: (JIJ)V
 */
JNIEXPORT void JNICALL Java_de_hhu_bsinfo_neutrino_verbs_CommunicationManager_listen0
  (JNIEnv *, jclass, jlong, jint, jlong);

/*
 * Class:     de_hhu_bsinfo_neutrino_verbs_CommunicationManager
 * Method:    getRequest0
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_de_hhu_bsinfo_neutrino_verbs_CommunicationManager_getRequest0
  (JNIEnv *, jclass, jlong, jlong);

/*
 * Class:     de_hhu_bsinfo_neutrino_verbs_CommunicationManager
 * Method:    accept0
 * Signature: (JJJ)V
 */
JNIEXPORT void JNICALL Java_de_hhu_bsinfo_neutrino_verbs_CommunicationManager_accept0
  (JNIEnv *, jclass, jlong, jlong, jlong);

/*
 * Class:     de_hhu_bsinfo_neutrino_verbs_CommunicationManager
 * Method:    reject0
 * Signature: (JJBJ)V
 */
JNIEXPORT void JNICALL Java_de_hhu_bsinfo_neutrino_verbs_CommunicationManager_reject0
  (JNIEnv *, jclass, jlong, jlong, jbyte, jlong);

/*
 * Class:     de_hhu_bsinfo_neutrino_verbs_CommunicationManager
 * Method:    connect0
 * Signature: (JJJ)V
 */
JNIEXPORT void JNICALL Java_de_hhu_bsinfo_neutrino_verbs_CommunicationManager_connect0
  (JNIEnv *, jclass, jlong, jlong, jlong);

/*
 * Class:     de_hhu_bsinfo_neutrino_verbs_CommunicationManager
 * Method:    disconnect0
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_de_hhu_bsinfo_neutrino_verbs_CommunicationManager_disconnect0
  (JNIEnv *, jclass, jlong, jlong);

#ifdef __cplusplus
}
#endif
#endif