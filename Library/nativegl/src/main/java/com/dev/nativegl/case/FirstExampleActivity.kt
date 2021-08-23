package com.dev.nativegl.case

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dev.nativegl.MyGLRender
import com.dev.nativegl.MyGLSurfaceView

/**
 * description : clone from https://github.com/githubhaohao/NDK_OpenGLES_3_0
 *
 * @author     : hudongxin
 * @date       : 8/11/21
 */
class FirstExampleActivity : AppCompatActivity() {

    private lateinit var mGLSurfaceView: MyGLSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Create a GLSurfaceView instance and set it
        // as the ContentView for this Activity.
        mGLSurfaceView = MyGLSurfaceView(this, MyGLRender())
        val metrics = resources.displayMetrics
        mGLSurfaceView.setAspectRatio(metrics.widthPixels, metrics.heightPixels)
        //gLView.performClick()
        setContentView(mGLSurfaceView)
    }
}