package com.dev.gles3.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.*
import android.util.Log
import com.dev.gles3.R
import com.dev.gles3.utils.LoadFileUtils
import com.dev.gles3.utils.ShaderUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * description : 圆柱
 *
 * @author     : hudongxin
 * @date       : 8/17/21
 */
class EarthMapRotateRenderer constructor(context: Context) : GLSurfaceView.Renderer {
    private val mContext = context

    companion object {
        private const val TAG = "EarthMapRenderer"

        // 一个Float占用4Byte
        private const val BYTES_PER_FLOAT = 4
    }

    // 顶点位置缓存、纹理顶点位置缓存、渲染程序
    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var mTexVertexBuffer: FloatBuffer
    private var mProgram = 0

    private var mBitmap: Bitmap? = null // 图片生成的位图
    private var textureId = -1 // 纹理id
    private var vCount = 0 // 向量个数

    // 相关属性id
    private var mHProjMatrix = -1
    private var mHRotateMatrix = -1
    private var mHViewMatrix = -1
    private var mHModelMatrix = -1
    private var mHPosition = -1
    private var mHCoordinate = -1

    // 相机矩阵、投影矩阵、根据传感器变化的矩阵
    private var mViewMatrix = FloatArray(16)
    private var mProjectMatrix = FloatArray(16)
    private var uRotateMatrix = FloatArray(16)
    private var mModelMatrix = FloatArray(16)

    // 返回属性变量的位置: 变换矩阵、位置、颜色
    private var uMatrixLocation = 0
    private var aPositionLocation = 0

    // 顶点着色器和片段着色器
    private var vertexShaderStr = LoadFileUtils.readResource(context, R.raw.vertex_earth_shade)
    private var fragmentShaderStr = LoadFileUtils.readResource(context, R.raw.fragment_earth_shade)

    init {
        calculateAttribute()
    }

    fun setRotateMatrix(uRotateMatrix: FloatArray) {
        this.uRotateMatrix = uRotateMatrix
    }

