package com.acuant.acuantcamera.camera

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.acuant.acuantcamera.R
import com.acuant.acuantcamera.constant.ACUANT_EXTRA_IMAGE_URL
import com.acuant.acuantcamera.constant.ACUANT_EXTRA_IS_AUTO_CAPTURE
import com.acuant.acuantcamera.constant.ACUANT_EXTRA_PDF417_BARCODE

interface ICameraActivityFinish{
    fun onActivityFinish(imageUrl: String, barCodeString: String?)
}

class AcuantCameraActivity : AppCompatActivity(), ICameraActivityFinish {
    companion object {
        const val RESULT_SUCCESS_CODE = 1
    }
    override fun onActivityFinish(imageUrl: String, barCodeString: String?) {
        val intent = Intent()
        intent.putExtra(ACUANT_EXTRA_IMAGE_URL, imageUrl)
        intent.putExtra(ACUANT_EXTRA_PDF417_BARCODE, barCodeString)
        this@AcuantCameraActivity.setResult(RESULT_SUCCESS_CODE, intent)
        this@AcuantCameraActivity.finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_acu_camera)

        //window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        window.decorView.apply {
            // Hide both the navigation bar and the status bar.
            // SYSTEM_UI_FLAG_FULLSCREEN is only available on Android 4.1 and higher, but as
            // a general rule, you should design your app to hide the status bar whenever you
            // hide the navigation bar.
            systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        }
        actionBar?.hide()
        supportActionBar?.hide()
        if (null == savedInstanceState) {
            val cameraFragment =  AcuantCameraFragment.newInstance()
            cameraFragment.arguments = Bundle().apply {
                putBoolean(ACUANT_EXTRA_IS_AUTO_CAPTURE, intent.getBooleanExtra(ACUANT_EXTRA_IS_AUTO_CAPTURE, true))
            }

            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, cameraFragment)
                    .commit()
        }
    }
}
