@file:Suppress("DEPRECATION")

package com.acuant.acuantcamera.camera.barcode.cameraone

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.hardware.Camera
import android.os.Bundle
import android.os.CountDownTimer
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import com.acuant.acuantcamera.R
import com.acuant.acuantcamera.camera.AcuantBaseCameraFragment.CameraState
import com.acuant.acuantcamera.camera.AcuantCameraActivity
import com.acuant.acuantcamera.camera.AcuantCameraOptions
import com.acuant.acuantcamera.constant.ACUANT_EXTRA_CAMERA_OPTIONS
import com.acuant.acuantcamera.constant.ACUANT_EXTRA_IMAGE_URL
import com.acuant.acuantcamera.constant.ACUANT_EXTRA_PDF417_BARCODE
import com.acuant.acuantcamera.detector.ImageSaver
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.*

/**
 * Activity for the multi-tracker app.  This app detects barcodes and displays the value with the
 * rear facing camera. During detection overlay graphics are drawn to indicate the position,
 * size, and ID of each barcode.
 */
class BarcodeCaptureActivity : AppCompatActivity(), BarcodeCameraSource.PictureCallback, BarcodeCameraSource.ShutterCallback {
    private var barcodeCameraSource: BarcodeCameraSource? = null
    private var mPreview: BarcodeCameraSourcePreview? = null
    private var capturing = false
    private lateinit var barcodeProcessor: LiveBarcodeProcessor
    private var permissionNotGranted = false
    private var barcodeDetector: BarcodeDetector? = null

    private lateinit var instructionView: TextView
    private lateinit var imageView: ImageView

    private var capturedbarcodeString: String? = null

    private var defaultTextDrawable: Drawable? = null
    private var timeInMsPerDigit: Int = 800
    private var digitsToShow: Int = 2
    private lateinit var options: AcuantCameraOptions
    private lateinit var displaySize: Point
    private var lastOrientation = ORIENTATION_LANDSCAPE
    private lateinit var parent: RelativeLayout
    private lateinit var autoCancel: CountDownTimer

    /**
     * Initializes the UI and creates the detector pipeline.
     */
    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        supportActionBar?.title = ""
        supportActionBar?.hide()

        defaultTextDrawable = this.getDrawable(R.drawable.camera_text_config_default)

        setContentView(R.layout.activity_acu_barcode_camera)

