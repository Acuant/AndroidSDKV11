package com.acuant.acuantfacecapture.camera

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import com.acuant.acuantcommon.model.AcuantError
import com.acuant.acuantfacecapture.R
import com.acuant.acuantfacecapture.camera.facecapture.AcuantFaceCaptureFragment
import com.acuant.acuantfacecapture.constant.Constants.ACUANT_EXTRA_FACE_CAPTURE_OPTIONS
import com.acuant.acuantfacecapture.constant.Constants.ACUANT_EXTRA_FACE_ERROR
import com.acuant.acuantfacecapture.constant.Constants.ACUANT_EXTRA_FACE_IMAGE_URL
import com.acuant.acuantfacecapture.constant.Constants.RESULT_ERROR
import com.acuant.acuantfacecapture.databinding.ActivityFaceCameraBinding
import com.acuant.acuantfacecapture.interfaces.IFaceCameraActivityFinish
import com.acuant.acuantfacecapture.model.CameraMode
import com.acuant.acuantfacecapture.model.FaceCaptureOptions

class AcuantFaceCameraActivity: AppCompatActivity(), IFaceCameraActivityFinish {

    private lateinit var binding: ActivityFaceCameraBinding

    //Camera Launch
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        binding = ActivityFaceCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        hideTopMenu()

        var options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getSerializableExtra(ACUANT_EXTRA_FACE_CAPTURE_OPTIONS, FaceCaptureOptions::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getSerializableExtra(ACUANT_EXTRA_FACE_CAPTURE_OPTIONS) as FaceCaptureOptions?
        }

        if (options == null)
            options = FaceCaptureOptions()

        //start the camera if this is the first time the activity is created (camera already exists otherwise)
        if (savedInstanceState == null) {
            val cameraFragment: AcuantBaseFaceCameraFragment = when (options.cameraMode) {
                CameraMode.HgLiveness, CameraMode.FaceCapture -> {
                    AcuantFaceCaptureFragment.newInstance(options)
                }
            }
            if (options.cameraMode == CameraMode.HgLiveness && cameraFragment is AcuantFaceCaptureFragment) {
                cameraFragment.changeToHGLiveness()
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.container, cameraFragment as Fragment)
                .commit()
        }
    }

    //Camera Responses
    override fun onCameraDone(imageUrl: String) {
        val intent = Intent()
        intent.putExtra(ACUANT_EXTRA_FACE_IMAGE_URL, imageUrl)
        this.setResult(RESULT_OK, intent)
        this.finish()
    }

    override fun onCancel() {
        val intent = Intent()
        this.setResult(RESULT_CANCELED, intent)
        this.finish()
    }

    override fun onError(error: AcuantError) {
        val intent = Intent()
        intent.putExtra(ACUANT_EXTRA_FACE_ERROR, error)
        this.setResult(RESULT_ERROR, intent)
        this.finish()
    }

    //misc/housekeeping
    override fun onBackPressed() {
        onCancel()
    }

    private fun hideTopMenu() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        actionBar?.hide()
        supportActionBar?.hide()
    }

    override fun onResume() {
        super.onResume()
        hideTopMenu()
    }
}
