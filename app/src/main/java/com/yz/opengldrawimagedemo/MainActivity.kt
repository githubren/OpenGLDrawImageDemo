package com.yz.opengldrawimagedemo

import android.opengl.GLSurfaceView
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = GLSurfaceView(this)
        //设置版本 要与manifest中保持一致
        view.setEGLContextClientVersion(2)
        view.setRenderer(ImageRender(this))
        setContentView(view)
    }
}