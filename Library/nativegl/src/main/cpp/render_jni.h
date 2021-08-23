//
// Created by hdx on 8/11/21.
//

#ifndef LEARNINGOPENGL_RENDER_JNI_H
#define LEARNINGOPENGL_RENDER_JNI_H

#include <jni.h>

#define NativeRender(sig) Java_com_dev_nativegl_MyNativeRender_##sig
#define NativeEglRender(sig) Java_com_dev_nativegl_egl_NativeEglRender_##sig

#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_dev_learning_nativegl_MyNativeRender
 * Method:    native_Init
 * Signature: ()V
 */
JNIEXPORT JNICALL void
NativeRender(nativeInit)(JNIEnv *env, jobject instance);

/*
 * Class:     com_dev_learning_nativegl_MyNativeRender
 * Method:    native_UnInit
 * Signature: ()V
 */
JNIEXPORT void JNICALL NativeRender(nativeUnInit)(JNIEnv *env, jobject instance);

/*
 * Class:     com_dev_learning_nativegl_MyNativeRender
 * Method:    native_SetImageData
 * Signature: (III[B)V
 */
JNIEXPORT void JNICALL NativeRender(nativeSetImageData)
        (JNIEnv *env, jobject instance, jint format, jint width, jint height, jbyteArray imageData);

/*
 * Class:     com_dev_learning_nativegl_MyNativeRender
 * Method:    native_SetImageDataWithIndex
 * Signature: (IIII[B)V
 */
JNIEXPORT void JNICALL NativeRender(nativeSetImageDataWithIndex)
        (JNIEnv *env, jobject instance, jint index, jint format, jint width, jint height,
         jbyteArray imageData);

/*
 * Class:     com_dev_learning_nativegl_MyNativeRender
 * Method:    native_SetParamsInt
 * Signature: (III)V
 */
JNIEXPORT void JNICALL NativeRender(nativeSetParamsInt)
        (JNIEnv *env, jobject instance, jint paramType, jint value0, jint value1);

/*
 * Class:     com_dev_learning_nativegl_MyNativeRender
 * Method:    native_SetParamsFloat
 * Signature: (IFF)V
 */
JNIEXPORT void JNICALL NativeRender(nativeSetParamsFloat)
        (JNIEnv *env, jobject instance, jint paramType, jfloat value0, jfloat value1);


/*
 * Class:     com_dev_learning_nativegl_MyNativeRender
 * Method:    native_SetAudioData
 * Signature: ([B)V
 */
JNIEXPORT void JNICALL NativeRender(nativeSetAudioData)
        (JNIEnv *env, jobject instance, jshortArray data);

/*
 * Class:     com_dev_learning_nativegl_MyNativeRender
 * Method:    native_UpdateTransformMatrix
 * Signature: (FFFF)V
 */
JNIEXPORT void JNICALL
NativeRender(nativeUpdateTransformMatrix)(JNIEnv *env, jobject instance, jfloat rotateX,
                                           jfloat rotateY, jfloat scaleX, jfloat scaleY);

/*
 * Class:     com_dev_learning_nativegl_MyNativeRender
 * Method:    native_OnSurfaceCreated
 * Signature: ()V
 */
JNIEXPORT void JNICALL NativeRender(nativeOnSurfaceCreated)(JNIEnv *env, jobject instance);

/*
 * Class:     com_dev_learning_nativegl_MyNativeRender
 * Method:    native_OnSurfaceChanged
 * Signature: (II)V
 */
JNIEXPORT void JNICALL NativeRender(nativeOnSurfaceChanged)
        (JNIEnv *env, jobject instance, jint width, jint height);

/*
 * Class:     com_dev_learning_nativegl_MyNativeRender
 * Method:    native_OnDrawFrame
 * Signature: ()V
 */
JNIEXPORT void JNICALL NativeRender(nativeOnDrawFrame)(JNIEnv *env, jobject instance);


/*
 * Class:     com_dev_learning_nativegl_egl_NativeBgRender
 * Method:    native_EglRenderInit
 * Signature: ()V
 */
JNIEXPORT void JNICALL NativeEglRender(nativeEglRenderInit)(JNIEnv *env, jobject instance);

/*
 * Class:     com_dev_learning_nativegl_egl_NativeBgRender
 * Method:    native_EglRenderSetImageData
 * Signature: ([BII)V
 */
JNIEXPORT void JNICALL
NativeEglRender(nativeEglRenderSetImageData)(JNIEnv *env, jobject instance, jbyteArray data,
                                           jint width, jint height);

/*
 * Class:     com_dev_learning_nativegl_egl_NativeBgRender
 * Method:    native_EglRenderSetIntParams
 * Signature: (II)V
 */
JNIEXPORT void JNICALL
NativeEglRender(nativeEglRenderSetIntParams)(JNIEnv *env, jobject instance, jint type, jint param);

/*
 * Class:     com_dev_learning_nativegl_egl_NativeBgRender
 * Method:    native_EglRenderDraw
 * Signature: ()V
 */
JNIEXPORT void JNICALL NativeEglRender(nativeEglRenderDraw)(JNIEnv *env, jobject instance);

/*
 * Class:     com_dev_learning_nativegl_egl_NativeBgRender
 * Method:    natuve_BgRenderUnInit
 * Signature: ()V
 */
JNIEXPORT void JNICALL NativeEglRender(nativeEglRenderUnInit)(JNIEnv *env, jobject instance);

#ifdef __cplusplus
}
#endif

#endif //LEARNINGOPENGL_RENDER_JNI_H
