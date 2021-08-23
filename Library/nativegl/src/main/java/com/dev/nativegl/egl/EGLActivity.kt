package com.dev.nativegl.egl

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLException
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.dev.nativegl.R
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.IntBuffer

/**
 * description : clone from https://github.com/githubhaohao/NDK_OpenGLES_3_0
 *
 * @author     : hudongxin
 * @date       : 8/11/21
 */
class EGLActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "EGLActivity"
        private const val PARAM_TYPE_SHADER_INDEX = 200
    }

    private lateinit var mImageView: ImageView
    private lateinit var mBtn: Button
    private lateinit var mBgRender: NativeEglRender

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_egl)
        mImageView = findViewById<View>(R.id.imageView) as ImageView
        mBtn = findViewById<View>(R.id.button) as Button
        mBgRender = NativeEglRender()
        mBgRender.nativeEglRenderInit()

        mBtn.setOnClickListener {
            if (mBtn.text == this@EGLActivity.resources.getString(R.string.btn_txt_reset)) {
                mImageView.setImageResource(R.drawable.leg)
                mBtn.setText(R.string.bg_render_txt)
            } else {
                startBgRender()
                mBtn.setText(R.string.btn_txt_reset)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mBgRender.nativeEglRenderUnInit()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_egl, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        var shaderIndex = 0
        when (id) {
            R.id.action_shader0 -> shaderIndex = 0
            R.id.action_shader1 -> shaderIndex = 1
            R.id.action_shader2 -> shaderIndex = 2
            R.id.action_shader3 -> shaderIndex = 3
            R.id.action_shader4 -> shaderIndex = 4
            R.id.action_shader5 -> shaderIndex = 5
            R.id.action_shader6 -> shaderIndex = 6
            else -> {

            }
        }

        mBgRender.nativeEglRenderSetIntParams(PARAM_TYPE_SHADER_INDEX, shaderIndex)
        startBgRender()
        mBtn.setText(R.string.btn_txt_reset)
        return true
    }

    private fun startBgRender() {
        loadRGBAImage(R.drawable.leg, mBgRender)
        mBgRender.nativeEglRenderDraw()
        mImageView.setImageBitmap(createBitmapFromGLSurface(0, 0, 933, 1400))
    }

    private fun loadRGBAImage(resId: Int, render: NativeEglRender) {
        val `is` = this.resources.openRawResource(resId)
        val bitmap: Bitmap?
        try {
            bitmap = BitmapFactory.decodeStream(`is`)
            if (bitmap != null) {
                val bytes = bitmap.byteCount
                val buf = ByteBuffer.allocate(bytes)
                bitmap.copyPixelsToBuffer(buf)
                val byteArray = buf.array()
                render.nativeEglRenderSetImageData(byteArray, bitmap.width, bitmap.height)
            }
        } finally {
            try {
                `is`.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun createBitmapFromGLSurface(x: Int, y: Int, w: Int, h: Int): Bitmap? {
        val bitmapBuffer = IntArray(w * h)
        val bitmapSource = IntArray(w * h)
        val intBuffer = IntBuffer.wrap(bitmapBuffer)
        intBuffer.position(0)
        try {
            GLES20.glReadPixels(
                x, y, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
                intBuffer
            )
            var offset1: Int
            var offset2: Int
            for (i in 0 until h) {
                offset1 = i * w
                offset2 = (h - i - 1) * w
                for (j in 0 until w) {
                    val texturePixel = bitmapBuffer[offset1 + j]
                    val blue = texturePixel shr 16 and 0xff
                    val red = texturePixel shl 16 and 0x00ff0000
                    val pixel = texturePixel and -0xff0100 or red or blue
                    bitmapSource[offset2 + j] = pixel
                }
            }
        } catch (e: GLException) {
            return null
        }
        return Bitmap.createBitmap(bitmapSource, w, h, Bitmap.Config.ARGB_8888)
    }
}