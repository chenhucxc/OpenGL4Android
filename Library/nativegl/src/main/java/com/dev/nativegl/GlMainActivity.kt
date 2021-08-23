package com.dev.nativegl

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.*
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dev.nativegl.*
import com.dev.nativegl.MyGLSurfaceView.Companion.IMAGE_FORMAT_GARY
import com.dev.nativegl.MyGLSurfaceView.Companion.IMAGE_FORMAT_NV21
import com.dev.nativegl.MyGLSurfaceView.Companion.IMAGE_FORMAT_RGBA
import com.dev.nativegl.MyNativeRender.Companion.SAMPLE_TYPE
import com.dev.nativegl.MyNativeRender.Companion.SAMPLE_TYPE_KEY_BEATING_HEART
import com.dev.nativegl.audio.AudioCollector
import com.dev.nativegl.egl.EGLActivity
import com.dev.nativegl.utils.LocalFileUtils
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.*

/**
 * description : clone from https://github.com/githubhaohao/NDK_OpenGLES_3_0
 *
 * @author     : hudongxin
 * @date       : 8/11/21
 */
class GlMainActivity : AppCompatActivity(), AudioCollector.Callback, ViewTreeObserver.OnGlobalLayoutListener,
    SensorEventListener {

    companion object {
        private const val TAG = "GlMainActivity"
        private val SAMPLE_TITLES = arrayOf(
            "DrawTriangle",
            "TextureMap",
            "YUV Rendering",
            "VAO&VBO",
            "FBO Offscreen Rendering",
            "EGL Background Rendering",
            "FBO Stretching",
            "Coordinate System",
            "Basic Lighting",
            "Transform Feedback",
            "Complex Lighting",
            "Depth Testing",
            "Instancing",
            "Stencil Testing",
            "Blending",
            "Particles",
            "SkyBox",
            "Assimp Load 3D Model",
            "PBO",
            "Beating Heart",
            "Cloud",
            "Time Tunnel",
            "Bezier Curve",
            "Big Eyes",
            "Face Slender",
            "Big Head",
            "Rotary Head",
            "Visualize Audio",
            "Scratch Card",
            "3D Avatar",
            "Shock Wave",
            "MRT",
            "FBO Blit",
            "Texture Buffer",
            "Uniform Buffer",
            "OpenGL RGB to YUV",
            "Multi-Thread Render",
            "Text Render",
            "Portrait stay color",
            "GL Transitions"
        )
    }

    private var mGLSurfaceView: MyGLSurfaceView? = null
    private lateinit var mRootView: ViewGroup
    private var mSampleSelectedIndex: Int = SAMPLE_TYPE_KEY_BEATING_HEART - SAMPLE_TYPE
    private var mAudioCollector: AudioCollector? = null
    private val mGLRender = MyGLRender()
    private lateinit var mSensorManager: SensorManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_gl)
        mRootView = findViewById(R.id.rootView)
        mRootView.viewTreeObserver.addOnGlobalLayoutListener(this)
        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        mGLRender.init()
    }

    override fun onGlobalLayout() {
        mRootView.viewTreeObserver.removeOnGlobalLayoutListener(this)
        val lp = ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        lp.topToTop = ViewGroup.LayoutParams.MATCH_PARENT
        lp.bottomToBottom = ViewGroup.LayoutParams.MATCH_PARENT
        lp.leftToLeft = ViewGroup.LayoutParams.MATCH_PARENT
        lp.endToEnd = ViewGroup.LayoutParams.MATCH_PARENT

        mGLSurfaceView = MyGLSurfaceView(this, mGLRender)
        mRootView.addView(mGLSurfaceView, lp)
        mGLSurfaceView?.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
    }

    override fun onResume() {
        super.onResume()
        mSensorManager.registerListener(
            this,
            mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
            SensorManager.SENSOR_DELAY_FASTEST
        )
        val fileDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.absolutePath ?: ""
        LocalFileUtils.copyAssetsDirToSDCard(this, "poly", "$fileDir/model")
        LocalFileUtils.copyAssetsDirToSDCard(this, "fonts", fileDir)
    }

    override fun onPause() {
        super.onPause()
        mSensorManager.unregisterListener(this)
        mAudioCollector?.unInit()
        mAudioCollector = null
    }

    override fun onDestroy() {
        super.onDestroy()
        mGLRender.unInit()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_change_sample) {
            showGLSampleDialog()
        }
        return true
    }

    override fun onAudioBufferCallback(buffer: ShortArray?) {
        Log.e(TAG, "onAudioBufferCallback() called with: buffer[0] = [" + buffer!![0] + "]")
        mGLRender.setAudioData(buffer)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_GRAVITY -> {
                Log.d(
                    TAG,
                    "onSensorChanged() called with TYPE_GRAVITY: [x,y,z] = [" + event.values[0] + ", " + event.values[1] + ", " + event.values[2] + "]"
                )
                if (mSampleSelectedIndex + SAMPLE_TYPE === MyNativeRender.SAMPLE_TYPE_KEY_AVATAR) {
                    mGLRender.setGravityXY(event.values[0], event.values[1])
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    private fun showGLSampleDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = LayoutInflater.from(this)
        val rootView = inflater.inflate(R.layout.sample_selected_layout, null)
        val dialog = builder.create()
        val confirmBtn = rootView.findViewById<Button>(R.id.confirm_btn)
        confirmBtn.setOnClickListener { dialog.cancel() }
        val resolutionsListView: RecyclerView = rootView.findViewById(R.id.resolution_list_view)
        val myPreviewSizeViewAdapter = MenuListAdapter(this, SAMPLE_TITLES.toList())
        myPreviewSizeViewAdapter.setSelectIndex(mSampleSelectedIndex)
        myPreviewSizeViewAdapter.addOnItemClickListener(object : MenuListAdapter.OnItemClickListener {
            override fun onItemClick(view: View?, position: Int) {
                mRootView.removeView(mGLSurfaceView)
                /*val lp = RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                )
                lp.addRule(RelativeLayout.CENTER_IN_PARENT)*/
                val lp = ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                lp.topToTop = ViewGroup.LayoutParams.MATCH_PARENT
                lp.bottomToBottom = ViewGroup.LayoutParams.MATCH_PARENT
                lp.leftToLeft = ViewGroup.LayoutParams.MATCH_PARENT
                lp.endToEnd = ViewGroup.LayoutParams.MATCH_PARENT
                mGLSurfaceView = MyGLSurfaceView(this@GlMainActivity, mGLRender)
                mRootView.addView(mGLSurfaceView, lp)

                val selectIndex: Int = myPreviewSizeViewAdapter.getSelectIndex()
                myPreviewSizeViewAdapter.setSelectIndex(position)
                myPreviewSizeViewAdapter.notifyItemChanged(selectIndex)
                myPreviewSizeViewAdapter.notifyItemChanged(position)

                mSampleSelectedIndex = position
                mGLSurfaceView!!.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
                if (mRootView.width != mGLSurfaceView!!.width
                    || mRootView.height != mGLSurfaceView!!.height
                ) {
                    mGLSurfaceView!!.setAspectRatio(mRootView.width, mRootView.height)
                }
                mGLRender.setParamsInt(SAMPLE_TYPE, position + SAMPLE_TYPE, 0)
                val sampleType = position + SAMPLE_TYPE
                Log.i(TAG, "onItemClick: sampleType = $sampleType")
                val tmp: Bitmap?
                when (sampleType) {
                    MyNativeRender.SAMPLE_TYPE_TRIANGLE -> {
                    }
                    MyNativeRender.SAMPLE_TYPE_TEXTURE_MAP -> loadRGBAImage(R.drawable.dzzz)
                    MyNativeRender.SAMPLE_TYPE_YUV_TEXTURE_MAP -> loadNV21Image()
                    MyNativeRender.SAMPLE_TYPE_VAO -> {
                    }
                    MyNativeRender.SAMPLE_TYPE_FBO -> loadRGBAImage(R.drawable.java)
                    MyNativeRender.SAMPLE_TYPE_FBO_LEG -> loadRGBAImage(R.drawable.leg)
                    MyNativeRender.SAMPLE_TYPE_EGL -> startActivity(Intent(this@GlMainActivity, EGLActivity::class.java))
                    MyNativeRender.SAMPLE_TYPE_COORD_SYSTEM,
                    MyNativeRender.SAMPLE_TYPE_BASIC_LIGHTING,
                    MyNativeRender.SAMPLE_TYPE_TRANS_FEEDBACK,
                    MyNativeRender.SAMPLE_TYPE_MULTI_LIGHTS,
                    MyNativeRender.SAMPLE_TYPE_DEPTH_TESTING,
                    MyNativeRender.SAMPLE_TYPE_INSTANCING,
                    MyNativeRender.SAMPLE_TYPE_STENCIL_TESTING -> loadRGBAImage(
                        R.drawable.board_texture
                    )
                    MyNativeRender.SAMPLE_TYPE_BLENDING -> {
                        loadRGBAImage(R.drawable.board_texture, 0)
                        loadRGBAImage(R.drawable.floor, 1)
                        loadRGBAImage(R.drawable.window, 2)
                    }
                    MyNativeRender.SAMPLE_TYPE_PARTICLES -> {
                        loadRGBAImage(R.drawable.board_texture)
                        mGLSurfaceView!!.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                    }
                    MyNativeRender.SAMPLE_TYPE_SKYBOX -> {
                        val startTime = System.currentTimeMillis()
                        loadRGBAImage(R.drawable.right, 0)
                        loadRGBAImage(R.drawable.left, 1)
                        loadRGBAImage(R.drawable.top, 2)
                        loadRGBAImage(R.drawable.bottom, 3)
                        loadRGBAImage(R.drawable.back, 4)
                        loadRGBAImage(R.drawable.front, 5)
                        val endTime = System.currentTimeMillis()
                        Log.e(TAG, "onItemClick: load img time = ${endTime - startTime} ms")
                    }
                    MyNativeRender.SAMPLE_TYPE_PBO -> {
                        loadRGBAImage(R.drawable.front)
                        mGLSurfaceView!!.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                    }
                    SAMPLE_TYPE_KEY_BEATING_HEART -> mGLSurfaceView!!.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                    MyNativeRender.SAMPLE_TYPE_KEY_CLOUD -> {
                        loadRGBAImage(R.drawable.noise)
                        mGLSurfaceView!!.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                    }
                    MyNativeRender.SAMPLE_TYPE_KEY_TIME_TUNNEL -> {
                        loadRGBAImage(R.drawable.front)
                        mGLSurfaceView!!.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                    }
                    MyNativeRender.SAMPLE_TYPE_KEY_BEZIER_CURVE ->                         //loadRGBAImage(R.drawable.board_texture);
                        mGLSurfaceView!!.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                    MyNativeRender.SAMPLE_TYPE_KEY_BIG_EYES,
                    MyNativeRender.SAMPLE_TYPE_KEY_FACE_SLENDER -> {
                        val bitmap = loadRGBAImage(R.drawable.yifei)
                        mGLSurfaceView!!.setAspectRatio(bitmap!!.width, bitmap.height)
                        mGLSurfaceView!!.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                    }
                    MyNativeRender.SAMPLE_TYPE_KEY_BIG_HEAD,
                    MyNativeRender.SAMPLE_TYPE_KEY_ROTARY_HEAD -> {
                        val b = loadRGBAImage(R.drawable.huge)
                        mGLSurfaceView!!.setAspectRatio(b!!.width, b.height)
                        mGLSurfaceView!!.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                    }
                    MyNativeRender.SAMPLE_TYPE_KEY_VISUALIZE_AUDIO -> {
                        if (mAudioCollector == null) {
                            mAudioCollector = AudioCollector()
                            mAudioCollector!!.addCallback(this@GlMainActivity)
                            mAudioCollector!!.init()
                        }
                        mGLSurfaceView!!.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                        val b1 = loadRGBAImage(R.drawable.yifei)
                        mGLSurfaceView!!.setAspectRatio(b1!!.width, b1.height)
                    }
                    MyNativeRender.SAMPLE_TYPE_KEY_SCRATCH_CARD -> {
                        val b1 = loadRGBAImage(R.drawable.yifei)
                        mGLSurfaceView!!.setAspectRatio(b1!!.width, b1.height)
                    }
                    MyNativeRender.SAMPLE_TYPE_KEY_AVATAR -> {
                        val b2 = loadRGBAImage(R.drawable.avatar_a, 0)
                        mGLSurfaceView!!.setAspectRatio(b2!!.width, b2.height)
                        loadRGBAImage(R.drawable.avatar_b, 1)
                        loadRGBAImage(R.drawable.avatar_c, 2)
                        mGLSurfaceView!!.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                    }
                    MyNativeRender.SAMPLE_TYPE_KEY_SHOCK_WAVE,
                    MyNativeRender.SAMPLE_TYPE_KEY_MULTI_THREAD_RENDER,
                    MyNativeRender.SAMPLE_TYPE_KEY_TEXT_RENDER -> {
                        val b3 = loadRGBAImage(R.drawable.lye)
                        mGLSurfaceView!!.setAspectRatio(b3!!.width, b3.height)
                        mGLSurfaceView!!.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                    }
                    MyNativeRender.SAMPLE_TYPE_KEY_MRT,
                    MyNativeRender.SAMPLE_TYPE_KEY_FBO_BLIT,
                    MyNativeRender.SAMPLE_TYPE_KEY_TBO,
                    MyNativeRender.SAMPLE_TYPE_KEY_UBO,
                    MyNativeRender.SAMPLE_TYPE_KEY_RGB2YUV -> {
                        val b4 = loadRGBAImage(R.drawable.lye)
                        mGLSurfaceView!!.setAspectRatio(b4!!.width, b4.height)
                    }
                    MyNativeRender.SAMPLE_TYPE_KEY_STAY_COLOR -> {
                        loadGrayImage()
                        val b5 = loadRGBAImage(R.drawable.lye2)
                        loadRGBAImage(R.drawable.ascii_mapping, 1)
                        mGLSurfaceView!!.setAspectRatio(b5!!.width, b5.height)
                        mGLSurfaceView!!.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                    }
                    MyNativeRender.SAMPLE_TYPE_KEY_TRANSITION -> {
                        val startTime = System.currentTimeMillis()
                        loadRGBAImage(R.drawable.lye, 0)
                        loadRGBAImage(R.drawable.lye4, 1)
                        loadRGBAImage(R.drawable.lye5, 2)
                        loadRGBAImage(R.drawable.lye6, 3)
                        loadRGBAImage(R.drawable.lye7, 4)
                        tmp = loadRGBAImage(R.drawable.lye8, 5)
                        val loaded = System.currentTimeMillis()
                        mGLSurfaceView!!.setAspectRatio(tmp!!.width, tmp.height)
                        mGLSurfaceView!!.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                        val endTime = System.currentTimeMillis()
                        Log.e(
                            TAG,
                            "onItemClick: load img time = ${loaded - startTime} ms, complete time = ${endTime - startTime} ms"
                        )
                    }
                    else -> {
                    }
                }
                mGLSurfaceView!!.requestRender()
                if (sampleType != MyNativeRender.SAMPLE_TYPE_KEY_VISUALIZE_AUDIO && mAudioCollector != null) {
                    mAudioCollector!!.unInit()
                    mAudioCollector = null
                }
                dialog.cancel()
            }
        })
        val manager = LinearLayoutManager(this)
        manager.orientation = LinearLayoutManager.VERTICAL
        resolutionsListView.layoutManager = manager
        resolutionsListView.adapter = myPreviewSizeViewAdapter
        resolutionsListView.scrollToPosition(mSampleSelectedIndex)
        dialog.show()
        dialog.window!!.setContentView(rootView)
    }

    private fun loadRGBAImage(resId: Int): Bitmap? {
        val inputStream = this.resources.openRawResource(resId)
        val bitmap: Bitmap?
        try {
            bitmap = BitmapFactory.decodeStream(inputStream)
            if (bitmap != null) {
                val bytes = bitmap.byteCount
                val buf = ByteBuffer.allocate(bytes)
                bitmap.copyPixelsToBuffer(buf)
                val byteArray = buf.array()
                mGLRender.setImageData(IMAGE_FORMAT_RGBA, bitmap.width, bitmap.height, byteArray)
            }
        } finally {
            try {
                inputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return bitmap
    }

    private fun loadRGBAImage(resId: Int, index: Int): Bitmap? {
        val inputStream = this.resources.openRawResource(resId)
        val bitmap: Bitmap?
        try {
            bitmap = BitmapFactory.decodeStream(inputStream)
            if (bitmap != null) {
                val bytes = bitmap.byteCount
                val buf = ByteBuffer.allocate(bytes)
                bitmap.copyPixelsToBuffer(buf)
                val byteArray = buf.array()
                mGLRender.setImageDataWithIndex(index, IMAGE_FORMAT_RGBA, bitmap.width, bitmap.height, byteArray)
            }
        } finally {
            try {
                inputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return bitmap
    }

    private fun loadNV21Image() {
        var inputStream: InputStream? = null
        try {
            inputStream = assets.open("YUV_Image_840x1074.NV21")
        } catch (e: IOException) {
            e.printStackTrace()
        }
        var lenght = 0
        try {
            lenght = inputStream!!.available()
            val buffer = ByteArray(lenght)
            inputStream.read(buffer)
            mGLRender.setImageData(IMAGE_FORMAT_NV21, 840, 1074, buffer)
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                inputStream!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun loadGrayImage() {
        var inputStream: InputStream? = null
        try {
            inputStream = assets.open("lye_1280x800.Gray")
        } catch (e: IOException) {
            e.printStackTrace()
        }
        var lenght = 0
        try {
            lenght = inputStream!!.available()
            val buffer = ByteArray(lenght)
            inputStream.read(buffer)
            mGLRender.setImageDataWithIndex(0, IMAGE_FORMAT_GARY, 1280, 800, buffer)
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                inputStream!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}