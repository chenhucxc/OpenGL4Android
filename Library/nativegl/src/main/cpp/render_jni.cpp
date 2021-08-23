//
// Created by hdx on 8/11/21.
//

#include "render_jni.h"
#include "util/LogUtil.h"
#include <MyGLRenderContext.h>
#include <EGLRender.h>

JNIEXPORT void JNICALL
NativeRender(nativeInit)(JNIEnv *env, jobject instance) {
    MyGLRenderContext::GetInstance();
}

JNIEXPORT void JNICALL NativeRender(nativeUnInit)(JNIEnv *env, jobject instance) {
    MyGLRenderContext::DestroyInstance();
}

JNIEXPORT void JNICALL NativeRender(nativeSetImageData)
        (JNIEnv *env, jobject instance, jint format, jint width, jint height,
         jbyteArray imageData) {
    int len = env->GetArrayLength(imageData);
    uint8_t *buf = new uint8_t[len];
    env->GetByteArrayRegion(imageData, 0, len, reinterpret_cast<jbyte *>(buf));
    MyGLRenderContext::GetInstance()->SetImageData(format, width, height, buf);
    delete[] buf;
    env->DeleteLocalRef(imageData);
}

JNIEXPORT void JNICALL NativeRender(nativeSetImageDataWithIndex)
        (JNIEnv *env, jobject instance, jint index, jint format, jint width, jint height,
         jbyteArray imageData) {
    int len = env->GetArrayLength(imageData);
    uint8_t *buf = new uint8_t[len];
    env->GetByteArrayRegion(imageData, 0, len, reinterpret_cast<jbyte *>(buf));
    MyGLRenderContext::GetInstance()->SetImageDataWithIndex(index, format, width, height, buf);
    delete[] buf;
    env->DeleteLocalRef(imageData);
}

JNIEXPORT void JNICALL NativeRender(nativeSetParamsInt)
        (JNIEnv *env, jobject instance, jint paramType, jint value0, jint value1) {
    MyGLRenderContext::GetInstance()->SetParamsInt(paramType, value0, value1);
}

JNIEXPORT void JNICALL NativeRender(nativeSetParamsFloat)
        (JNIEnv *env, jobject instance, jint paramType, jfloat value0, jfloat value1) {
    MyGLRenderContext::GetInstance()->SetParamsFloat(paramType, value0, value1);
}

JNIEXPORT void JNICALL NativeRender(nativeSetAudioData)
        (JNIEnv *env, jobject instance, jshortArray data) {
    int len = env->GetArrayLength(data);
    short *pShortBuf = new short[len];
    env->GetShortArrayRegion(data, 0, len, reinterpret_cast<jshort *>(pShortBuf));
    MyGLRenderContext::GetInstance()->SetParamsShortArr(pShortBuf, len);
    delete[] pShortBuf;
    env->DeleteLocalRef(data);
}

JNIEXPORT void JNICALL
NativeRender(nativeUpdateTransformMatrix)(JNIEnv *env, jobject instance, jfloat rotateX,
                                            jfloat rotateY, jfloat scaleX, jfloat scaleY) {
    MyGLRenderContext::GetInstance()->UpdateTransformMatrix(rotateX, rotateY, scaleX, scaleY);
}

JNIEXPORT void JNICALL NativeRender(nativeOnSurfaceCreated)(JNIEnv *env, jobject instance) {
    MyGLRenderContext::GetInstance()->OnSurfaceCreated();
}

JNIEXPORT void JNICALL NativeRender(nativeOnSurfaceChanged)
        (JNIEnv *env, jobject instance, jint width, jint height) {
    MyGLRenderContext::GetInstance()->OnSurfaceChanged(width, height);
}

JNIEXPORT void JNICALL NativeRender(nativeOnDrawFrame)(JNIEnv *env, jobject instance) {
    MyGLRenderContext::GetInstance()->OnDrawFrame();
}

JNIEXPORT void JNICALL NativeEglRender(nativeEglRenderInit)(JNIEnv *env, jobject instance) {
    EGLRender::GetInstance()->Init();
}

JNIEXPORT void JNICALL
NativeEglRender(nativeEglRenderSetImageData)(JNIEnv *env, jobject instance, jbyteArray data,
                                               jint width, jint height) {
    int len = env->GetArrayLength(data);
    uint8_t *buf = new uint8_t[len];
    env->GetByteArrayRegion(data, 0, len, reinterpret_cast<jbyte *>(buf));
    EGLRender::GetInstance()->SetImageData(buf, width, height);
    delete[] buf;
    env->DeleteLocalRef(data);
}

JNIEXPORT void JNICALL
NativeEglRender(nativeEglRenderSetIntParams)(JNIEnv *env, jobject instance, jint type,
                                               jint param) {
    EGLRender::GetInstance()->SetIntParams(type, param);
}

JNIEXPORT void JNICALL NativeEglRender(nativeEglRenderDraw)(JNIEnv *env, jobject instance) {
    EGLRender::GetInstance()->Draw();
}

JNIEXPORT void JNICALL NativeEglRender(nativeEglRenderUnInit)(JNIEnv *env, jobject instance) {
    EGLRender::GetInstance()->UnInit();
}