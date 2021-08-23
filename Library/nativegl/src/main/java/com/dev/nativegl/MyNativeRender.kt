package com.dev.nativegl

import android.util.Log
import java.lang.Exception

/**
 * description : clone from https://github.com/githubhaohao/NDK_OpenGLES_3_0
 *
 * @author     : hudongxin
 * @date       : 8/11/21
 */
class MyNativeRender {

    companion object {
        const val SAMPLE_TYPE = 200

        const val SAMPLE_TYPE_TRIANGLE = SAMPLE_TYPE
        const val SAMPLE_TYPE_TEXTURE_MAP = SAMPLE_TYPE + 1
        const val SAMPLE_TYPE_YUV_TEXTURE_MAP = SAMPLE_TYPE + 2
        const val SAMPLE_TYPE_VAO = SAMPLE_TYPE + 3
        const val SAMPLE_TYPE_FBO = SAMPLE_TYPE + 4
        const val SAMPLE_TYPE_EGL = SAMPLE_TYPE + 5
        const val SAMPLE_TYPE_FBO_LEG = SAMPLE_TYPE + 6
        const val SAMPLE_TYPE_COORD_SYSTEM = SAMPLE_TYPE + 7
        const val SAMPLE_TYPE_BASIC_LIGHTING = SAMPLE_TYPE + 8
        const val SAMPLE_TYPE_TRANS_FEEDBACK = SAMPLE_TYPE + 9
        const val SAMPLE_TYPE_MULTI_LIGHTS = SAMPLE_TYPE + 10
        const val SAMPLE_TYPE_DEPTH_TESTING = SAMPLE_TYPE + 11
        const val SAMPLE_TYPE_INSTANCING = SAMPLE_TYPE + 12
        const val SAMPLE_TYPE_STENCIL_TESTING = SAMPLE_TYPE + 13
        const val SAMPLE_TYPE_BLENDING = SAMPLE_TYPE + 14
        const val SAMPLE_TYPE_PARTICLES = SAMPLE_TYPE + 15
        const val SAMPLE_TYPE_SKYBOX = SAMPLE_TYPE + 16
        const val SAMPLE_TYPE_3D_MODEL = SAMPLE_TYPE + 17
        const val SAMPLE_TYPE_PBO = SAMPLE_TYPE + 18
        const val SAMPLE_TYPE_KEY_BEATING_HEART = SAMPLE_TYPE + 19
        const val SAMPLE_TYPE_KEY_CLOUD = SAMPLE_TYPE + 20
        const val SAMPLE_TYPE_KEY_TIME_TUNNEL = SAMPLE_TYPE + 21
        const val SAMPLE_TYPE_KEY_BEZIER_CURVE = SAMPLE_TYPE + 22
        const val SAMPLE_TYPE_KEY_BIG_EYES = SAMPLE_TYPE + 23
        const val SAMPLE_TYPE_KEY_FACE_SLENDER = SAMPLE_TYPE + 24
        const val SAMPLE_TYPE_KEY_BIG_HEAD = SAMPLE_TYPE + 25
        const val SAMPLE_TYPE_KEY_ROTARY_HEAD = SAMPLE_TYPE + 26
        const val SAMPLE_TYPE_KEY_VISUALIZE_AUDIO = SAMPLE_TYPE + 27
        const val SAMPLE_TYPE_KEY_SCRATCH_CARD = SAMPLE_TYPE + 28
        const val SAMPLE_TYPE_KEY_AVATAR = SAMPLE_TYPE + 29
        const val SAMPLE_TYPE_KEY_SHOCK_WAVE = SAMPLE_TYPE + 30
        const val SAMPLE_TYPE_KEY_MRT = SAMPLE_TYPE + 31
        const val SAMPLE_TYPE_KEY_FBO_BLIT = SAMPLE_TYPE + 32
        const val SAMPLE_TYPE_KEY_TBO = SAMPLE_TYPE + 33
        const val SAMPLE_TYPE_KEY_UBO = SAMPLE_TYPE + 34
        const val SAMPLE_TYPE_KEY_RGB2YUV = SAMPLE_TYPE + 35
        const val SAMPLE_TYPE_KEY_MULTI_THREAD_RENDER = SAMPLE_TYPE + 36
        const val SAMPLE_TYPE_KEY_TEXT_RENDER = SAMPLE_TYPE + 37
        const val SAMPLE_TYPE_KEY_STAY_COLOR = SAMPLE_TYPE + 38
        const val SAMPLE_TYPE_KEY_TRANSITION = SAMPLE_TYPE + 39

        const val SAMPLE_TYPE_SET_TOUCH_LOC = SAMPLE_TYPE + 999
        const val SAMPLE_TYPE_SET_GRAVITY_XY = SAMPLE_TYPE + 1000

        init {
            val start = System.currentTimeMillis()
            val end: Long = try {
                System.loadLibrary("native_render")
                System.currentTimeMillis()
            } catch (e: Exception) {
                e.printStackTrace()
                System.currentTimeMillis()
            }
            Log.e("MyNativeRender", "loadLibrary diff: ${end - start}")
        }

    }

    external fun nativeInit()

    external fun nativeUnInit()

    external fun nativeSetParamsInt(paramType: Int, value0: Int, value1: Int)

    external fun nativeSetParamsFloat(paramType: Int, value0: Float, value1: Float)

    external fun nativeUpdateTransformMatrix(
        rotateX: Float,
        rotateY: Float,
        scaleX: Float,
        scaleY: Float
    )

    external fun nativeSetImageData(format: Int, width: Int, height: Int, bytes: ByteArray?)

    external fun nativeSetImageDataWithIndex(
        index: Int,
        format: Int,
        width: Int,
        height: Int,
        bytes: ByteArray?
    )

    external fun nativeSetAudioData(audioData: ShortArray?)

    external fun nativeOnSurfaceCreated()

    external fun nativeOnSurfaceChanged(width: Int, height: Int)

    external fun nativeOnDrawFrame()
}