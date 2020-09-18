package com.acuant.sampleapp

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.ImageView

class ClassificationFailureActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_classification_failure)

        val image = findViewById<ImageView>(R.id.classificationErrorImage)
        image.setImageBitmap(MainActivity.image)
    }

    fun retryClassificationClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        val result = Intent()
        this@ClassificationFailureActivity.setResult(Constants.REQUEST_RETRY, result)
        this@ClassificationFailureActivity.finish()
    }
}