    // 计算顶点坐标和纹理坐标
    private fun calculateAttribute() {
        val radius = 1.0f // 球的半径
        val angleSpan = Math.PI / 90f // 将球进行单位切分的角度
        val alVertix: ArrayList<Float> = ArrayList()
        val textureVertix: ArrayList<Float> = ArrayList()
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
                val x2 = (radius * Math.sin(vAngle + angleSpan) * Math.cos(hAngle + angleSpan)).toFloat()
                val y2 = (radius * Math.sin(vAngle + angleSpan) * Math.sin(hAngle + angleSpan)).toFloat()
                val z2 = (radius * Math.cos(vAngle + angleSpan)).toFloat()
                val x3 = (radius * Math.sin(vAngle + angleSpan) * Math.cos(hAngle)).toFloat()
                val y3 = (radius * Math.sin(vAngle + angleSpan) * Math.sin(hAngle)).toFloat()
                val z3 = (radius * Math.cos(vAngle + angleSpan)).toFloat()
                val s0 = (-hAngle / Math.PI / 2).toFloat()
                val s1 = (-(hAngle + angleSpan) / Math.PI / 2).toFloat()
                val t0 = (vAngle / Math.PI).toFloat()
                val t1 = ((vAngle + angleSpan) / Math.PI).toFloat()
                alVertix.add(x1)
                alVertix.add(y1)
                alVertix.add(z1)
                alVertix.add(x0)
                alVertix.add(y0)
                alVertix.add(z0)
                alVertix.add(x3)
                alVertix.add(y3)
                alVertix.add(z3)
                textureVertix.add(s1) // x1 y1对应纹理坐标
                textureVertix.add(t0)
                textureVertix.add(s0) // x0 y0对应纹理坐标
                textureVertix.add(t0)
                textureVertix.add(s0) // x3 y3对应纹理坐标
                textureVertix.add(t1)
                alVertix.add(x1)
                alVertix.add(y1)
                alVertix.add(z1)
                alVertix.add(x3)
                alVertix.add(y3)
                alVertix.add(z3)
                alVertix.add(x2)
                alVertix.add(y2)
                alVertix.add(z2)
                textureVertix.add(s1) // x1 y1对应纹理坐标
                textureVertix.add(t0)
                textureVertix.add(s0) // x3 y3对应纹理坐标
                textureVertix.add(t1)
                textureVertix.add(s1) // x2 y3对应纹理坐标
                textureVertix.add(t1)
                hAngle += angleSpan
            }
            vAngle += angleSpan
        }
        vCount = alVertix.size / 3
        vertexBuffer = convertToFloatBuffer(alVertix)
        mTexVertexBuffer = convertToFloatBuffer(textureVertix)
    }

    // 动态数组转FloatBuffer
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

    private fun loadTexture(context: Context, resourceId: Int): Int {
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
        mBitmap = BitmapFactory.decodeResource(context.resources, resourceId, options)
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
        mBitmap?.recycle()

        // 取消绑定纹理
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
        return textureIds[0]
    }

    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
        // 将背景设置为灰色
        GLES30.glClearColor(0.5f, 0.5f, 0.5f, 1.0f)
        // 编译顶点和片元着色程序
        val vertexShaderId = ShaderUtils.compileVertexShader(vertexShaderStr)
        val fragmentShaderId = ShaderUtils.compileFragmentShader(fragmentShaderStr)
        Log.e(TAG, "onSurfaceCreated: compile result vertex = $vertexShaderId, fragment = $fragmentShaderId")
        // 连接程序
        mProgram = ShaderUtils.linkProgram(vertexShaderId, fragmentShaderId)
        Log.e(TAG, "onSurfaceCreated: linkProgram result = $mProgram")
        // 在OpenGLES环境中使用程序
        GLES30.glUseProgram(mProgram)

        // 编译glprogram并获取控制句柄
        mHProjMatrix = GLES20.glGetUniformLocation(mProgram, "uProjMatrix")
        mHRotateMatrix = GLES30.glGetUniformLocation(mProgram, "uRotateMatrix")
        mHViewMatrix = GLES20.glGetUniformLocation(mProgram, "uViewMatrix")
        mHModelMatrix = GLES20.glGetUniformLocation(mProgram, "uModelMatrix")
        mHPosition = GLES20.glGetAttribLocation(mProgram, "aPosition")
        mHCoordinate = GLES20.glGetAttribLocation(mProgram, "aCoordinate")

        // 加载纹理
        textureId = loadTexture(mContext, R.drawable.guangzhou)
        Log.i(TAG, "onSurfaceCreated: $uMatrixLocation, $aPositionLocation")
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        // 设置绘制窗口
        GLES30.glViewport(0, 0, width, height)

        // 计算宽高比
        val ratio = width.toFloat() / height
        // 透视投影矩阵/视锥
        //Matrix.perspectiveM(mProjectMatrix, 0, 60f, ratio, 1f, 300f)
        // 设置相机位置
        //Matrix.setLookAtM(mViewMatrix, 0, 0f, 4f, 2f, 0.0f, 0.0f, 0f, 0f, 0f, 1f)

        // 查看球的内表面
        Matrix.perspectiveM(mProjectMatrix, 0, 90f, ratio, 0f, 300f)
        Matrix.setLookAtM(mViewMatrix, 0, 0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0f, 1.0f, 0.0f)

        // 模型矩阵
        Matrix.setIdentityM(mModelMatrix, 0)

        Matrix.setIdentityM(uRotateMatrix, 0)
    }

    override fun onDrawFrame(unused: GL10) {
        // 把颜色缓冲区设置为我们预设的颜色
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

        GLES30.glUniformMatrix4fv(mHProjMatrix, 1, false, mProjectMatrix, 0)
        GLES30.glUniformMatrix4fv(mHRotateMatrix, 1, false, uRotateMatrix, 0)
        GLES30.glUniformMatrix4fv(mHViewMatrix, 1, false, mViewMatrix, 0)
        GLES30.glUniformMatrix4fv(mHModelMatrix, 1, false, mModelMatrix, 0)

        GLES30.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES30.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

        GLES30.glEnableVertexAttribArray(mHPosition)
        GLES30.glVertexAttribPointer(mHPosition, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES30.glEnableVertexAttribArray(mHCoordinate)
        GLES30.glVertexAttribPointer(mHCoordinate, 2, GLES20.GL_FLOAT, false, 0, mTexVertexBuffer)

        GLES30.glDrawArrays(GLES20.GL_TRIANGLES, 0, vCount)

        GLES30.glDisableVertexAttribArray(mHCoordinate)
        GLES30.glDisableVertexAttribArray(mHPosition)
    }
}