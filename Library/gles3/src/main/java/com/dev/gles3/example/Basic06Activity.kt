package com.dev.gles3.example

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.GLSurfaceView
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dev.gles3.R
import com.dev.gles3.render.BallRenderer
import com.dev.gles3.render.CylinderRenderer
import com.dev.gles3.render.EarthMapRenderer
import com.dev.gles3.render.EarthMapRotateRenderer

/**
 * description : 球和 VR 示例，https://blog.csdn.net/gongxiaoou/article/details/89484843
 *
 * @author     : hudongxin
 * @date       : 8/14/21
 */
class Basic06Activity : AppCompatActivity() {
    companion object {
        private const val TAG = "Basic06Activity"
    }

    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var sensorManager: SensorManager
    private lateinit var mRotation: Sensor
    private var mRotateMatrix: FloatArray = FloatArray(16)
    private lateinit var renderer: EarthMapRotateRenderer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.basic_activity_common)
        // 球形
        //initGlView(0)

        // 方向 + 球形
        initRotateGlView()
        initSensorRotate()
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
            0 -> EarthMapRenderer(this)
            1 -> CylinderRenderer(this)
            2 -> BallRenderer(this)
            else -> EarthMapRenderer(this)
        }
        glSurfaceView.setRenderer(mRender)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
    }

    private fun initRotateGlView() {
        glSurfaceView = findViewById(R.id.glView)
        glSurfaceView.setEGLContextClientVersion(3)
        renderer = EarthMapRotateRenderer(this)
        glSurfaceView.setRenderer(renderer)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
    }

    private fun initSensorRotate() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mRotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        sensorManager.registerListener(object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                setRotateMatrix(event)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

            }
        }, mRotation, SensorManager.SENSOR_DELAY_GAME)
    }

    private fun setRotateMatrix(event: SensorEvent) {
        SensorManager.getRotationMatrixFromVector(mRotateMatrix, event.values)
        renderer.setRotateMatrix(mRotateMatrix)
    }
}

