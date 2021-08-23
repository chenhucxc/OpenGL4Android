package com.dev.gles3

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.dev.gles3.example.*
import com.dev.gles3.player.VrGlSurfaceViewActivity
import com.dev.gles3.player.VrVideoActivity

/**
 * description : java 层 OpenGL ES 使用示例
 *
 * @author     : hudongxin
 * @date       : 8/14/21
 */
class OpenGl3MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_opengl_03)
        initOnclick()
    }

    private fun initOnclick() {
        // 基础
        findViewById<Button>(R.id.btn_basic01).setOnClickListener {
            startActivity(Intent(this, Basic01Activity::class.java))
        }
        // 转化
        findViewById<Button>(R.id.btn_case).setOnClickListener {
            startActivity(Intent(this, Basic02Activity::class.java))
        }
        // 矩形和圆
        findViewById<Button>(R.id.btn_gl_jni).setOnClickListener {
            startActivity(Intent(this, Basic03Activity::class.java))
        }
        // 纹理
        findViewById<Button>(R.id.btn_ripple).setOnClickListener {
            startActivity(Intent(this, Basic04Activity::class.java))
        }
        // 3d
        findViewById<Button>(R.id.btn_pano).setOnClickListener {
            startActivity(Intent(this, Basic05Activity::class.java))
        }
        // 球
        findViewById<Button>(R.id.btn_gl3).setOnClickListener {
            startActivity(Intent(this, Basic06Activity::class.java))
        }
        // VR
        findViewById<Button>(R.id.btn_vr_video).setOnClickListener {
            startActivity(Intent(this, VrGlSurfaceViewActivity::class.java))
        }
        // VR3
        findViewById<Button>(R.id.btn_video_03).setOnClickListener {
            startActivity(Intent(this, VrVideoActivity::class.java))
        }
    }
}