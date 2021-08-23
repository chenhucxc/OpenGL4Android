package com.dev.gles3.player

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.*
import android.util.Log
import android.view.Surface
import com.dev.gles3.R
import com.dev.gles3.utils.LoadFileUtils
import com.dev.gles3.utils.ShaderUtils
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * description : VR 视频播放
 *
 * @author     : hudongxin
 * @date       : 8/17/21
 */
class VRGlassGLVideoRenderer constructor(context: Context, glView: GLSurfaceView, playUrl: String) :
    GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "VRGlassGLVideoRenderer"
        const val DISPLAY_NORMAL_MODE = 1
        const val DISPLAY_GLASS_MODE = 2

        // 顶点向量元素个数（x,y,z）
        private const val COORDS_PER_VERTEX = 3

        // 纹理向量元素个数（s,t）
        private const val COORDS_PER_TEXTURE = 2
    }

    // 顶点位置缓存、纹理顶点位置缓存、渲染程序
    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var mTexVertexBuffer: FloatBuffer
    private var mProgram = 0
    private var vCount = 0 // 向量个数

    private var textureId = -1 // 纹理id
    private var mSurfaceTexture: SurfaceTexture? = null // 图片生成的位图

    // 返回属性变量的位置
    private var aPositionLocation = -1 // 顶点
    private var aTextureLocation = -1 // 纹理
    private var projectMatrixLocation = -1 // 投影矩阵
    private var rotateMatrixLocation = -1 // 旋转矩阵
    private var viewMatrixLocation = -1 // 相机矩阵
    private var modelMatrixLocation = -1 // 模型矩阵

    // 相机矩阵、投影矩阵、旋转矩阵、模型矩阵
    private var mViewMatrix = FloatArray(16)
    private var mProjectMatrix = FloatArray(16)
    private var mRotateMatrix = FloatArray(16)

    private var mTempRotateMatrix = FloatArray(16)
    private var mModelMatrix = FloatArray(16)

    // IjkMediaPlayer实例
    private lateinit var ijkMediaPlayer: IjkMediaPlayer
    private var videoPath = playUrl
    private val glSurfaceView = glView
    private val mContext = context

    // 展示模式：分为 全景；VR眼睛
    private var displayMode = DISPLAY_NORMAL_MODE

    // 交互模式：是否响应传感器
    private var interactionModeNormal = false

    // 顶点着色器和片段着色器
    private var vertexShaderStr = LoadFileUtils.readResource(context, R.raw.vertex_vr_shader)
    private var fragmentShaderStr = LoadFileUtils.readResource(context, R.raw.fragment_vr_shader)

    private var screenWidth = 0
    private var screenHeight = 0

    private var mXAngle = 0f
    private var mYAngle = 0f
    private var mZAngle = 0f

    init {
        setRotateAngle(0f, 0f, 1f)
        calculateAttribute()
        initMediaPlayer()
    }

    fun changeInteractionMode() {
        interactionModeNormal = !interactionModeNormal
    }

    fun changeDisplayMode() {
        displayMode = when (displayMode) {
            DISPLAY_NORMAL_MODE -> DISPLAY_GLASS_MODE
            DISPLAY_GLASS_MODE -> DISPLAY_NORMAL_MODE
            else -> DISPLAY_NORMAL_MODE
        }
    }

    fun setRotateMatrix(rotateMatrix: FloatArray) {
        //mTempRotateMatrix = rotateMatrix
        if (interactionModeNormal) {
            System.arraycopy(rotateMatrix, 0, mRotateMatrix, 0, 16)
        }
    }

    fun setRotateMatrixByTouch(rotateMatrix: FloatArray) {
        mTempRotateMatrix = rotateMatrix
        System.arraycopy(rotateMatrix, 0, mRotateMatrix, 0, 16)
    }

    fun getRotateMatrix(): FloatArray {
        val retArr = FloatArray(16)
        System.arraycopy(retArr, 0, mRotateMatrix, 0, 16)
        return retArr
    }

    fun setRotateAngle(xAngle: Float = 0f, yAngle: Float = 0f, zAngle: Float) {
        this.mXAngle = xAngle
        this.mYAngle = yAngle
        this.mZAngle = zAngle
    }

    fun onPause() {
        ijkMediaPlayer.pause()
    }

    fun onStop() {
        ijkMediaPlayer.stop()
        IjkMediaPlayer.native_profileEnd()
    }

    fun release() {
        ijkMediaPlayer.reset()
        ijkMediaPlayer.release()
    }

    /**
     * 计算顶点位置和纹理位置数据
     */
    private fun calculateAttribute() {
        val radius = 2.0f // 球的半径
        val angleSpan = Math.PI / 90f // 将球进行单位切分的角度

        val alVertex: ArrayList<Float> = ArrayList()
        val textureVertex: ArrayList<Float> = ArrayList()

        var vAngle = 0.0
        while (vAngle < Math.PI) {
            var hAngle = 0.0
            while (hAngle < 2 * Math.PI) {
                val x0 = (radius * Math.sin(vAngle) * Math.cos(hAngle)).toFloat()
                val y0 = (radius * Math.sin(vAngle) * Math.sin(hAngle)).toFloat()
                val z0 = (radius * Math.cos(vAngle)).toFloat()

                val x1 = (radius * Math.sin(vAngle) * Math.cos(hAngle + angleSpan)).toFloat()
                val y1 = (radius * Math.sin(vAngle) * Math.sin(hAngle + angleSpan)).toFloat()
                val z1 = (radius * Math.cos(vAngle)).toFloat()

                val x2 =
                    (radius * Math.sin(vAngle + angleSpan) * Math.cos(hAngle + angleSpan)).toFloat()
                val y2 =
                    (radius * Math.sin(vAngle + angleSpan) * Math.sin(hAngle + angleSpan)).toFloat()
                val z2 = (radius * Math.cos(vAngle + angleSpan)).toFloat()

                val x3 = (radius * Math.sin(vAngle + angleSpan) * Math.cos(hAngle)).toFloat()
                val y3 = (radius * Math.sin(vAngle + angleSpan) * Math.sin(hAngle)).toFloat()
                val z3 = (radius * Math.cos(vAngle + angleSpan)).toFloat()

                alVertex.add(x1)
                alVertex.add(y1)
                alVertex.add(z1)
                alVertex.add(x0)
                alVertex.add(y0)
                alVertex.add(z0)
                alVertex.add(x3)
                alVertex.add(y3)
                alVertex.add(z3)

                val s0 = (hAngle / Math.PI / 2).toFloat()
                val s1 = ((hAngle + angleSpan) / Math.PI / 2).toFloat()
                val t0 = (vAngle / Math.PI).toFloat()
                val t1 = ((vAngle + angleSpan) / Math.PI).toFloat()

                textureVertex.add(s1) // x1 y1对应纹理坐标
                textureVertex.add(t0)
                textureVertex.add(s0) // x0 y0对应纹理坐标
                textureVertex.add(t0)
                textureVertex.add(s0) // x3 y3对应纹理坐标
                textureVertex.add(t1)

                alVertex.add(x1)
                alVertex.add(y1)
                alVertex.add(z1)
                alVertex.add(x3)
                alVertex.add(y3)
                alVertex.add(z3)
                alVertex.add(x2)
                alVertex.add(y2)
                alVertex.add(z2)

                textureVertex.add(s1) // x1 y1对应纹理坐标
                textureVertex.add(t0)
                textureVertex.add(s0) // x3 y3对应纹理坐标
                textureVertex.add(t1)
                textureVertex.add(s1) // x2 y3对应纹理坐标
                textureVertex.add(t1)
                hAngle += angleSpan
            }
            vAngle += angleSpan
        }
        vCount = alVertex.size / 3
        vertexBuffer = convertToFloatBuffer(alVertex)
        mTexVertexBuffer = convertToFloatBuffer(textureVertex)
    }

    /**
     * 动态数组转FloatBuffer
     */
    private fun convertToFloatBuffer(data: ArrayList<Float>): FloatBuffer {
        val d = FloatArray(data.size)
        for (i in d.indices) {
            d[i] = data[i]
        }
        val buffer = ByteBuffer.allocateDirect(data.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(d)
        buffer.position(0)
        return buffer
    }

    private fun initMediaPlayer() {
        ijkMediaPlayer = IjkMediaPlayer()
        try {
            ijkMediaPlayer.dataSource = videoPath
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e(TAG, "initMediaPlayer: ${e.message}")
            return
        }
        ijkMediaPlayer.isLooping = true
    }

    /**
     * 初始化纹理id
     */
    private fun loadTexture(): Int {
        val textureIds = IntArray(1)
        GLES30.glGenTextures(1, textureIds, 0)
        if (textureIds[0] == 0) {
            Log.e(TAG, "Could not generate a new OpenGL textureId object.")
            return 0
        }
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureIds[0])
        GLES30.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_MIN_FILTER,
            GL10.GL_LINEAR.toFloat()
        )
        GLES30.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_MAG_FILTER,
            GL10.GL_LINEAR.toFloat()
        )
        GLES30.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_WRAP_S,
            GL10.GL_CLAMP_TO_EDGE
        )
        GLES30.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_WRAP_T,
            GL10.GL_CLAMP_TO_EDGE
        )

        return textureIds[0]
    }

    private fun attachTexture() {
        Log.i(TAG, "attachTexture: textureId  = $textureId")
        mSurfaceTexture = SurfaceTexture(textureId)
        // 监听是否有新的一帧数据到来
        mSurfaceTexture?.setOnFrameAvailableListener {
            //Log.i(TAG, "attachTexture: =======")
            glSurfaceView.requestRender()
        }
        val surface = Surface(mSurfaceTexture)
        ijkMediaPlayer.setSurface(surface)
        surface.release()
    }

    private fun preparePlay() {
        ijkMediaPlayer.prepareAsync()
        ijkMediaPlayer.setOnPreparedListener { iMediaPlayer ->
            Log.i(TAG, "preparePlay: ==onPrepared")
            iMediaPlayer.start()
        }
    }

    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
        // 将背景设置为黑色
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        // 编译顶点和片元着色程序
        val vertexShaderId = ShaderUtils.compileVertexShader(vertexShaderStr)
        val fragmentShaderId = ShaderUtils.compileFragmentShader(fragmentShaderStr)
        Log.e(
            TAG,
            "onSurfaceCreated: compile result vertex = $vertexShaderId, fragment = $fragmentShaderId"
        )
        // 连接程序
        mProgram = ShaderUtils.linkProgram(vertexShaderId, fragmentShaderId)
        Log.e(TAG, "onSurfaceCreated: linkProgram result = $mProgram")
        // 在OpenGLES环境中使用程序
        GLES30.glUseProgram(mProgram)

        // 编译 glprogram 并获取控制句柄
        aPositionLocation = GLES30.glGetAttribLocation(mProgram, "aPosition")
        aTextureLocation = GLES30.glGetAttribLocation(mProgram, "aTexCoord")
        projectMatrixLocation = GLES20.glGetUniformLocation(mProgram, "uProjMatrix")
        rotateMatrixLocation = GLES20.glGetUniformLocation(mProgram, "uRotateMatrix")
        viewMatrixLocation = GLES20.glGetUniformLocation(mProgram, "uViewMatrix")
        modelMatrixLocation = GLES20.glGetUniformLocation(mProgram, "uModelMatrix")

        // 加载纹理
        textureId = loadTexture()
        Log.i(
            TAG,
            "onSurfaceCreated: $aPositionLocation, $aTextureLocation, textureId = $textureId"
        )
        // 播放器和纹理绑定
        attachTexture()
        // 播放
        preparePlay()
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        // 设置绘制窗口
        GLES30.glViewport(0, 0, width, height)
        screenWidth = width
        screenHeight = height
        // 模型矩阵
        Matrix.setIdentityM(mModelMatrix, 0)
        // 旋转矩阵
        Matrix.setIdentityM(mRotateMatrix, 0)
    }

    override fun onDrawFrame(gl: GL10) {
        // 把颜色缓冲区设置为我们预设的颜色
        GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT or GLES30.GL_COLOR_BUFFER_BIT)

        // 更新画面
        mSurfaceTexture?.updateTexImage()

        // 现实模式
        when (displayMode) {
            DISPLAY_NORMAL_MODE -> drawNormal()
            DISPLAY_GLASS_MODE -> drawGlass()
        }
    }

    private fun setSize(width: Int, height: Int) {
        // 计算宽高比
        val ratio = width.toFloat() / height
        // 透视投影矩阵/视锥
        Matrix.perspectiveM(mProjectMatrix, 0, 100f, ratio, 0f, 300f)
        // 设置相机位置
        Matrix.setLookAtM(
            mViewMatrix, 0,
            0f, 0.0f, 0.0f,
            0.0f, 0.0f, -1.0f,
            0.0f, 1.0f, 0.0f
        )
    }

    private fun drawNormal() {
        val width = screenWidth
        val height = screenHeight
        // 视口为全屏
        //GLES30.glViewport(0, height, width, height)
        GLES30.glViewport(0, 0, width, height) // (0, 0) 对应当前屏幕方向左下角坐标

        //setSize(screenWidth, screenHeight / 2)
        /*if (width < height) {
            val ratio = height * 1f / width
            Matrix.frustumM(mProjectMatrix, 0, -1f, 1f, -ratio, ratio, 1f, 1000f)
        } else {
            val ratio = width * 1f / height
            Matrix.perspectiveM(mProjectMatrix, 0, 70f, ratio, 1f, 1000f)
        }*/
        // 计算宽高比
        val ratio = width.toFloat() / height
        // 透视投影矩阵/视锥
        Matrix.perspectiveM(mProjectMatrix, 0, 100f, ratio, 0f, 300f)
        // 设置相机位置
        Matrix.setLookAtM(
            mViewMatrix, 0,
            0f, 0.0f, 0.0f,
            0.0f, 0.0f, -1.0f,
            0.0f, 1.0f, 0.0f
        )

        // 将变换矩阵传入顶点渲染器
        GLES20.glUniformMatrix4fv(projectMatrixLocation, 1, false, mProjectMatrix, 0)
        GLES20.glUniformMatrix4fv(rotateMatrixLocation, 1, false, mRotateMatrix, 0)
        GLES20.glUniformMatrix4fv(viewMatrixLocation, 1, false, mViewMatrix, 0)
        GLES20.glUniformMatrix4fv(modelMatrixLocation, 1, false, mModelMatrix, 0)

        // 激活纹理
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)

        // 启用顶点坐标属性
        GLES30.glEnableVertexAttribArray(aPositionLocation)
        GLES30.glVertexAttribPointer(
            aPositionLocation,
            COORDS_PER_VERTEX,
            GLES30.GL_FLOAT,
            false,
            0,
            vertexBuffer
        )

        // 启用纹理坐标属性
        GLES30.glEnableVertexAttribArray(aTextureLocation)
        GLES30.glVertexAttribPointer(
            aTextureLocation,
            COORDS_PER_TEXTURE,
            GLES30.GL_FLOAT,
            false,
            0,
            mTexVertexBuffer
        )

        // 绘制
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vCount)

        // 禁止顶点数组的句柄
        GLES30.glDisableVertexAttribArray(aPositionLocation)
        GLES30.glDisableVertexAttribArray(aTextureLocation)
    }

    private fun drawGlass() {
        drawLeftEye()
        drawRightEye()
    }

    private fun drawLeftEye() {
        GLES30.glViewport(0, 0, screenWidth / 2, screenHeight)
        setSize(screenWidth / 2, screenHeight)

        // 将变换矩阵传入顶点渲染器
        GLES20.glUniformMatrix4fv(projectMatrixLocation, 1, false, mProjectMatrix, 0)
        GLES20.glUniformMatrix4fv(rotateMatrixLocation, 1, false, mRotateMatrix, 0)
        GLES20.glUniformMatrix4fv(viewMatrixLocation, 1, false, mViewMatrix, 0)
        GLES20.glUniformMatrix4fv(modelMatrixLocation, 1, false, mModelMatrix, 0)

        // 激活纹理
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)

        // 启用顶点坐标属性
        GLES30.glEnableVertexAttribArray(aPositionLocation)
        GLES30.glVertexAttribPointer(
            aPositionLocation,
            COORDS_PER_VERTEX,
            GLES30.GL_FLOAT,
            false,
            0,
            vertexBuffer
        )

        // 启用纹理坐标属性
        GLES30.glEnableVertexAttribArray(aTextureLocation)
        GLES30.glVertexAttribPointer(
            aTextureLocation,
            COORDS_PER_TEXTURE,
            GLES30.GL_FLOAT,
            false,
            0,
            mTexVertexBuffer
        )

        // 绘制
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vCount)

        // 禁止顶点数组的句柄
        GLES30.glDisableVertexAttribArray(aPositionLocation)
        GLES30.glDisableVertexAttribArray(aTextureLocation)
    }

    private fun drawRightEye() {
        GLES30.glViewport(screenWidth / 2, 0, screenWidth / 2, screenHeight)
        setSize(screenWidth / 2, screenHeight)

        // 将变换矩阵传入顶点渲染器
        GLES20.glUniformMatrix4fv(projectMatrixLocation, 1, false, mProjectMatrix, 0)
        GLES20.glUniformMatrix4fv(rotateMatrixLocation, 1, false, mRotateMatrix, 0)
        GLES20.glUniformMatrix4fv(viewMatrixLocation, 1, false, mViewMatrix, 0)
        GLES20.glUniformMatrix4fv(modelMatrixLocation, 1, false, mModelMatrix, 0)

        // 激活纹理
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)

        // 启用顶点坐标属性
        GLES30.glEnableVertexAttribArray(aPositionLocation)
        GLES30.glVertexAttribPointer(
            aPositionLocation,
            COORDS_PER_VERTEX,
            GLES30.GL_FLOAT,
            false,
            0,
            vertexBuffer
        )

        // 启用纹理坐标属性
        GLES30.glEnableVertexAttribArray(aTextureLocation)
        GLES30.glVertexAttribPointer(
            aTextureLocation,
            COORDS_PER_TEXTURE,
            GLES30.GL_FLOAT,
            false,
            0,
            mTexVertexBuffer
        )

        // 绘制
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vCount)

        // 禁止顶点数组的句柄
        GLES30.glDisableVertexAttribArray(aPositionLocation)
        GLES30.glDisableVertexAttribArray(aTextureLocation)
    }
}