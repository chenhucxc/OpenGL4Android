package com.dev.gles3.utils

import android.opengl.GLES30
import android.util.Log


/**
 * description : openGL 编译，链接工具类
 *
 * @author     : hudongxin
 * @date       : 8/14/21
 */
object ShaderUtils {

    private const val TAG = "ShaderUtils"

    /**
     * 编译顶点着色器
     * @param shaderCode
     */
    fun compileVertexShader(shaderCode: String): Int {
        return compileShader(GLES30.GL_VERTEX_SHADER, shaderCode)
    }

    /**
     * 编译片段着色器
     * @param shaderCode
     */
    fun compileFragmentShader(shaderCode: String): Int {
        return compileShader(GLES30.GL_FRAGMENT_SHADER, shaderCode)
    }

    /**
     * 编译
     * @param type       顶点着色器:GLES30.GL_VERTEX_SHADER
     * 片段着色器:GLES30.GL_FRAGMENT_SHADER
     * @param shaderCode
     */
    private fun compileShader(type: Int, shaderCode: String): Int {
        // 创建一个着色器
        val shaderId = GLES30.glCreateShader(type)
        Log.i(TAG, "compileShader: shaderId = $shaderId")
        return if (shaderId != 0) {
            GLES30.glShaderSource(shaderId, shaderCode)
            GLES30.glCompileShader(shaderId)
            // 检测状态
            val compileStatus = IntArray(1)
            GLES30.glGetShaderiv(shaderId, GLES30.GL_COMPILE_STATUS, compileStatus, 0)
            if (compileStatus[0] == 0) {
                val logInfo = GLES30.glGetShaderInfoLog(shaderId)
                System.err.println(logInfo)
                // 创建失败
                GLES30.glDeleteShader(shaderId)
                return 0
            }
            shaderId
        } else {
            // 创建失败
            0
        }
    }

    /**
     * 链接小程序
     * @param vertexShaderId   顶点着色器
     * @param fragmentShaderId 片段着色器
     */
    fun linkProgram(vertexShaderId: Int, fragmentShaderId: Int): Int {
        // 创建一个空的OpenGLES程序
        val programId = GLES30.glCreateProgram()
        Log.i(TAG, "linkProgram: programId = $programId")
        return if (programId != 0) {
            // 将顶点着色器加入到程序
            GLES30.glAttachShader(programId, vertexShaderId)
            // 将片元着色器加入到程序中
            GLES30.glAttachShader(programId, fragmentShaderId)
            // 链接着色器程序
            GLES30.glLinkProgram(programId)
            val linkStatus = IntArray(1)
            GLES30.glGetProgramiv(programId, GLES30.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] == 0) {
                val logInfo = GLES30.glGetProgramInfoLog(programId)
                System.err.println(logInfo)
                GLES30.glDeleteProgram(programId)
                return 0
            }
            programId
        } else {
            // 创建失败
            0
        }
    }

    /**
     * 验证程序片段是否有效
     * @param programObjectId
     */
    fun validProgram(programObjectId: Int): Boolean {
        GLES30.glValidateProgram(programObjectId)
        val programStatus = IntArray(1)
        GLES30.glGetProgramiv(programObjectId, GLES30.GL_VALIDATE_STATUS, programStatus, 0)
        return programStatus[0] != 0
    }
}