        parent = findViewById(R.id.cam1_barcode_parent)
        mPreview = findViewById(R.id.cam1_barcode_preview)
        instructionView = findViewById(R.id.cam1_barcode_text)
        imageView = findViewById(R.id.cam1_barcode_image)


        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        val rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource(useFlash = false)
        } else {
            requestCameraPermission()
            permissionNotGranted = true
        }

        options = intent.getSerializableExtra(ACUANT_EXTRA_CAMERA_OPTIONS) as AcuantCameraOptions? ?: AcuantCameraOptions.BarcodeCameraOptionsBuilder().build()

        setOptions(options)

        setTextFromState(CameraState.Align)

        displaySize = Point()
        this.windowManager.defaultDisplay.getSize(displaySize)

        autoCancel = object : CountDownTimer(digitsToShow.toLong(), 500) {
            override fun onFinish() {
                doFinish()
            }

            override fun onTick(millisUntilFinished: Long) {
                //do nothing
            }
        }
        autoCancel.start()

        mOrientationEventListener = object : OrientationEventListener(this.applicationContext) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation < 0) {
                    return  // Flip screen, Not take account
                }
                val curOrientation: Int = when {
                    orientation <= 45 -> {
                        ORIENTATION_PORTRAIT
                    }
                    orientation <= 135 -> {
                        ORIENTATION_LANDSCAPE_REVERSE
                    }
                    orientation <= 225 -> {
                        ORIENTATION_PORTRAIT_REVERSE
                    }
                    orientation <= 315 -> {
                        ORIENTATION_LANDSCAPE
                    }
                    else -> {
                        ORIENTATION_PORTRAIT
                    }
                }
                if (curOrientation != lastOrientation) {
                    onChanged(lastOrientation, curOrientation)
                    lastOrientation = curOrientation
                }
            }
        }
    }

    fun doFinish() {
        this@BarcodeCaptureActivity.runOnUiThread {
            val result = Intent()
            result.putExtra(ACUANT_EXTRA_PDF417_BARCODE, this.capturedbarcodeString)
            setResult(AcuantCameraActivity.RESULT_SUCCESS_CODE, result)
            finish()
        }
    }

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private fun requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission")

        val permissions = arrayOf(Manifest.permission.CAMERA)

        val builder = AlertDialog.Builder(this)
        builder.setMessage(R.string.cam_perm_request_text)
                .setOnCancelListener {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                                    Manifest.permission.CAMERA)) {
                        ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM)
                    } else {
                        finish()
                    }
                }
                .setPositiveButton(R.string.ok
                ) { dialog, _ ->
                    dialog.dismiss()
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                                    Manifest.permission.CAMERA)) {
                        ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM)
                    } else {
                        finish()
                    }
                }
        builder.create().show()
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     *
     *
     * Suppressing InlinedApi since there is a check that the minimum version is met before using
     * the constant.
     */
    @Suppress("SameParameterValue")
    @SuppressLint("InlinedApi")
    private fun createCameraSource(useFlash: Boolean) {
        // Creates and starts the camera.  Note that this uses a higher resolution in comparison
        // to other detection examples to enable the barcode detector to detect small barcodes
        // at long distances.
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val height: Int = displayMetrics.heightPixels
        val width: Int = displayMetrics.widthPixels

        barcodeDetector = createDocumentDetector()
        var builder: BarcodeCameraSource.Builder = BarcodeCameraSource.Builder(applicationContext, barcodeDetector)
                .setFacing(BarcodeCameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(width, height)
                .setRequestedFps(60.0f)

        builder = builder.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)

        barcodeCameraSource = builder
                .setFlashMode(if (useFlash) Camera.Parameters.FLASH_MODE_TORCH else Camera.Parameters.FLASH_MODE_OFF)
                .build()
    }


    private fun createDocumentDetector(): BarcodeDetector {
        barcodeProcessor = LiveBarcodeProcessor()
        return barcodeProcessor.getBarcodeDetector(applicationContext) {
            if (it.feedback == BarcodeFeedback.Barcode) {
                this.capturedbarcodeString = it.barcode
                if (!capturing) {
                    runOnUiThread {
                        setTextFromState(CameraState.Capturing)
                        capturing = true
                        autoCancel.cancel()
                        object : CountDownTimer(timeInMsPerDigit.toLong(), 100) {
                            override fun onFinish() {

                                doFinish()
                            }

                            override fun onTick(millisUntilFinished: Long) {
                                //do nothing
                            }
                        }.start()
                    }
                }
            }
        }
    }

    private fun setOptions(options : AcuantCameraOptions) {
        this.timeInMsPerDigit = options.timeInMsPerDigit
        this.digitsToShow = options.digitsToShow
    }

    private lateinit var mOrientationEventListener: OrientationEventListener
    /**
     * Restarts the camera.
     */
    override fun onResume() {
        super.onResume()

        if (mOrientationEventListener.canDetectOrientation()) {
            mOrientationEventListener.enable()
        }
        startCameraSource()
    }

    /**
     * Stops the camera.
     */
    override fun onPause() {
        super.onPause()
        if (mPreview != null) {
            mPreview!!.stop()
        }
        mOrientationEventListener.disable()
    }

    /**
     * Releases the resources associated with the camera source, the associated detectors, and the
     * rest of the processing pipeline.
     */
    override fun onDestroy() {
        super.onDestroy()
        if (!permissionNotGranted) {
            barcodeProcessor.stop()
        }
        if (mPreview != null) {
            mPreview!!.release()
        }
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on [.requestPermissions].
     *
     *
     * **Note:** It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     *
     *
     * @param requestCode  The request code passed in [.requestPermissions].
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     * which is either [PackageManager.PERMISSION_GRANTED]
     * or [PackageManager.PERMISSION_DENIED]. Never null.
     * @see .requestPermissions
     */
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: $requestCode")
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            return
        }

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source")
            permissionNotGranted = false
            createCameraSource(useFlash = false)
            return
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.size +
                " Result code = " + if (grantResults.isNotEmpty()) grantResults[0] else "(empty)")

        val listener = DialogInterface.OnClickListener { _, _ -> finish() }

        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.camera_load_error)
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show()
    }

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    @Throws(SecurityException::class)
    private fun startCameraSource() {
        if (barcodeCameraSource != null && mPreview != null) {
            try {
                mPreview!!.start(barcodeCameraSource)
            } catch (e: IOException) {
                Log.e(TAG, "Unable to start camera source.", e)
                barcodeCameraSource!!.release()
                barcodeCameraSource = null
            }

        }
    }

    override fun onBackPressed() {
        this@BarcodeCaptureActivity.finish()
    }

    private fun setTextFromState(state: CameraState) {
        when(state) {
            CameraState.Capturing -> {
                imageView.visibility = View.GONE
                instructionView.background = defaultTextDrawable
                instructionView.text = getString(R.string.acuant_camera_capturing_barcode)
                instructionView.setTextColor(options.colorCapturing)
                instructionView.textSize = 24f
            }
            else -> {//align
                imageView.visibility = View.VISIBLE
                instructionView.background = defaultTextDrawable
                instructionView.text = getString(R.string.acuant_camera_align_barcode)
                instructionView.setTextColor(options.colorHold)
                instructionView.textSize = 24f
            }
        }
    }

    private fun onChanged(@Suppress("UNUSED_PARAMETER") lastOrientation: Int, curOrientation: Int) {

        runOnUiThread {
            if (curOrientation == ORIENTATION_LANDSCAPE_REVERSE) {
                rotateView(instructionView, 0f, 270f)


            } else if (curOrientation == ORIENTATION_LANDSCAPE) {
                rotateView(instructionView, 360f, 90f)


            }
        }

    }

    private fun rotateView(view: View?, startDeg: Float, endDeg: Float) {
        if (view != null) {
            view.rotation = startDeg
            view.animate().rotation(endDeg).start()
        }
    }


    override fun onPictureTaken(data: ByteArray) {
        val image = BitmapFactory.decodeByteArray(data, 0, data.size)

        if((image.width > image.height && this.lastOrientation == ORIENTATION_LANDSCAPE_REVERSE) ||
                (image.width < image.height && this.lastOrientation == ORIENTATION_LANDSCAPE)){
            val rotated = ImageSaver.rotateImage(image, 180f)
            val stream = ByteArrayOutputStream()
            rotated.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            rotated.recycle()

        }

        val file = File(this.cacheDir, "${UUID.randomUUID()}.jpg")

        this@BarcodeCaptureActivity.runOnUiThread {
            val result = Intent()
            result.putExtra(ACUANT_EXTRA_IMAGE_URL, file.absolutePath)
            result.putExtra(ACUANT_EXTRA_PDF417_BARCODE, this.capturedbarcodeString)
            setResult(AcuantCameraActivity.RESULT_SUCCESS_CODE, result)
            finish()
        }
    }

    override fun onShutter() {

        Log.d("onShutter", "onShutter")

    }

    companion object {
        private const val TAG = "Barcode-reader"

        // permission request codes need to be < 256
        private const val RC_HANDLE_CAMERA_PERM = 2
        private const val ORIENTATION_PORTRAIT_REVERSE = 4
        private const val ORIENTATION_LANDSCAPE_REVERSE = 3
    }
}
