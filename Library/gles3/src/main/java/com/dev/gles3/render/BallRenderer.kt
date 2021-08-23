package com.dev.gles3.render

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
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
class BallRenderer constructor(context: Context) : GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "CylinderRenderer"

        // 一个Float占用4Byte
        private const val BYTES_PER_FLOAT = 4
        private const val SEPARATE_COUNT = 120
        private const val RADIUS = 0.5f
        private const val HEIGHT = 1.0f
    }

    // 顶点位置缓存、渲染程序
    private var vertexBuffer: FloatBuffer
    private var mProgram = 0

    // 相机矩阵、投影矩阵、最终变换矩阵
    private var mViewMatrix = FloatArray(16)
    private var mProjectMatrix = FloatArray(16)
    private var mMVPMatrix = FloatArray(16)

    // 返回属性变量的位置: 变换矩阵、位置、颜色
    private var uMatrixLocation = 0
    private var aPositionLocation = 0

    private lateinit var ballCoords: FloatArray

    // 顶点着色器和片段着色器
    private var vertexShaderStr = LoadFileUtils.readResource(context, R.raw.vertex_ball_shade)
    private var fragmentShaderStr = LoadFileUtils.readResource(context, R.raw.fragment_ball_shade)

    init {
        createVertexPos()

        // 顶点位置相关（圆锥侧面）
        // 分配本地内存空间,每个浮点型占4字节空间；将坐标数据转换为FloatBuffer，用以传入给OpenGL ES程序
        vertexBuffer = ByteBuffer.allocateDirect(ballCoords.size * BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(ballCoords)
        vertexBuffer.position(0)
    }

    private fun createVertexPos() {
        val radius = 1.0f // 球的半径
        val angleSpan = Math.PI / 90f // 将球进行单位切分的角度
        val alVertix: ArrayList<Float> = ArrayList()
        var vAngle = 0.0
        while (vAngle < Math.PI) {
            var hAngle = 0.0
            while (hAngle < 2 * Math.PI) {
                // 获取一个四边形的四个顶点
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

                // 将四个点拆分为两个三角形
                alVertix.add(x1)
                alVertix.add(y1)
                alVertix.add(z1)
                alVertix.add(x0)
                alVertix.add(y0)
                alVertix.add(z0)
                alVertix.add(x3)
                alVertix.add(y3)
                alVertix.add(z3)
                alVertix.add(x1)
                alVertix.add(y1)
                alVertix.add(z1)
                alVertix.add(x3)
                alVertix.add(y3)
                alVertix.add(z3)
                alVertix.add(x2)
                alVertix.add(y2)
                alVertix.add(z2)
                hAngle += angleSpan
            }
            vAngle += angleSpan
        }
        val size: Int = alVertix.size
        ballCoords = FloatArray(size)
        for (i in 0 until size) {
            ballCoords[i] = alVertix[i]
        }
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

        uMatrixLocation = GLES30.glGetUniformLocation(mProgram, "u_Matrix")
        aPositionLocation = GLES30.glGetAttribLocation(mProgram, "vPosition")
        Log.i(TAG, "onSurfaceCreated: $uMatrixLocation, $aPositionLocation")
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        // 设置绘制窗口
        GLES30.glViewport(0, 0, width, height)

        // 计算宽高比
        val ratio = width.toFloat() / height
        // 设置透视投影
        Matrix.frustumM(mProjectMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)
        // 设置相机位置
        Matrix.setLookAtM(mViewMatrix, 0, 6f, 0f, -1f, 0f, 0f, 0f, 0f, 0.0f, 1.0f)
        // 计算变换矩阵
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectMatrix, 0, mViewMatrix, 0)
    }

    /**
     * GLES30.glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride, Buffer ptr)
     * index：顶点属性的索引.（这里我们的顶点位置和颜色向量在着色器中分别为0和1）
     *          layout (location = 0) in vec4 vPosition;
     *          layout (location = 1) in vec4 aColor;
     * size: 指定每个通用顶点属性的元素个数。必须是1、2、3、4。此外，glvertexattribpointer接受符号常量gl_bgra。初始值为4（也就是涉及颜色的时候必为4）。
     * type：属性的元素类型。（上面都是Float所以使用GLES30.GL_FLOAT）；
     * normalized：转换的时候是否要经过规范化，true：是；false：直接转化；
     * stride：跨距，默认是0。（由于我们将顶点位置和颜色数据分别存放没写在一个数组中，所以使用默认值0）
     * ptr： 本地数据缓存（这里我们的是顶点的位置和颜色数据）。
     */
    override fun onDrawFrame(unused: GL10) {
        // 把颜色缓冲区设置为我们预设的颜色
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

        // 将变换矩阵传入顶点渲染器
        GLES20.glUniformMatrix4fv(uMatrixLocation, 1, false, mMVPMatrix, 0)

        // 准备坐标数据
        GLES30.glVertexAttribPointer(aPositionLocation, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer)
        // 启用顶点位置句柄
        GLES30.glEnableVertexAttribArray(aPositionLocation)
        // 绘制球
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, ballCoords.size / 3)

        // 禁止顶点数组的句柄
        GLES30.glDisableVertexAttribArray(aPositionLocation)
    }
}