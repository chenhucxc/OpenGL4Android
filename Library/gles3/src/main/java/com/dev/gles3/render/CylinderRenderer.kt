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
class CylinderRenderer constructor(context: Context) : GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "CylinderRenderer"

        // 一个Float占用4Byte
        private const val BYTES_PER_FLOAT = 4

        // 三个顶点
        private const val POSITION_COMPONENT_COUNT = 3
        private const val SEPARATE_COUNT = 120
        private const val RADIUS = 0.5f
        private const val HEIGHT = 1.0f
    }

    // 顶点位置缓存(圆柱侧面)、顶点颜色缓存(圆柱顶面)、顶点颜色缓存(圆柱底面)、渲染程序
    private var vertexBuffer: FloatBuffer
    private var vertexBufferTop: FloatBuffer
    private var vertexBufferBtm: FloatBuffer
    private var mProgram = 0

    // 相机矩阵、投影矩阵、最终变换矩阵
    private var mViewMatrix = FloatArray(16)
    private var mProjectMatrix = FloatArray(16)
    private var mMVPMatrix = FloatArray(16)

    // 返回属性变量的位置: 变换矩阵、位置、颜色
    private var uMatrixLocation = 0
    private var aPositionLocation = 0

    private lateinit var cylinderCoords: FloatArray // 圆形顶点位置（侧面）
    private lateinit var cylinderCoordsTop: FloatArray // 圆锥顶点位置（圆顶面）
    private lateinit var cylinderCoordsBtm: FloatArray // 圆锥顶点位置（圆底面）

    // 顶点着色器和片段着色器
    private var vertexShaderStr = LoadFileUtils.readResource(context, R.raw.vertex_cone_renderer)
    private var fragmentShaderStr = LoadFileUtils.readResource(context, R.raw.fragment_cone_renderer)

    init {
        // 侧面数据
        createCylinderPositions()

        // 底面数据
        createCircularPositions()

        // 顶点位置相关（圆锥侧面）
        // 分配本地内存空间,每个浮点型占4字节空间；将坐标数据转换为FloatBuffer，用以传入给OpenGL ES程序
        vertexBuffer = ByteBuffer.allocateDirect(cylinderCoords.size * BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(cylinderCoords)
        vertexBuffer.position(0)

        // 顶点颜色相关
        vertexBufferTop = ByteBuffer.allocateDirect(cylinderCoordsTop.size * BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(cylinderCoordsTop)
        vertexBufferTop.position(0)

        // 顶点位置相关（圆锥底面）
        vertexBufferBtm = ByteBuffer.allocateDirect(cylinderCoordsBtm.size * BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(cylinderCoordsBtm)
        vertexBufferBtm.position(0)
    }

    private fun createCircularPositions() {
        val data: ArrayList<Float> = ArrayList()
        data.add(0.0f)
        data.add(0.0f)
        data.add(HEIGHT)
        val data1: ArrayList<Float> = ArrayList()
        data1.add(0.0f)
        data1.add(0.0f)
        data1.add(0.0f)
        val angDegSpan = 360f / SEPARATE_COUNT
        run {
            var i = 0f
            while (i < 360 + angDegSpan) {
                data.add((RADIUS * Math.sin(i * Math.PI / 180f)).toFloat())
                data.add((RADIUS * Math.cos(i * Math.PI / 180f)).toFloat())
                data.add(HEIGHT)
                data1.add((RADIUS * Math.sin(i * Math.PI / 180f)).toFloat())
                data1.add((RADIUS * Math.cos(i * Math.PI / 180f)).toFloat())
                data1.add(0.0f)
                i += angDegSpan
            }
        }
        val f = FloatArray(data.size)
        val f1 = FloatArray(data.size)
        for (i in f.indices) {
            f[i] = data[i]
            f1[i] = data1[i]
        }
        cylinderCoordsTop = f
        cylinderCoordsBtm = f1
    }

    private fun createCylinderPositions() {
        val pos: ArrayList<Float> = ArrayList()
        val angDegSpan = 360f / SEPARATE_COUNT
        run {
            var i = 0f
            while (i < 360 + angDegSpan) {
                pos.add((RADIUS * Math.sin(i * Math.PI / 180f)).toFloat())
                pos.add((RADIUS * Math.cos(i * Math.PI / 180f)).toFloat())
                pos.add(HEIGHT)
                pos.add((RADIUS * Math.sin(i * Math.PI / 180f)).toFloat())
                pos.add((RADIUS * Math.cos(i * Math.PI / 180f)).toFloat())
                pos.add(0.0f)
                i += angDegSpan
            }
        }
        val d = FloatArray(pos.size)
        for (i in d.indices) {
            d[i] = pos[i]
        }
        cylinderCoords = d
    }

    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
        // 将背景设置为白色
        GLES30.glClearColor(0.3f, 0.3f, 0.3f, 1.0f)
        // 编译顶点着色程序
        val vertexShaderId = ShaderUtils.compileVertexShader(vertexShaderStr)
        // 编译片段着色程序
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
        // 绘制圆柱侧面
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, cylinderCoords.size / 3)

        // 准备坐标数据
        GLES30.glVertexAttribPointer(aPositionLocation, 3, GLES30.GL_FLOAT, false, 0, vertexBufferTop)
        // 启用顶点位置句柄
        GLES30.glEnableVertexAttribArray(aPositionLocation)
        // 绘制圆柱侧面
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, cylinderCoordsTop.size / 3)

        //准备坐标数据
        GLES30.glVertexAttribPointer(aPositionLocation, 3, GLES30.GL_FLOAT, false, 0, vertexBufferBtm)
        //启用顶点位置句柄
        GLES30.glEnableVertexAttribArray(aPositionLocation)
        //绘制圆柱侧面
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, cylinderCoordsBtm.size/3)

        // 禁止顶点数组的句柄
        GLES30.glDisableVertexAttribArray(aPositionLocation)
    }
}