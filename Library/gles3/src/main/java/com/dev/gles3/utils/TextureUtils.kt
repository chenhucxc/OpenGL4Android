package com.dev.gles3.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES30
import android.opengl.GLUtils
import android.util.Log

/**
 * description : 纹理加载
 *
 * @author     : hudongxin
 * @date       : 8/17/21
 */
object TextureUtils {
    private const val TAG = "TextureUtils"

    fun loadTexture(context: Context, resourceId: Int): Int {
        val textureIds = IntArray(1)
        // 创建一个纹理对象
        GLES30.glGenTextures(1, textureIds, 0)
        if (textureIds[0] == 0) {
            Log.e(TAG, "Could not generate a new OpenGL textureId object.")
            return 0
        }
        val options = BitmapFactory.Options()
        // 这里需要加载原图未经缩放的数据
        options.inScaled = false
        val mBitmap = BitmapFactory.decodeResource(context.resources, resourceId, options)
        if (mBitmap == null) {
            Log.e(TAG, "Resource ID $resourceId could not be decoded.")
            GLES30.glDeleteTextures(1, textureIds, 0)
            return 0
        }
        // 绑定纹理到OpenGL
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureIds[0])

        // 设置默认的纹理过滤参数
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)

        // 加载bitmap到纹理中
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, mBitmap, 0)

        // 生成MIP贴图
        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D)

        // 数据如果已经被加载进OpenGL,则可以回收该bitmap
        mBitmap.recycle()

        // 取消绑定纹理
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)

        return textureIds[0]
    }
}