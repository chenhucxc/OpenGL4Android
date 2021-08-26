package com.dev.opengl4android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.dev.gles3.OpenGl3MainActivity
import com.dev.nativegl.GlMainActivity

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    private val permissions = mutableListOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            add(Manifest.permission.ACCESS_MEDIA_LOCATION)
        }
    }

    private val permissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.all { it.value }) {
                onPermissionGranted()
            } else {
                Log.i(TAG, ": no permission")
            }
        }

    /**
     * Check for the permissions
     */
    private fun allPermissionsGranted() = permissions.all {
        Log.e(TAG, "allPermissionsGranted: permission = $it")
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.btn_java).setOnClickListener {
            startActivity(Intent(this, OpenGl3MainActivity::class.java))
        }
        findViewById<Button>(R.id.btn_jni).setOnClickListener {
            startActivity(Intent(this, GlMainActivity::class.java))
        }
        requestPermissions()
    }
    
    private fun requestPermissions() {
        if (allPermissionsGranted()) {
            Log.i(TAG, "onResume: yes")
            onPermissionGranted()
        } else {
            Log.i(TAG, "onResume: not")
            permissionRequest.launch(permissions.toTypedArray())
        }
    }

    /**
     * A function which will be called after the permission check
     * */
    open fun onPermissionGranted() = Unit
}