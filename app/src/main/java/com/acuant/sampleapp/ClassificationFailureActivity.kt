package com.acuant.sampleapp

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import com.acuant.acuantcamera.CapturedImage

class ClassificationFailureActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_classification_failure)

        val image = findViewById<ImageView>(R.id.classificationErrorImage)
        image.setImageBitmap(CapturedImage.acuantImage!!.image)
    }

    fun retryClassificationClicked(view: View) {
        val result = Intent()
        this@ClassificationFailureActivity.setResult(Constants.REQUEST_RETRY, result)
        this@ClassificationFailureActivity.finish()
    }
}
