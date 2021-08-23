package com.dev.gles3.example

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.dev.gles3.R
import com.dev.gles3.render.BallRenderer
import com.dev.gles3.render.CylinderRenderer
import com.dev.gles3.utils.LoadFileUtils
import com.dev.gles3.utils.ShaderUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


/**
 * description : OpenGL 3.0 基础示例，https://blog.csdn.net/gongxiaoou/article/details/89295973
 *
 * @author     : hudongxin
 * @date       : 8/14/21
 */
class Basic05Activity : AppCompatActivity() {
    companion object {
        private const val TAG = "Basic03Activity"
    }

    private lateinit var glSurfaceView: GLSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.basic_activity_common)
        initGlView(2)
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()
    }

    override fun onPause() {
        super.onPause()
        glSurfaceView.onPause()
    }

    private fun initGlView(type: Int) {
        glSurfaceView = findViewById(R.id.glView)
        glSurfaceView.setEGLContextClientVersion(3)
        val mRender = when (type) {
            0 -> ConeRenderer(this)
            1 -> CylinderRenderer(this)
            2 -> BallRenderer(this)
            else -> ConeRenderer(this)
        }
        glSurfaceView.setRenderer(mRender)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
    }

    // 索引法：根据索引序列，在顶点序列中找到对应的顶点，并根据绘制的方式，组成相应的图元绘制
    private class ConeRenderer constructor(context: Context) : GLSurfaceView.Renderer {

        companion object {
            // 一个Float占用4Byte
            private const val BYTES_PER_FLOAT = 4

            // 三个顶点
            private const val POSITION_COMPONENT_COUNT = 3
        }

        // 顶点位置缓存、顶点颜色缓存、渲染程序
        private var vertexBuffer: FloatBuffer
        private var vertexBufferBottom: FloatBuffer
        private var colorBuffer: FloatBuffer
        private var mProgram = 0

        // 相机矩阵、投影矩阵、最终变换矩阵
        private var mViewMatrix = FloatArray(16)
        private var mProjectMatrix = FloatArray(16)
        private var mMVPMatrix = FloatArray(16)

        // 返回属性变量的位置: 变换矩阵、位置、颜色
        private var uMatrixLocation = 0
        private var aPositionLocation = 0
        private var aColorLocation = 0

        private lateinit var circularCoords: FloatArray // 圆形顶点位置（侧面）
        private lateinit var coneCoordsBottom: FloatArray // 圆锥顶点位置（圆底面）
        private lateinit var color: FloatArray // 顶点的颜色

        // 顶点着色器和片段着色器
        private var vertexShaderStr = LoadFileUtils.readResource(context, R.raw.vertex_cone_renderer)
        private var fragmentShaderStr = LoadFileUtils.readResource(context, R.raw.fragment_cone_renderer)

        init {
            // 圆锥的侧面数据
            createPositions(0.5f, 60)
            // 圆锥的圆形底面数据
            createCircularPositions()

            // 顶点位置相关（圆锥侧面）
            // 分配本地内存空间,每个浮点型占4字节空间；将坐标数据转换为FloatBuffer，用以传入给OpenGL ES程序
            vertexBuffer = ByteBuffer.allocateDirect(circularCoords.size * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(circularCoords)
            vertexBuffer.position(0)

            // 顶点颜色相关
            colorBuffer = ByteBuffer.allocateDirect(color.size * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(color)
            colorBuffer.position(0)

            // 顶点位置相关（圆锥底面）
            vertexBufferBottom = ByteBuffer.allocateDirect(coneCoordsBottom.size * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(coneCoordsBottom)
            vertexBufferBottom.position(0)
        }

        private fun createPositions(radius: Float, n: Int) {
            val data: ArrayList<Float> = ArrayList()
            data.add(0.0f) //设置圆锥顶点坐标
            data.add(0.0f)
            data.add(-0.5f)
            val angDegSpan = 360f / n
            run {
                var i = 0f
                while (i < 360 + angDegSpan) {
                    data.add((radius * Math.sin(i * Math.PI / 180f)).toFloat())
                    data.add((radius * Math.cos(i * Math.PI / 180f)).toFloat())
                    data.add(0.0f)
                    i += angDegSpan
                }
            }
            val f = FloatArray(data.size)
            for (i in f.indices) {
                f[i] = data[i]
            }
            circularCoords = f

            // 处理各个顶点的颜色
            color = FloatArray(f.size * 4 / 3)
            val tempC: ArrayList<Float> = ArrayList()
            val totalC: ArrayList<Float> = ArrayList()
            val total0: ArrayList<Float> = ArrayList()
            total0.add(0.5f)
            total0.add(0.0f)
            total0.add(0.0f)
            total0.add(1.0f)
            tempC.add(1.0f)
            tempC.add(1.0f)
            tempC.add(1.0f)
            tempC.add(1.0f)
            for (i in 0 until f.size / 3) {
                if (i == 0) {
                    totalC.addAll(total0)
                } else {
                    totalC.addAll(tempC)
                }
            }
            for (i in 0 until totalC.size) {
                color[i] = totalC[i]
            }
        }

        private fun createPositions02(radius: Float, n: Int) {
            val data: ArrayList<Float> = ArrayList()
            data.add(0.0f) //设置锥心坐标
            data.add(0.0f)
            data.add(-0.5f)
            val angDegSpan = 360f / n
            run {
                var i = 0f
                while (i < 360 + angDegSpan) {
                    data.add((radius * Math.sin(i * Math.PI / 180f)).toFloat())
                    data.add((radius * Math.cos(i * Math.PI / 180f)).toFloat())
                    data.add(0.0f)
                    i += angDegSpan
                }
            }
            val f = FloatArray(data.size)
            for (i in f.indices) {
                f[i] = data[i]
            }
            circularCoords = f

            //处理各个顶点的颜色
            color = FloatArray(f.size * 4 / 3)
            val tempC: ArrayList<Float> = ArrayList()
            val totalC: ArrayList<Float> = ArrayList()
            val totalCForColor: ArrayList<Float> = ArrayList()
            tempC.add(0.8f)
            tempC.add(0.8f)
            tempC.add(0.8f)
            tempC.add(1.0f)
            val zeroIndexC: ArrayList<Float> = ArrayList()
            zeroIndexC.add(1.0f)
            zeroIndexC.add(0.0f)
            zeroIndexC.add(0.0f)
            zeroIndexC.add(1.0f)
            for (i in 0 until f.size / 3) {
                if (i == 0) {
                    totalC.addAll(zeroIndexC)
                } else {
                    totalC.addAll(tempC)
                }
                totalCForColor.addAll(tempC)
            }
            for (i in 0 until totalC.size) {
                color[i] = totalC[i]
            }
        }

        private fun createCircularPositions() {
            coneCoordsBottom = FloatArray(circularCoords.size)
            for (i in circularCoords.indices) {
                if (i == 2) {
                    coneCoordsBottom[i] = 0.0f
                } else {
                    coneCoordsBottom[i] = circularCoords[i]
                }
            }
        }

        override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
            // 将背景设置为白色
            GLES30.glClearColor(0.5f, 0.5f, 0.5f, 1.0f)
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
            aColorLocation = GLES30.glGetAttribLocation(mProgram, "aColor")
            Log.i(TAG, "onSurfaceCreated: $uMatrixLocation, $aPositionLocation, $aColorLocation")
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

            // 准备颜色数据
            GLES30.glVertexAttribPointer(aColorLocation, 4, GLES30.GL_FLOAT, false, 0, colorBuffer)
            // 启用顶点颜色句柄
            GLES30.glEnableVertexAttribArray(aColorLocation)

            // 绘制圆锥侧面
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, circularCoords.size / 3)

            //准备坐标数据
            GLES30.glVertexAttribPointer(aPositionLocation, 3, GLES30.GL_FLOAT, false, 0, vertexBufferBottom)
            //启用顶点位置句柄
            GLES30.glEnableVertexAttribArray(aPositionLocation)

            //绘制圆锥圆形底面
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, coneCoordsBottom.size / 3)

            // 禁止顶点数组的句柄
            GLES30.glDisableVertexAttribArray(aPositionLocation)
            GLES30.glDisableVertexAttribArray(aColorLocation)
        }
    }
}

