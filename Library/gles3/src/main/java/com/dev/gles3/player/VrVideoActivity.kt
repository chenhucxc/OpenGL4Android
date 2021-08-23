package com.dev.gles3.player

import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.Surface
import android.view.WindowManager
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.dev.gles3.R
import kotlin.math.cos
import kotlin.math.sin

/**
 * description : VR 视频播放
 *
 * @author     : hudongxin
 * @date       : 8/17/21
 */
class VrVideoActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "VrVideoActivity"
        private const val VIDEO_URL =
            "http://cnvod.cnr.cn/audio2017/ondemand/transcode/l_target/wcm_system/video/20190403/xw0219xwyt22_56/index.m3u8"
        private const val TOUCH_SCALE_FACTOR = 180.0f / 640
    }

    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var renderer: VrVideoRenderer
    private lateinit var sensorManager: SensorManager
    private var mRotation: Sensor? = null

    private var mRotateMatrix = FloatArray(16)
    private var mTempRotateMatrix = FloatArray(16)

    private var mDeviceRotation = Surface.ROTATION_90

    // 处理触摸旋转
    private var startRawX = 0f
    private var startRawY = 0f

    private var xFlingAngle = 0.0
    private var xFlingAngleTemp = 0.0

    private var yFlingAngle = 0.0
    private var yFlingAngleTemp = 0.0

    private var mAngleX = 0f
    private var mAngleY = 90f
    private var mAngleZ = 1f

    // 缩放
    private var mScaleGestureDetector: ScaleGestureDetector? = null
    private var mPreScale = 1.0f
    private var mCurScale = 1.0f
    private var mLastMultiTouchTime: Long = 0

    /**
     * 陀螺仪
     */
    private val sensorEventListener: SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let { setRotateMatrix(it) }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

        }
    }

    private val onScaleGestureListener = object : ScaleGestureDetector.OnScaleGestureListener {
        override fun onScale(detector: ScaleGestureDetector?): Boolean {
            val preSpan: Float = detector?.previousSpan ?: 0f
            val curSpan: Float = detector?.currentSpan ?: 0f
            mCurScale = if (curSpan < preSpan) {
                mPreScale - (preSpan - curSpan) / 200
            } else {
                mPreScale + (curSpan - preSpan) / 200
            }
            mCurScale = Math.max(0.05f, Math.min(mCurScale, 80.0f))
            //renderer.updateTransformMatrix(mAngleX, mAngleY, mCurScale, mCurScale)
            //glSurfaceView.requestRender()
            return false
        }

        override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector?) {
            mPreScale = mCurScale
            mLastMultiTouchTime = System.currentTimeMillis()
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vr_glsurface_view)
        initView()
        initSensor()
        //initScaleListener()
    }

    private fun initView() {
        glSurfaceView = findViewById(R.id.play_vr_glsv)
        glSurfaceView.setEGLContextClientVersion(3)
        renderer = VrVideoRenderer(this, glSurfaceView, VIDEO_URL)
        glSurfaceView.setRenderer(renderer)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY

        findViewById<Button>(R.id.btn_switch_mode).setOnClickListener {
            changeDisplayMode()
        }
        findViewById<Button>(R.id.btn_switch_sensor).setOnClickListener {
            changeInteractionMode()
        }
    }

    private fun initSensor() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        mRotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        sensorManager.registerListener(sensorEventListener, mRotation, SensorManager.SENSOR_DELAY_GAME)
    }

    private fun initScaleListener() {
        mScaleGestureDetector = ScaleGestureDetector(this, onScaleGestureListener)
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "onResume: ")
    }

    override fun onPause() {
        renderer.onPause()
        super.onPause()
    }

    override fun onStop() {
        renderer.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        renderer.release()
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        mDeviceRotation = windowManager.defaultDisplay.rotation
        Log.i(TAG, "onConfigurationChanged: mDeviceRotation = $mDeviceRotation")
    }

    private var lastTime = System.currentTimeMillis()

    private fun setRotateMatrix(event: SensorEvent) {
        SensorManager.getRotationMatrixFromVector(mTempRotateMatrix, event.values)
        //Log.i(TAG, "setRotateMatrix: diff time = ${System.currentTimeMillis() - lastTime} ms")
        if (System.currentTimeMillis() - lastTime > 2000) {
            Log.e(TAG, "setRotateMatrix: values = ${event.values.asList()}")
            Log.i(TAG, "setRotateMatrix: mTempRotateMatrix = ${mTempRotateMatrix.asList()}")
        }
        val values = event.values
        //Log.i(TAG, "setRotateMatrix: mDeviceRotation = $mDeviceRotation")
        when (mDeviceRotation) {
            Surface.ROTATION_0 -> SensorManager.getRotationMatrixFromVector(mRotateMatrix, values)
            Surface.ROTATION_90 -> {
                SensorManager.getRotationMatrixFromVector(mTempRotateMatrix, values)
                SensorManager.remapCoordinateSystem(
                    mTempRotateMatrix,
                    SensorManager.AXIS_Y,
                    SensorManager.AXIS_MINUS_X,
                    mRotateMatrix
                )
            }
        }
        if (System.currentTimeMillis() - lastTime > 2000) {
            Log.i(TAG, "setRotateMatrix: mRotateMatrix = ${mRotateMatrix.asList()}")
            lastTime = System.currentTimeMillis()
        }
        renderer.setRotateMatrix(mRotateMatrix)
    }

    private fun changeInteractionMode() {
        renderer.changeInteractionMode()
    }

    private fun changeDisplayMode() {
        renderer.changeDisplayMode()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        // 处理手指滑动事件，我这里的处理是判断手指在横向和竖向滑动的距离
        // 这个距离隐射到球体上经度和纬度的距离，根据这个距离计算三维空间的两个
        // 夹角，根据这个夹角调整摄像机所在位置
        event?.let { me ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startRawX = me.rawX
                    startRawY = me.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    var distanceX = startRawX - me.rawX
                    var distanceY = startRawY - me.rawY

                    // 这里的0.1f是为了不上摄像机移动的过快
                    distanceY = TOUCH_SCALE_FACTOR * (distanceY) / windowManager.defaultDisplay.height

                    yFlingAngleTemp = distanceY * 180 / (Math.PI * 3)

                    if (yFlingAngleTemp + yFlingAngle > Math.PI / 2) {
                        yFlingAngleTemp = Math.PI / 2 - yFlingAngle
                    }
                    if (yFlingAngleTemp + yFlingAngle < -Math.PI / 2) {
                        yFlingAngleTemp = -Math.PI / 2 - yFlingAngle
                    }

                    // 这里的0.1f是为了不上摄像机移动的过快
                    distanceX = TOUCH_SCALE_FACTOR * (-distanceX) / windowManager.defaultDisplay.width
                    xFlingAngleTemp = distanceX * 180 / (Math.PI * 3)

                    mAngleX = (cos(yFlingAngle + yFlingAngleTemp) * sin(xFlingAngle + xFlingAngleTemp)).toFloat()
                    mAngleY = -(sin(yFlingAngle + yFlingAngleTemp)).toFloat()
                    mAngleZ = (cos(yFlingAngle + yFlingAngleTemp) * cos(xFlingAngle + xFlingAngleTemp)).toFloat()

                    renderer.setRotateAngle(mAngleX, mAngleY, mAngleZ)
                    glSurfaceView.requestRender()
                }
                MotionEvent.ACTION_UP -> {
                    xFlingAngle += xFlingAngleTemp
                    yFlingAngle += yFlingAngleTemp
                }
            }
        }
        return true
    }
}