package com.acuant.acuantcamera.camera

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
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
import com.acuant.acuantcommon.model.AcuantError

class AcuantCameraActivity: AppCompatActivity(), ICameraActivityFinish {

    private lateinit var binding: ActivityCameraBinding

    //Camera Launch
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)


        var options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getSerializableExtra(ACUANT_EXTRA_CAMERA_OPTIONS, AcuantCameraOptions::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getSerializableExtra(ACUANT_EXTRA_CAMERA_OPTIONS) as AcuantCameraOptions?
        }

        if (options == null)
            options = AcuantCameraOptions.DocumentCameraOptionsBuilder().build()

        if (options.preventScreenshots) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
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

    override fun onCameraDone(imageBytes: ByteArray, barCodeString: String?) {
        val intent = Intent()
        //We can not do this as the maximum transaction size is way too small.
//        intent.putExtra(ACUANT_EXTRA_IMAGE_BYTES, imageBytes)
        AcuantCameraActivity.imageBytes = imageBytes
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

    override fun onCameraDone(imageBytes: ByteArray, mrzResult: MrzResult) {
        val intent = Intent()
        AcuantCameraActivity.imageBytes = imageBytes
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

    companion object {
        private var imageBytes: ByteArray? = null

        @JvmStatic
        @Synchronized
        fun getLatestCapturedBytes(clearBytesAfterRead: Boolean): ByteArray? {
            val bytes = imageBytes?.clone()
            if (clearBytesAfterRead) {
                if (imageBytes != null) {
                    for (i in imageBytes!!.indices) {
                        imageBytes!![i] = (0).toByte()
                    }
                    imageBytes = null
                }
            }
            return bytes
        }
    }
}
