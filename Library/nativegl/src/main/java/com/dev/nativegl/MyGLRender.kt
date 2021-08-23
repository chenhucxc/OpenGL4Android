package com.dev.nativegl

import android.opengl.GLSurfaceView
import android.util.Log
import com.dev.nativegl.MyNativeRender.Companion.SAMPLE_TYPE
import com.dev.nativegl.MyNativeRender.Companion.SAMPLE_TYPE_SET_GRAVITY_XY
import com.dev.nativegl.MyNativeRender.Companion.SAMPLE_TYPE_SET_TOUCH_LOC
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * description : clone from https://github.com/githubhaohao/NDK_OpenGLES_3_0
 *
 * @author     : hudongxin
 * @date       : 8/11/21
 */
class MyGLRender : GLSurfaceView.Renderer {
    companion object {
        private const val TAG = "MyGLRender"
    }

    private var mNativeRender: MyNativeRender = MyNativeRender()
    private var mSampleType = 0

    override fun onSurfaceCreated(gl: GL10?, p1: EGLConfig?) {
        mNativeRender.nativeOnSurfaceCreated()
        Log.e(TAG, "onSurfaceCreated() called with: GL_VERSION = [" + gl?.glGetString(GL10.GL_VERSION) + "]")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        mNativeRender.nativeOnSurfaceChanged(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        mNativeRender.nativeOnDrawFrame()
    }

    fun init() {
        mNativeRender.nativeInit()
    }

    fun unInit() {
        mNativeRender.nativeUnInit()
    }

    fun setParamsInt(paramType: Int, value0: Int, value1: Int) {
        if (paramType == SAMPLE_TYPE) {
            mSampleType = value0
        }
        mNativeRender.nativeSetParamsInt(paramType, value0, value1)
    }

    fun setTouchLoc(x: Float, y: Float) {
        mNativeRender.nativeSetParamsFloat(SAMPLE_TYPE_SET_TOUCH_LOC, x, y)
    }

    fun setGravityXY(x: Float, y: Float) {
        mNativeRender.nativeSetParamsFloat(SAMPLE_TYPE_SET_GRAVITY_XY, x, y)
    }

    fun setImageData(format: Int, width: Int, height: Int, bytes: ByteArray?) {
        mNativeRender.nativeSetImageData(format, width, height, bytes)
    }

    fun setImageDataWithIndex(index: Int, format: Int, width: Int, height: Int, bytes: ByteArray?) {
        mNativeRender.nativeSetImageDataWithIndex(index, format, width, height, bytes)
    }

    fun setAudioData(audioData: ShortArray?) {
        mNativeRender.nativeSetAudioData(audioData)
    }

    fun getSampleType(): Int {
        return mSampleType
    }

    fun updateTransformMatrix(rotateX: Float, rotateY: Float, scaleX: Float, scaleY: Float) {
        mNativeRender.nativeUpdateTransformMatrix(rotateX, rotateY, scaleX, scaleY)
    }
}