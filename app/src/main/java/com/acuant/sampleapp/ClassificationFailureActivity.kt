package com.acuant.sampleapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

class ClassificationFailureActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.GRAY),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.GRAY)
        )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_classification_failure)


        val topLayout = findViewById<RelativeLayout>(R.id.classificationMessageLayout)

        ViewCompat.setOnApplyWindowInsetsListener(topLayout) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
                        or WindowInsetsCompat.Type.displayCutout()
            )
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = bars.top
            }
            WindowInsetsCompat.CONSUMED
        }


        val bottomLayout = findViewById<Button>(R.id.retryClassificationButton)
        ViewCompat.setOnApplyWindowInsetsListener(bottomLayout) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin += insets.bottom
            }
            WindowInsetsCompat.CONSUMED
        }

        val image = findViewById<ImageView>(R.id.classificationErrorImage)
        image.setImageBitmap(MainActivity.image)
    }

    fun retryClassificationClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        val result = Intent()
        this@ClassificationFailureActivity.setResult(RESULT_OK, result)
        this@ClassificationFailureActivity.finish()
    }
}
