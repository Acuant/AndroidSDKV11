package com.acuant.acuantcamera.camera

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.acuant.acuantcamera.R
import com.acuant.acuantcamera.constant.ACUANT_EXTRA_BORDER_ENABLED
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

    private fun hideTopMenu(){
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        actionBar?.hide()
        supportActionBar?.hide()
    }

    override fun onResume() {
        super.onResume()
        hideTopMenu()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_acu_camera)
        hideTopMenu()

        if (null == savedInstanceState) {
            val cameraFragment =  AcuantCameraFragment.newInstance()
            cameraFragment.arguments = Bundle().apply {
                putBoolean(ACUANT_EXTRA_IS_AUTO_CAPTURE, intent.getBooleanExtra(ACUANT_EXTRA_IS_AUTO_CAPTURE, true))
                putBoolean(ACUANT_EXTRA_BORDER_ENABLED, intent.getBooleanExtra(ACUANT_EXTRA_BORDER_ENABLED, true))
            }

            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, cameraFragment)
                    .commit()
        }
    }

    override fun onBackPressed() {
        this@AcuantCameraActivity.finish()
    }
}
