package com.dev.gles3.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.*
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
 * description : 纹理贴图，https://blog.csdn.net/gongxiaoou/article/details/89463784
 *
 * @author     : hudongxin
 * @date       : 8/14/21
 */
class Basic04Activity : AppCompatActivity() {
    companion object {
        private const val TAG = "Basic03Activity"
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
        private val mContext = context

        companion object {
            // 一个Float占用4Byte
            private const val BYTES_PER_FLOAT = 4

            /**
             * 顶点坐标
             * (x,y,z)
             */
            private val POSITION_VERTEX = floatArrayOf(
                0f, 0f, 0f,     // 顶点坐标V0
                1f, 1f, 0f,     // 顶点坐标V1
                -1f, 1f, 0f,    // 顶点坐标V2
                -1f, -1f, 0f,   // 顶点坐标V3
                1f, -1f, 0f     // 顶点坐标V4
            )

            /**
             * 绘制顺序索引
             */
            private val VERTEX_INDEX = shortArrayOf(
                0, 1, 2,  // V0,V1,V2 三个顶点组成一个三角形
                0, 2, 3,  //  V0,V2,V3 三个顶点组成一个三角形
                0, 3, 4,  // V0,V3,V4 三个顶点组成一个三角形
                0, 4, 1   // V0,V4,V1 三个顶点组成一个三角形
            )

            /**
             * 纹理坐标
             * (s,t)
             */
            private val TEX_VERTEX = floatArrayOf(
                0.5f, 0.5f, // 纹理坐标V0
                1f, 0f,     // 纹理坐标V1
                0f, 0f,     // 纹理坐标V2
                0f, 1.0f,   // 纹理坐标V3
                1f, 1.0f    // 纹理坐标V4
            )
        }

        private var vertexBuffer: FloatBuffer
        private var mTexVertexBuffer: FloatBuffer
        private var mVertexIndexBuffer: ShortBuffer

        private var mProgram = 0 // 渲染程序
        private var textureId: Int = 0 // 纹理id

        private var mViewMatrix = FloatArray(16) // 相机矩阵
        private var mProjectMatrix = FloatArray(16) // 投影矩阵
        private var mMVPMatrix = FloatArray(16) // 最终变换矩阵

        // 返回属性变量的位置
        private var uMatrixLocation = 0 // 变换矩阵
        private var aPositionLocation = 0 // 顶点
        private var aTextureLocation = 0 // 纹理

        private var mBitmap: Bitmap? = null // 图片生成的位图

        // 顶点着色器和片段着色器
        private var vertexShaderStr =
            LoadFileUtils.readResource(context, R.raw.vertex_texture2d_shader)
        private var fragmentShaderStr =
            LoadFileUtils.readResource(context, R.raw.fragment_texture2d_shader)

        init {
            vertexBuffer = ByteBuffer.allocateDirect(POSITION_VERTEX.size * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(POSITION_VERTEX)
            vertexBuffer.position(0)

            mTexVertexBuffer = ByteBuffer.allocateDirect(TEX_VERTEX.size * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(TEX_VERTEX)
            mTexVertexBuffer.position(0)

            mVertexIndexBuffer = ByteBuffer.allocateDirect(VERTEX_INDEX.size * 2)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer()
                .put(VERTEX_INDEX)
            mVertexIndexBuffer.position(0)
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
            GLES30.glTexParameteri(
                GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_MIN_FILTER,
                GLES30.GL_LINEAR_MIPMAP_LINEAR
            )
            GLES30.glTexParameteri(
                GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_MAG_FILTER,
                GLES30.GL_LINEAR
            )

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
            Log.e(TAG, "onSurfaceCreated: ******************************")
            // 将背景设置为白色
            GLES20.glClearColor(0f, 0f, 0f, 1.0f)
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
            aTextureLocation = GLES30.glGetAttribLocation(mProgram, "aTextureCoord")
            Log.i(TAG, "onSurfaceCreated: $uMatrixLocation, $aPositionLocation, $aTextureLocation")

            textureId = loadTexture(mContext, R.drawable.front)
        }

        override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
            // 设置绘制窗口
            GLES30.glViewport(0, 0, width, height)

            val w = mBitmap!!.width
            val h = mBitmap!!.height
            val sWH = w / h.toFloat()
            val sWidthHeight = width / height.toFloat()
            if (width > height) {
                if (sWH > sWidthHeight) {
                    Matrix.orthoM(
                        mProjectMatrix,
                        0,
                        -sWidthHeight * sWH,
                        sWidthHeight * sWH,
                        -1f,
                        1f,
                        3f,
                        7f
                    )
                } else {
                    Matrix.orthoM(
                        mProjectMatrix,
                        0,
                        -sWidthHeight / sWH,
                        sWidthHeight / sWH,
                        -1f,
                        1f,
                        3f,
                        7f
                    )
                }
            } else {
                if (sWH > sWidthHeight) {
                    Matrix.orthoM(
                        mProjectMatrix,
                        0,
                        -1f,
                        1f,
                        -1 / sWidthHeight * sWH,
                        1 / sWidthHeight * sWH,
                        3f,
                        7f
                    )
                } else {
                    Matrix.orthoM(
                        mProjectMatrix,
                        0,
                        -1f,
                        1f,
                        -sWH / sWidthHeight,
                        sWH / sWidthHeight,
                        3f,
                        7f
                    )
                }
            }
            // 设置相机位置
            Matrix.setLookAtM(mViewMatrix, 0, 0f, 0f, 7.0f, 0f, 0f, 0f, 0f, 1.0f, 0.0f)
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

            // 启用纹理坐标属性
            GLES30.glEnableVertexAttribArray(aTextureLocation)
            GLES30.glVertexAttribPointer(
                aTextureLocation,
                2,
                GLES30.GL_FLOAT,
                false,
                0,
                mTexVertexBuffer
            )
            // 激活纹理
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            //绑定纹理
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)

            // 绘制
            GLES30.glDrawElements(
                GLES20.GL_TRIANGLES,
                VERTEX_INDEX.size,
                GLES20.GL_UNSIGNED_SHORT,
                mVertexIndexBuffer
            )

            // 禁止顶点数组的句柄
            GLES30.glDisableVertexAttribArray(aPositionLocation)
            GLES30.glDisableVertexAttribArray(aTextureLocation)
        }
    }
}

