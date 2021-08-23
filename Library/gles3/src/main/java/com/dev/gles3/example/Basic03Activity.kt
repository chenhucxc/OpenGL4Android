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
import com.dev.gles3.utils.LoadFileUtils
import com.dev.gles3.utils.ShaderUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


/**
 * description : OpenGL 3.0 基础示例，https://blog.csdn.net/gongxiaoou/article/details/89295973
 *
 * @author     : hudongxin
 * @date       : 8/14/21
 */
class Basic03Activity : AppCompatActivity() {
    companion object {
        private const val TAG = "Basic03Activity"
    }

    private lateinit var glSurfaceView: GLSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.basic_activity_common)
        initGlView(true)
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()
    }

    override fun onPause() {
        super.onPause()
        glSurfaceView.onPause()
    }

    private fun initGlView(isCircle: Boolean) {
        glSurfaceView = findViewById(R.id.glView)
        glSurfaceView.setEGLContextClientVersion(3)
        val mRender = if (isCircle) {
            CircleShapeRender(this)
        } else {
            SimpleShapeRender(this)
        }
        glSurfaceView.setRenderer(mRender)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
    }

    // 索引法：根据索引序列，在顶点序列中找到对应的顶点，并根据绘制的方式，组成相应的图元绘制
    private class SimpleShapeRender constructor(context: Context) : GLSurfaceView.Renderer {

        companion object {
            // 一个Float占用4Byte
            private const val BYTES_PER_FLOAT = 4
        }

        private var vertexBuffer: FloatBuffer // 顶点位置缓存
        private var colorBuffer: FloatBuffer // 顶点颜色缓存
        private var indicesBuffer: ShortBuffer // 顶点索引缓存
        private var mProgram = 0 // 渲染程序

        private var mViewMatrix = FloatArray(16) // 相机矩阵
        private var mProjectMatrix = FloatArray(16) // 投影矩阵
        private var mMVPMatrix = FloatArray(16) // 最终变换矩阵

        // 返回属性变量的位置
        private var uMatrixLocation = 0 // 变换矩阵
        private var aPositionLocation = 0 // 位置
        private var aColorLocation = 0 // 颜色

        // 四个顶点的位置参数
        private val rectangleCoords = floatArrayOf(
            -0.5f, 0.5f, 0.0f,//top left
            -0.5f, -0.5f, 0.0f, // bottom left
            0.5f, -0.5f, 0.0f, // bottom right
            0.5f, 0.5f, 0.0f // top right
        )

        // 顶点索引
        private val indices = shortArrayOf(
            0, 1, 2, 0, 2, 3
        )

        // 四个顶点的颜色参数
        private val color = floatArrayOf(
            0.0f, 0.0f, 1.0f, 1.0f,//top left
            0.0f, 1.0f, 0.0f, 1.0f,// bottom left
            0.0f, 0.0f, 1.0f, 1.0f,// bottom right
            1.0f, 0.0f, 0.0f, 1.0f// top right
        )

        // 顶点着色器和片段着色器
        private var vertexShaderStr =
            LoadFileUtils.readResource(context, R.raw.vertex_simple_shade_modified)
        private var fragmentShaderStr =
            LoadFileUtils.readResource(context, R.raw.fragment_simple_shade)

        init {
            // 顶点位置相关
            // 分配本地内存空间,每个浮点型占4字节空间；
            // 将坐标数据转换为FloatBuffer，用以传入给OpenGL ES程序
            vertexBuffer = ByteBuffer.allocateDirect(rectangleCoords.size * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            vertexBuffer.put(rectangleCoords)
            vertexBuffer.position(0)

            // 顶点颜色相关
            colorBuffer = ByteBuffer.allocateDirect(color.size * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            colorBuffer.put(color)
            colorBuffer.position(0)

            // 顶点索引相关
            indicesBuffer = ByteBuffer.allocateDirect(indices.size * 4)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer()
            indicesBuffer.put(indices)
            indicesBuffer.position(0)
        }

        override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
            // 将背景设置为白色
            GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f)
            // 编译顶点着色程序
            val vertexShaderId = ShaderUtils.compileVertexShader(vertexShaderStr)
            // 编译片段着色程序
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

            uMatrixLocation = GLES30.glGetUniformLocation(mProgram, "u_Matrix")
            aPositionLocation = GLES30.glGetAttribLocation(mProgram, "vPosition")
            aColorLocation = GLES30.glGetAttribLocation(mProgram, "aColor")
            Log.i(TAG, "onSurfaceCreated: $uMatrixLocation, $aPositionLocation, $aColorLocation")
        }

        override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
            // 设置绘制窗口
            GLES30.glViewport(0, 0, width, height)

            // 第一种方式：相机和透视投影方式
            // 计算宽高比
            val ratio = width.toFloat() / height
            // 设置透视投影
            Matrix.frustumM(mProjectMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)
            // 设置相机位置
            Matrix.setLookAtM(mViewMatrix, 0, 0f, 0f, 7.0f, 0f, 0f, 0f, 0f, 1.0f, 0.0f)
            // 计算变换矩阵
            Matrix.multiplyMM(mMVPMatrix, 0, mProjectMatrix, 0, mViewMatrix, 0)

            /*// 第二种方式：正交投影方式
            val aspectRatio = if (width > height) width.toFloat() / height.toFloat() else height.toFloat() / width.toFloat()
            if (width > height) { // 横屏
                Matrix.orthoM(mMVPMatrix, 0, -aspectRatio, aspectRatio, -1f, 1f, -1f, 1f)
            } else { // 竖屏
                Matrix.orthoM(mMVPMatrix, 0, -1f, 1f, -aspectRatio, aspectRatio, -1f, 1f)
            }*/
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
            GLES30.glVertexAttribPointer(
                aPositionLocation,
                3,
                GLES30.GL_FLOAT,
                false,
                0,
                vertexBuffer
            )
            // 启用顶点位置句柄
            GLES30.glEnableVertexAttribArray(aPositionLocation)

            // 准备颜色数据
            GLES30.glVertexAttribPointer(aColorLocation, 4, GLES30.GL_FLOAT, false, 0, colorBuffer)
            // 启用顶点颜色句柄
            GLES30.glEnableVertexAttribArray(aColorLocation)

            // 绘制三角形, GLES30.glDrawElements，称之为索引法
            GLES30.glDrawElements(
                GL10.GL_TRIANGLES,
                indices.size,
                GL10.GL_UNSIGNED_SHORT,
                indicesBuffer
            )

            // 禁止顶点数组的句柄
            GLES30.glDisableVertexAttribArray(aPositionLocation)
            GLES30.glDisableVertexAttribArray(aColorLocation)
        }
    }

    // 顶点法画圆形
    private class CircleShapeRender constructor(context: Context) : GLSurfaceView.Renderer {

        companion object {
            // 一个Float占用4Byte
            private const val BYTES_PER_FLOAT = 4
        }

        private var vertexBuffer: FloatBuffer // 顶点位置缓存
        private var colorBuffer: FloatBuffer // 顶点颜色缓存
        private var mProgram = 0 // 渲染程序

        private var mViewMatrix = FloatArray(16) // 相机矩阵
        private var mProjectMatrix = FloatArray(16) // 投影矩阵
        private var mMVPMatrix = FloatArray(16) // 最终变换矩阵

        // 返回属性变量的位置
        private var uMatrixLocation = 0 // 变换矩阵
        private var aPositionLocation = 0 // 位置
        private var aColorLocation = 0 // 颜色

        // 圆形顶点位置
        private lateinit var circularCoords: FloatArray

        // 顶点的颜色
        private lateinit var color: FloatArray

        // 顶点着色器和片段着色器
        private var vertexShaderStr =
            LoadFileUtils.readResource(context, R.raw.vertex_simple_shade_modified)
        private var fragmentShaderStr =
            LoadFileUtils.readResource(context, R.raw.fragment_simple_shade)

        init {
            createPositions(1, 60)
            // 顶点位置相关
            // 分配本地内存空间,每个浮点型占4字节空间；
            // 将坐标数据转换为FloatBuffer，用以传入给OpenGL ES程序
            vertexBuffer = ByteBuffer.allocateDirect(circularCoords.size * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            vertexBuffer.put(circularCoords)
            vertexBuffer.position(0)

            // 顶点颜色相关
            colorBuffer = ByteBuffer.allocateDirect(color.size * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            colorBuffer.put(color)
            colorBuffer.position(0)
        }

        private fun createPositions(radius: Int, n: Int) {
            val data: ArrayList<Float> = ArrayList()
            // 设置圆心坐标
            data.add(0.0f)
            data.add(0.0f)
            data.add(0.0f)
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
            tempC.add(1.0f)
            tempC.add(0.0f)
            tempC.add(0.0f)
            tempC.add(1.0f)
            for (i in 0 until f.size / 3) {
                totalC.addAll(tempC)
            }
            for (i in 0 until totalC.size) {
                color[i] = totalC[i]
            }
        }

        override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
            // 将背景设置为白色
            GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f)
            // 编译顶点着色程序
            val vertexShaderId = ShaderUtils.compileVertexShader(vertexShaderStr)
            // 编译片段着色程序
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

            uMatrixLocation = GLES30.glGetUniformLocation(mProgram, "u_Matrix")
            aPositionLocation = GLES30.glGetAttribLocation(mProgram, "vPosition")
            aColorLocation = GLES30.glGetAttribLocation(mProgram, "aColor")
            Log.i(TAG, "onSurfaceCreated: $uMatrixLocation, $aPositionLocation, $aColorLocation")
        }

        override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
            // 设置绘制窗口
            GLES30.glViewport(0, 0, width, height)

            // 第一种方式：相机和透视投影方式
            // 计算宽高比
            val ratio = width.toFloat() / height
            // 设置透视投影
            Matrix.frustumM(mProjectMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)
            // 设置相机位置
            Matrix.setLookAtM(mViewMatrix, 0, 0f, 0f, 7.0f, 0f, 0f, 0f, 0f, 1.0f, 0.0f)
            // 计算变换矩阵
            Matrix.multiplyMM(mMVPMatrix, 0, mProjectMatrix, 0, mViewMatrix, 0)

            /*// 第二种方式：正交投影方式
            val aspectRatio = if (width > height) width.toFloat() / height.toFloat() else height.toFloat() / width.toFloat()
            if (width > height) { // 横屏
                Matrix.orthoM(mMVPMatrix, 0, -aspectRatio, aspectRatio, -1f, 1f, -1f, 1f)
            } else { // 竖屏
                Matrix.orthoM(mMVPMatrix, 0, -1f, 1f, -aspectRatio, aspectRatio, -1f, 1f)
            }*/
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
            GLES30.glVertexAttribPointer(
                aPositionLocation,
                3,
                GLES30.GL_FLOAT,
                false,
                0,
                vertexBuffer
            )
            // 启用顶点位置句柄
            GLES30.glEnableVertexAttribArray(aPositionLocation)

            // 准备颜色数据
            GLES30.glVertexAttribPointer(aColorLocation, 4, GLES30.GL_FLOAT, false, 0, colorBuffer)
            // 启用顶点颜色句柄
            GLES30.glEnableVertexAttribArray(aColorLocation)

            // 绘制圆形
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, circularCoords.size / 3)

            // 禁止顶点数组的句柄
            GLES30.glDisableVertexAttribArray(aPositionLocation)
            GLES30.glDisableVertexAttribArray(aColorLocation)
        }
    }
}

