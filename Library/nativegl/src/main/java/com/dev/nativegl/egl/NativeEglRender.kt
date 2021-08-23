package com.dev.nativegl.egl

/**
 * description : clone from https://github.com/githubhaohao/NDK_OpenGLES_3_0
 *
 * @author     : hudongxin
 * @date       : 8/11/21
 */
class NativeEglRender {
    external fun nativeEglRenderInit()

    external fun nativeEglRenderSetImageData(data: ByteArray?, width: Int, height: Int)

    external fun nativeEglRenderSetIntParams(paramType: Int, param: Int)

    external fun nativeEglRenderDraw()

    external fun nativeEglRenderUnInit()
}