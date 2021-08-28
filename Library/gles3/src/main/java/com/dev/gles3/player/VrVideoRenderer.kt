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
import kotlin.math.cos
import kotlin.math.sin

/**
 * description : VR 视频播放
 *
 * @author     : hudongxin
 * @date       : 8/17/21
 */
class VrVideoRenderer constructor(context: Context, glView: GLSurfaceView, playUrl: String) :
    GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "VideoRenderer"
        const val DISPLAY_NORMAL_MODE = 1
        const val DISPLAY_GLASS_MODE = 2

        // 顶点向量元素个数（x,y,z）
        private const val COORDS_PER_VERTEX = 3

        // 纹理向量元素个数（s,t）
        private const val COORDS_PER_TEXTURE = 2

        // 绘制球体时，每次增加的角度
        private const val CAP = 9
    }

    private var videoPath = playUrl
    private val glSurfaceView = glView
    private val mContext = context

    private var verticals = FloatArray((180 / CAP) * (360 / CAP) * 6 * 3)
    private var uvTexVertex = FloatArray((180 / CAP) * (360 / CAP) * 6 * 2)

    // 顶点位置缓存、纹理顶点位置缓存、渲染程序
    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var mTexVertexBuffer: FloatBuffer

    private var mProgram = 0
    private var mPositionLocation = -1 // 顶点
    private var mTexCoordLocation = -1 // 纹理
    private var mMatrixLocation = -1
    private var mTexSamplerLocation = -1
    private var rotateMatrixLocation = -1 // 旋转矩阵

    private var mViewMatrix = FloatArray(16)
    private var mProjectMatrix = FloatArray(16)
    private var mMVPMatrix = FloatArray(16)
    private var mRotateMatrix = FloatArray(16)

    private var mModelMatrix = FloatArray(16)

    private var textureId = -1 // 纹理id
    private var mSurfaceTexture: SurfaceTexture? = null // 图片生成的位图
    private var isAvailable = false
    private var mSurface: Surface? = null
    private var r = 6f  // 球体半径

    //private var vCount = (180 / CAP) * (360 / CAP) * 6 // 向量个数
    private var vCount = 0 // 向量个数

    // IjkMediaPlayer实例
    private lateinit var ijkMediaPlayer: IjkMediaPlayer

    // 展示模式：分为 全景；VR眼睛
    private var displayMode = DISPLAY_NORMAL_MODE

    // 交互模式：是否响应传感器
    private var interactionModeNormal = false

    private var screenWidth = 0
    private var screenHeight = 0

    private var mXAngle = 0f
    private var mYAngle = 0f
    private var mZAngle = 1f

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
        if (interactionModeNormal) {
            System.arraycopy(rotateMatrix, 0, mRotateMatrix, 0, 16)
        }
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

    init {
        calculateAttribute()
        initMediaPlayer()
    }

    /**
     * 计算顶点位置和纹理位置数据
     */
    private fun calculateAttribute() {
        val x = 0f
        val y = 0f
        val z = 0f

        var index = 0
        var index1 = 0
        val d = CAP * Math.PI / 180 // 每次递增的弧度

        for (i in 0 until 180 step CAP) {
            Log.i(TAG, "calculateAttribute: loop indexI = $i")
            val d1 = i * Math.PI / 180
            for (j in 0 until 360 step CAP) {
                Log.i(TAG, "calculateAttribute: loop indexJ = $j")
                // 获得球体上切分的超小片矩形的顶点坐标（两个三角形组成，所以有六点顶点）
                val d2 = j * Math.PI / 180
                verticals[index++] = (x + r * sin(d1 + d) * cos(d2 + d)).toFloat()
                verticals[index++] = (y + r * cos(d1 + d)).toFloat()
                verticals[index++] = (z + r * sin(d1 + d) * sin(d2 + d)).toFloat()
                // 获得球体上切分的超小片三角形的纹理坐标
                uvTexVertex[index1++] = (j + CAP) * 1f / 360
                uvTexVertex[index1++] = (i + CAP) * 1f / 180

                verticals[index++] = (x + r * sin(d1) * cos(d2)).toFloat()
                verticals[index++] = (y + r * cos(d1)).toFloat()
                verticals[index++] = (z + r * sin(d1) * sin(d2)).toFloat()

                uvTexVertex[index1++] = j * 1f / 360
                uvTexVertex[index1++] = i * 1f / 180

                verticals[index++] = (x + r * sin(d1) * cos(d2 + d)).toFloat()
                verticals[index++] = (y + r * cos(d1)).toFloat()
                verticals[index++] = (z + r * sin(d1) * sin(d2 + d)).toFloat()

                uvTexVertex[index1++] = (j + CAP) * 1f / 360
                uvTexVertex[index1++] = i * 1f / 180

                verticals[index++] = (x + r * sin(d1 + d) * cos(d2 + d)).toFloat()
                verticals[index++] = (y + r * cos(d1 + d)).toFloat()
                verticals[index++] = (z + r * sin(d1 + d) * sin(d2 + d)).toFloat()

                uvTexVertex[index1++] = (j + CAP) * 1f / 360
                uvTexVertex[index1++] = (i + CAP) * 1f / 180

                verticals[index++] = (x + r * sin(d1 + d) * cos(d2)).toFloat()
                verticals[index++] = (y + r * cos(d1 + d)).toFloat()
                verticals[index++] = (z + r * sin(d1 + d) * sin(d2)).toFloat()

                uvTexVertex[index1++] = j * 1f / 360
                uvTexVertex[index1++] = (i + CAP) * 1f / 180

                verticals[index++] = (x + r * sin(d1) * cos(d2)).toFloat()
                verticals[index++] = (y + r * cos(d1)).toFloat()
                verticals[index++] = (z + r * sin(d1) * sin(d2)).toFloat()

                uvTexVertex[index1++] = j * 1f / 360
                uvTexVertex[index1++] = i * 1f / 180
            }
        }

        vertexBuffer = ByteBuffer.allocateDirect(verticals.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(verticals)
        vertexBuffer.position(0)
        vCount = verticals.size / 3

        mTexVertexBuffer = ByteBuffer.allocateDirect(uvTexVertex.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(uvTexVertex)
        mTexVertexBuffer.position(0)
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
        if (mSurface != null) {
            return
        }
        Log.i(TAG, "attachTexture: textureId  = $textureId")
        mSurfaceTexture = SurfaceTexture(textureId).apply {
            //setDefaultBufferSize(100, 100)
            setOnFrameAvailableListener {
                //Log.i(TAG, "attachTexture: =======")
                isAvailable = true
                glSurfaceView.requestRender()
            }
            mSurface = Surface(this)
            ijkMediaPlayer.setSurface(mSurface)
            mSurface?.release()
        }
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
        // 顶点着色器和片段着色器
        val vertexShaderStr = LoadFileUtils.readResource(mContext, R.raw.vertex_video_shader)
        val vertexShaderId = ShaderUtils.compileVertexShader(vertexShaderStr)
        val fragmentShaderStr = LoadFileUtils.readResource(mContext, R.raw.fragment_video_shader)
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
        mPositionLocation = GLES30.glGetAttribLocation(mProgram, "vPosition")
        mTexCoordLocation = GLES30.glGetAttribLocation(mProgram, "a_texCoord")
        mMatrixLocation = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")
        mTexSamplerLocation = GLES20.glGetUniformLocation(mProgram, "s_texture")
        rotateMatrixLocation = GLES20.glGetUniformLocation(mProgram, "uRotateMatrix")
        Log.i(
            TAG, "onSurfaceCreated: \n" +
                    "mPositionLocation = $mPositionLocation, \n" +
                    "mTexCoordLocation = $mTexCoordLocation, \n" +
                    "mMatrixLocation = $mMatrixLocation, \n" +
                    "mTexSamplerLocation = $mTexSamplerLocation \n" +
                    "rotateMatrixLocation = $rotateMatrixLocation, \n"
        )
        // 加载纹理
        textureId = loadTexture()
        Log.i(TAG, "onSurfaceCreated: textureId = $textureId")
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
        if (isAvailable) {
            mSurfaceTexture?.updateTexImage()
            isAvailable = false
        }
        val width = screenWidth
        val height = screenHeight
        //GLES30.glViewport(0, height / 2, width, height / 2) // 半屏
        GLES30.glViewport(0, 0, width, height) // (0, 0) 对应当前屏幕方向左下角坐标
        // set
        if (width < height) {
            val ratio = height * 1f / width
            Matrix.frustumM(mProjectMatrix, 0, -1f, 1f, -ratio, ratio, 1f, 300f)
        } else {
            val ratio = width * 1f / height
            Matrix.perspectiveM(mProjectMatrix, 0, 70f, ratio, 1f, 300f)
        }

        Matrix.setLookAtM(
            mViewMatrix, 0,
            0f, 0.0f, 0.0f,
            mXAngle, mYAngle, -mZAngle,
            0.0f, 1.0f, 0.0f
        )
        // 是否支持陀螺仪
        if (interactionModeNormal) {
            Matrix.multiplyMM(mModelMatrix, 0, mProjectMatrix, 0, mRotateMatrix, 0)
            Matrix.multiplyMM(mMVPMatrix, 0, mModelMatrix, 0, mViewMatrix, 0)
        } else {
            /*Log.i(TAG, "onDrawFrame: mXAngle = $mXAngle, mYAngle = $mYAngle, mZAngle = $mZAngle")
            Matrix.rotateM(mProjectMatrix, 0, -mXAngle, 1f, 0f, 0f)
            Matrix.rotateM(mProjectMatrix, 0, -mYAngle, 0f, 1f, 0f)
            Matrix.rotateM(mProjectMatrix, 0, -mZAngle, 0f, 0f, 1f)*/
            Matrix.multiplyMM(mMVPMatrix, 0, mProjectMatrix, 0, mViewMatrix, 0)
        }
        // 激活纹理
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)

        // 启用顶点坐标属性
        GLES30.glEnableVertexAttribArray(mPositionLocation)
        GLES30.glVertexAttribPointer(
            mPositionLocation,
            COORDS_PER_VERTEX,
            GLES30.GL_FLOAT,
            false,
            12,
            vertexBuffer
        )

        // 启用纹理坐标属性
        GLES30.glEnableVertexAttribArray(mTexCoordLocation)
        GLES30.glVertexAttribPointer(
            mTexCoordLocation,
            COORDS_PER_TEXTURE,
            GLES30.GL_FLOAT,
            false,
            0,
            mTexVertexBuffer
        )

        // 旋转矩阵
        GLES20.glUniformMatrix4fv(rotateMatrixLocation, 1, false, mRotateMatrix, 0)

        // 将变换矩阵传入顶点渲染器
        GLES20.glUniformMatrix4fv(mMatrixLocation, 1, false, mMVPMatrix, 0)
        GLES20.glUniform1i(mTexSamplerLocation, 0)

        // 绘制
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vCount)

        // 禁止顶点数组的句柄
        GLES30.glDisableVertexAttribArray(mPositionLocation)
        GLES30.glDisableVertexAttribArray(mTexCoordLocation)
    }
}