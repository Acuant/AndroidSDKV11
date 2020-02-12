package com.acuant.acuantcamera.camera

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.acuant.acuantcamera.R
import com.acuant.acuantcamera.camera.cameraone.DocumentCaptureActivity
import com.acuant.acuantcamera.constant.*


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

        if(supportCamera2(this)){
            setContentView(R.layout.activity_acu_camera)
            hideTopMenu()

            if (null == savedInstanceState) {
                val cameraFragment =  AcuantCameraFragment.newInstance()
                cameraFragment.arguments = Bundle().apply {
                    putBoolean(ACUANT_EXTRA_IS_AUTO_CAPTURE, intent.getBooleanExtra(ACUANT_EXTRA_IS_AUTO_CAPTURE, true))
                    putBoolean(ACUANT_EXTRA_BORDER_ENABLED, intent.getBooleanExtra(ACUANT_EXTRA_BORDER_ENABLED, true))
                    putSerializable(ACUANT_EXTRA_CAMERA_OPTIONS, intent.getSerializableExtra(ACUANT_EXTRA_CAMERA_OPTIONS))
                }

                supportFragmentManager.beginTransaction()
                        .replace(R.id.container, cameraFragment)
                        .commit()
            }
        }
        else{
            val options = intent.getSerializableExtra(ACUANT_EXTRA_CAMERA_OPTIONS) as AcuantCameraOptions? ?: AcuantCameraOptions(autoCapture = true, allowBox = true)
            val cameraIntent = Intent(
                    this@AcuantCameraActivity,
                    DocumentCaptureActivity::class.java
            )
            options.autoCapture = options.autoCapture || intent.getBooleanExtra(ACUANT_EXTRA_IS_AUTO_CAPTURE, false)
            options.allowBox =  options.allowBox || intent.getBooleanExtra(ACUANT_EXTRA_BORDER_ENABLED, false)

            cameraIntent.putExtra(ACUANT_EXTRA_CAMERA_OPTIONS, intent.getSerializableExtra(ACUANT_EXTRA_CAMERA_OPTIONS))
            startActivityForResult(cameraIntent, 1)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when(requestCode){
            1 -> {
                if(resultCode == RESULT_SUCCESS_CODE && data != null){
                    onActivityFinish(data.getStringExtra(ACUANT_EXTRA_IMAGE_URL), data.getStringExtra(ACUANT_EXTRA_PDF417_BARCODE))
                }
                else{
                    this@AcuantCameraActivity.finish()
                }
            }
        }
    }

    override fun onBackPressed() {
        this@AcuantCameraActivity.finish()
    }

    private fun supportCamera2(activity: Activity): Boolean {
        // Check if we're running on Android 5.0 or higher
        /*if(Build.MANUFACTURER!=null && Build.MANUFACTURER.toLowerCase().contains("samsung")){
            return false;
        }*/
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (Build.MODEL == "Nexus 9") {
                false
            } else hasSuitableCamera2Camera(activity)
        } else false
    }

    private fun hasSuitableCamera2Camera(activity: Activity): Boolean {
        var foundCamera = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val manager = activity.applicationContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            try {
                // Find first back-facing camera that has necessary capability.
                val cameraIds = manager.cameraIdList
                for (id in cameraIds) {
                    val info = manager.getCameraCharacteristics(id)
                    val facing = info.get(CameraCharacteristics.LENS_FACING)!!

                    val level = info.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)!!
                    val hasFullLevel = level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL

                    val syncLatency = info.get(CameraCharacteristics.SYNC_MAX_LATENCY)!!
                    val hasEnoughCapability = syncLatency == CameraCharacteristics.SYNC_MAX_LATENCY_PER_FRAME_CONTROL

                    // All these are guaranteed by
                    // CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL, but checking
                    // for only the things we care about expands range of devices we can run on.
                    // We want:
                    //  - Back-facing camera
                    //  - Per-frame synchronization (so that exposure can be changed every frame)
                    if (facing == CameraCharacteristics.LENS_FACING_BACK && (hasFullLevel || hasEnoughCapability)) {
                        // Found suitable camera - get info, open, and set up outputs
                        foundCamera = true
                        break
                    }
                }

            } catch (e: Exception) {

            }

        }
        return foundCamera

    }
}
