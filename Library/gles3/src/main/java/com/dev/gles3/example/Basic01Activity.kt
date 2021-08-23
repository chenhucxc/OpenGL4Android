package com.dev.gles3.example

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.dev.gles3.R
import com.dev.gles3.utils.LoadFileUtils
import com.dev.gles3.utils.ShaderUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


/**
 * description : OpenGL 3.0 基础示例，https://blog.csdn.net/gongxiaoou/article/details/89289320
 *
 * @author     : hudongxin
 * @date       : 8/14/21
 */
class Basic01Activity : AppCompatActivity() {
    companion object {
        private const val TAG = "BasicActivity"
    }

    private lateinit var glSurfaceView: GLSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.basic_activity_common)
        initGlView()
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()
    }

    override fun onPause() {
        super.onPause()
        glSurfaceView.onPause()
    }

    private fun initGlView() {
        glSurfaceView = findViewById(R.id.glView)
        glSurfaceView.setEGLContextClientVersion(3)
        val mRender = SimpleShapeRender(this)
        glSurfaceView.setRenderer(mRender)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
    }

    private class SimpleShapeRender constructor(context: Context) : GLSurfaceView.Renderer {

        companion object {
            // 一个Float占用4Byte
            private const val BYTES_PER_FLOAT = 4

            // 三个顶点
            private const val POSITION_COMPONENT_COUNT = 3
        }

        // 顶点位置缓存
        private var vertexBuffer: FloatBuffer

        // 顶点颜色缓存
        private var colorBuffer: FloatBuffer

        // 渲染程序
        private var mProgram = 0

        // 顶点着色器和片段着色器
        private var vertexShaderStr = LoadFileUtils.readResource(context, R.raw.vertex_simple_shade)
        private var fragmentShaderStr =
            LoadFileUtils.readResource(context, R.raw.fragment_simple_shade)

        // 三个顶点的位置参数
        private val triangleCoords = floatArrayOf(
            0.5f, 0.5f, 0.0f,  // top
            -0.5f, -0.5f, 0.0f,  // bottom left
            0.5f, -0.5f, 0.0f // bottom right
        )

        // 三个顶点的颜色参数
        private val color = floatArrayOf(
            1.0f, 0.0f, 0.0f, 1.0f,  // top
            0.0f, 1.0f, 0.0f, 1.0f,  // bottom left
            0.0f, 0.0f, 1.0f, 1.0f // bottom right
        )

        init {
            // 顶点位置相关
            // 分配本地内存空间,每个浮点型占4字节空间；
            // 将坐标数据转换为FloatBuffer，用以传入给OpenGL ES程序
            vertexBuffer = ByteBuffer.allocateDirect(triangleCoords.size * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            vertexBuffer.put(triangleCoords);
            vertexBuffer.position(0)

            // 顶点颜色相关
            colorBuffer = ByteBuffer.allocateDirect(color.size * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            colorBuffer.put(color)
            colorBuffer.position(0)
        }

        override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
            Log.i(TAG, "onSurfaceCreated: vertexShaderStr = \n $vertexShaderStr")
            Log.i(TAG, "onSurfaceCreated: vertexShaderStr =  \n $fragmentShaderStr")
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
        }

        override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
            // 设置绘制窗口
            GLES30.glViewport(0, 0, width, height)
        }

        /**
         * GLES30.glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride, Buffer ptr)
         * index：顶点属性的索引.（这里我们的顶点位置和颜色向量在着色器中分别为0和1）layout (location = 0) in vec4 vPosition; layout (location = 1) in vec4 aColor;
         * size: 指定每个通用顶点属性的元素个数。必须是1、2、3、4。此外，glvertexattribpointer接受符号常量gl_bgra。初始值为4（也就是涉及颜色的时候必为4）。
         * type：属性的元素类型。（上面都是Float所以使用GLES30.GL_FLOAT）；
         * normalized：转换的时候是否要经过规范化，true：是；false：直接转化；
         * stride：跨距，默认是0。（由于我们将顶点位置和颜色数据分别存放没写在一个数组中，所以使用默认值0）
         * ptr： 本地数据缓存（这里我们的是顶点的位置和颜色数据）。
         */
        override fun onDrawFrame(unused: GL10) {
            // 把颜色缓冲区设置为我们预设的颜色
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

            // 准备坐标数据
            GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer)
            // 启用顶点位置句柄
            GLES30.glEnableVertexAttribArray(0)

            // 准备颜色数据
            GLES30.glVertexAttribPointer(1, 4, GLES30.GL_FLOAT, false, 0, colorBuffer)
            // 启用顶点颜色句柄
            GLES30.glEnableVertexAttribArray(1)

            // 绘制三个点
            //GLES30.glDrawArrays(GLES30.GL_POINTS, 0, POSITION_COMPONENT_COUNT)

            // 绘制三条线
            GLES30.glLineWidth(3f) // 设置线宽
            GLES30.glDrawArrays(GLES30.GL_LINE_LOOP, 0, POSITION_COMPONENT_COUNT)

            // 绘制三角形
            //GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, POSITION_COMPONENT_COUNT)

            // 禁止顶点数组的句柄
            GLES30.glDisableVertexAttribArray(0)
            GLES30.glDisableVertexAttribArray(1)
        }
    }
}

