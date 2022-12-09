package com.acuant.acuantcamera.camera

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.acuant.acuantcamera.R
import com.acuant.acuantcamera.camera.barcode.AcuantBarcodeCameraFragment
import com.acuant.acuantcamera.camera.document.AcuantDocCameraFragment
import com.acuant.acuantcamera.camera.mrz.AcuantMrzCameraFragment
import com.acuant.acuantcamera.interfaces.ICameraActivityFinish
import com.acuant.acuantcamera.constant.*
import com.acuant.acuantcamera.databinding.ActivityCameraBinding
import com.acuant.acuantcamera.helper.MrzResult
import com.acuant.acuantcamera.language.LocaleManager
import com.acuant.acuantcommon.model.AcuantError
import java.util.*

class AcuantCameraActivity : AppCompatActivity(), ICameraActivityFinish {

    private lateinit var binding: ActivityCameraBinding

    //Camera Launch
    override fun onCreate(savedInstanceState: Bundle?) {
        val unserializedOptions = intent.getSerializableExtra(ACUANT_EXTRA_CAMERA_OPTIONS)

        val options: AcuantCameraOptions = if (unserializedOptions == null) {
            AcuantCameraOptions.DocumentCameraOptionsBuilder().build()
        } else {
            unserializedOptions as AcuantCameraOptions
        }
        LocaleManager.updateResources(this, Locale(options.language))
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        if (options.preventScreenshots) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }

        setContentView(binding.root)
        hideTopMenu()

        //start the camera if this is the first time the activity is created (camera already exists otherwise)
        if (savedInstanceState == null) {
            val cameraFragment: AcuantBaseCameraFragment = when (options.cameraMode) {
                AcuantCameraOptions.CameraMode.BarcodeOnly -> {
                    AcuantBarcodeCameraFragment.newInstance(options)
                }
                AcuantCameraOptions.CameraMode.Mrz -> {
                    AcuantMrzCameraFragment.newInstance(options)
                }
                else -> { //Document
                    AcuantDocCameraFragment.newInstance(options)
                }
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.container, cameraFragment as Fragment)
                .commit()
        }
    }

    //Camera Responses
    override fun onCameraDone(imageUrl: String, barCodeString: String?) {
        val intent = Intent()
        intent.putExtra(ACUANT_EXTRA_IMAGE_URL, imageUrl)
        intent.putExtra(ACUANT_EXTRA_PDF417_BARCODE, barCodeString)
        this@AcuantCameraActivity.setResult(RESULT_OK, intent)
        this@AcuantCameraActivity.finish()
    }

    override fun onCameraDone(mrzResult: MrzResult) {
        val intent = Intent()
        intent.putExtra(ACUANT_EXTRA_MRZ_RESULT, mrzResult)
        this@AcuantCameraActivity.setResult(RESULT_OK, intent)
        this@AcuantCameraActivity.finish()
    }

    override fun onCameraDone(barCodeString: String) {
        val intent = Intent()
        intent.putExtra(ACUANT_EXTRA_PDF417_BARCODE, barCodeString)
        this@AcuantCameraActivity.setResult(RESULT_OK, intent)
        this@AcuantCameraActivity.finish()
    }

    override fun onCancel() {
        val intent = Intent()
        this@AcuantCameraActivity.setResult(RESULT_CANCELED, intent)
        this@AcuantCameraActivity.finish()
    }

    override fun onError(error: AcuantError) {
        val intent = Intent()
        intent.putExtra(ACUANT_EXTRA_ERROR, error)
        this@AcuantCameraActivity.setResult(RESULT_ERROR, intent)
        this@AcuantCameraActivity.finish()
    }

    //misc/housekeeping
    override fun onBackPressed() {
        onCancel()
    }

    private fun hideTopMenu() {
        actionBar?.hide()
        supportActionBar?.hide()
    }

    override fun onResume() {
        super.onResume()
        hideTopMenu()
    }
}