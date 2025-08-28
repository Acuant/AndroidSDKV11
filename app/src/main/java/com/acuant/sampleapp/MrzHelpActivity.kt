package com.acuant.sampleapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.ImageView
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge

class MrzHelpActivity : AppCompatActivity()  {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.GRAY),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.GRAY)
        )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mrz_help)

        val mrzHelpScreen : ConstraintLayout = findViewById(R.id.main_mrz_help_layout)
        findViewById<ImageView>(R.id.main_mrz_image)?.setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        mrzHelpScreen.setOnClickListener {
            showMrzCaptureCamera()
        }
    }

    private fun showMrzCaptureCamera() {
        val result = Intent()
        this@MrzHelpActivity.setResult(RESULT_OK, result)
        this@MrzHelpActivity.finish()
    }
}
