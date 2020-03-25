package com.acuant.acuantcamera.camera.mrz.cameraone

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Point
import android.hardware.Camera
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.OrientationEventListener
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

import java.io.IOException

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.widget.ImageView
import com.acuant.acuantcamera.R
import com.acuant.acuantcamera.camera.*
import com.acuant.acuantcamera.camera.mrz.AcuantMrzCameraFragment
import com.acuant.acuantcamera.camera.mrz.AcuantMrzCameraFragment.Companion.setTextFromState
import com.acuant.acuantcamera.constant.ACUANT_EXTRA_CAMERA_OPTIONS
import com.acuant.acuantcamera.constant.ACUANT_EXTRA_IS_AUTO_CAPTURE
import com.acuant.acuantcamera.constant.ACUANT_EXTRA_MRZ_RESULT
import com.acuant.acuantcamera.detector.ocr.AcuantOcrDetector
import com.acuant.acuantcamera.detector.ocr.AcuantOcrDetectorHandler
import com.acuant.acuantcamera.helper.MrzParser
import com.acuant.acuantcamera.helper.MrzResult
import com.acuant.acuantcamera.overlay.MrzRectangleView
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Activity for the multi-tracker app.  This app detects barcodes and displays the value with the
 * rear facing camera. During detection overlay graphics are drawn to indicate the position,
 * size, and ID of each barcode.
 */
class DocumentCaptureActivity : AppCompatActivity(), DocumentCameraSource.PictureCallback, DocumentCameraSource.ShutterCallback, AcuantOcrDetectorHandler {

    private var documentCameraSource: DocumentCameraSource? = null
    private var mPreview: DocumentCameraSourcePreview? = null
    private var waitTime = 2
    private var autoCapture = false
    private lateinit var documentProcessor: LiveDocumentProcessor
    private var documentDetector: DocumentDetector? = null

    private lateinit var instructionView: TextView
    private lateinit var imageView: ImageView

    private var capturedbarcodeString: String? = null

    private lateinit var rectangleView: MrzRectangleView
    private lateinit var tvlp: RelativeLayout.LayoutParams

    private var capturingTextDrawable: Drawable? = null
    private var defaultTextDrawable: Drawable? = null
    private var oldPoints : Array<Point>? = null
    private val mrzParser = MrzParser()
    private var tries = 0
    private val handler = Handler()
    private var mrzResult : MrzResult? = null
    private var capturing = false
    private var allowCapture = false

    override fun onPointsDetected(points: Array<Point>?) {
        runOnUiThread {
            if (points != null) {
                if (points.size == 4) {
                    val scaledPointY = mPreview!!.mSurfaceView.height.toFloat() / (documentDetector?.frame?.width?.toFloat() ?: mPreview!!.mSurfaceView.height.toFloat())
                    val scaledPointX = mPreview!!.mSurfaceView.width.toFloat() / (documentDetector?.frame?.height?.toFloat() ?: mPreview!!.mSurfaceView.width.toFloat())
                    rectangleView.setWidth(mPreview!!.mSurfaceView.width.toFloat())
                    points.apply {
                        this.forEach {
                            it.x = (it.x * scaledPointY).toInt()
                            it.y = (it.y * scaledPointX).toInt()
                        }
                    }

                    MrzRectangleView.fixPoints(points)

                    var dist = 0
                    if (oldPoints != null && oldPoints!!.size == 4 && points.size == 4) {
                        for (i in 0..3) {
                            dist += sqrt(((oldPoints!![i].x - points[i].x).toDouble().pow(2) + (oldPoints!![i].y - points[i].y).toDouble().pow(2))).toInt()
                        }
                    }

                    if (dist > AcuantMrzCameraFragment.TOO_MUCH_MOVEMENT) {
                        resetCapture()
                    }

                    if (capturing) {
                        setTextFromState(this, AcuantBaseCameraFragment.CameraState.MrzCapturing, instructionView, imageView)
                        rectangleView.setViewFromState(AcuantBaseCameraFragment.CameraState.MrzCapturing)
                    } else if (!AcuantMrzCameraFragment.isAligned(points) || !AcuantMrzCameraFragment.isAcceptableAspectRatio(points)) {
                        resetCapture()
                        setTextFromState(this, AcuantBaseCameraFragment.CameraState.MrzAlign, instructionView, imageView)
                        rectangleView.setViewFromState(AcuantBaseCameraFragment.CameraState.MrzAlign)
                        rectangleView.setAndDrawPoints(null)
                    } else if(!AcuantMrzCameraFragment.isAcceptableDistance(points, mPreview?.mSurfaceView?.height?.toFloat() ?: 0f)) {
                        resetCapture()
                        setTextFromState(this, AcuantBaseCameraFragment.CameraState.MrzMoveCloser, instructionView, imageView)
                        rectangleView.setViewFromState(AcuantBaseCameraFragment.CameraState.MrzMoveCloser)
                        rectangleView.setAndDrawPoints(points)
                    }else if (tries < AcuantMrzCameraFragment.ALLOWED_ERRORS) {
                        allowCapture = true
                        setTextFromState(this, AcuantBaseCameraFragment.CameraState.MrzTrying, instructionView, imageView)
                        rectangleView.setViewFromState(AcuantBaseCameraFragment.CameraState.MrzTrying)
                        rectangleView.setAndDrawPoints(points)
                    } else {
                        allowCapture = true
                        setTextFromState(this, AcuantBaseCameraFragment.CameraState.MrzReposition, instructionView, imageView)
                        rectangleView.setViewFromState(AcuantBaseCameraFragment.CameraState.MrzReposition)
                        rectangleView.setAndDrawPoints(points)
                    }
                } else if(!capturing) {
                    resetCapture()
                    setTextFromState(this, AcuantBaseCameraFragment.CameraState.MrzNone, instructionView, imageView)
                    rectangleView.setViewFromState(AcuantBaseCameraFragment.CameraState.MrzNone)
                    rectangleView.setAndDrawPoints(null)
                }
            } else if(!capturing) {
                resetCapture()
                setTextFromState(this, AcuantBaseCameraFragment.CameraState.MrzNone, instructionView, imageView)
                rectangleView.setViewFromState(AcuantBaseCameraFragment.CameraState.MrzNone)
                rectangleView.setAndDrawPoints(null)
            }

            oldPoints = points
        }
    }

    private fun resetCapture() {
        tries = 0
        allowCapture = false
    }

    override fun onOcrDetected(textBlock: String?){
        if (textBlock != null && allowCapture) {
            val result = mrzParser.parseMrz(textBlock)
            if (result != null) {
                if (result.checkSumResult1 && result.checkSumResult2 && result.checkSumResult3 && result.checkSumResult4 && result.checkSumResult5) {
                    capturing = true
                    setTextFromState(this, AcuantBaseCameraFragment.CameraState.MrzCapturing, instructionView, imageView)
                    rectangleView.setViewFromState(AcuantBaseCameraFragment.CameraState.MrzCapturing)
                    if (mrzResult == null || (mrzResult?.country == "" && result.country != "")) {
                        mrzResult = result
                    }
                    handler.postDelayed({
                        handler.removeCallbacksAndMessages(null)
                        val data = Intent()
                        data.putExtra(ACUANT_EXTRA_MRZ_RESULT, mrzResult)
                        setResult(AcuantCameraActivity.RESULT_SUCCESS_CODE, data)
                        finish()
                    }, 750)
                }
            }
            ++tries
        }
    }

    private fun setOptions(options : AcuantCameraOptions?) {
        if(options != null) {
            rectangleView.allowBox = options.allowBox
            rectangleView.bracketLengthInHorizontal = options.bracketLengthInHorizontal
            rectangleView.bracketLengthInVertical = options.bracketLengthInVertical
            rectangleView.defaultBracketMarginHeight = options.defaultBracketMarginHeight
            rectangleView.defaultBracketMarginWidth = options.defaultBracketMarginWidth
            rectangleView.paintColorCapturing = options.colorCapturing
            rectangleView.paintColorHold = options.colorHold
            rectangleView.paintColorBracketAlign = options.colorBracketAlign
            rectangleView.paintColorBracketCapturing = options.colorBracketCapturing
            rectangleView.paintColorBracketCloser = options.colorBracketCloser
            rectangleView.paintColorBracketHold = options.colorBracketHold
            rectangleView.cardRatio = options.cardRatio
        }
    }

    /**
     * Initializes the UI and creates the detector pipeline.
     */
    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        waitTime = intent.getIntExtra("WAIT", 2)
        autoCapture = intent.getBooleanExtra(ACUANT_EXTRA_IS_AUTO_CAPTURE, true)


        val options = intent?.getSerializableExtra(ACUANT_EXTRA_CAMERA_OPTIONS) as AcuantCameraOptions?
                ?: AcuantCameraOptions
                        .MrzCameraOptionsBuilder()
                        .build()

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        supportActionBar?.title = ""
        supportActionBar?.hide()
        val parent = RelativeLayout(this)
        parent.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        parent.keepScreenOn = true

        setContentView(parent)
        mPreview = DocumentCameraSourcePreview(this, null)
        parent.addView(mPreview)

        capturingTextDrawable = this.getDrawable(R.drawable.camera_text_config_capturing)
        defaultTextDrawable = this.getDrawable(R.drawable.camera_text_config_default)

        val vfvp = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT)
        rectangleView = MrzRectangleView(this, null)
        rectangleView.layoutParams = vfvp
        parent.addView(rectangleView)

        setOptions(options)

        // UI Customization
        tvlp = RelativeLayout.LayoutParams(resources.getDimension(R.dimen.cam_error_width).toInt(), resources.getDimension(R.dimen.cam_mrz_height).toInt())
        tvlp.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
        instructionView = TextView(this)

        instructionView.setPadding(60, 15, 60, 15)
        instructionView.gravity = Gravity.CENTER
        instructionView.rotation = 90.0f
        instructionView.layoutParams = tvlp
        instructionView.typeface = Typeface.MONOSPACE
        instructionView.id = R.id.acu_display_text

        val tvlp2 = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT)
        tvlp2.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
        imageView = ImageView(this)
        imageView.rotation = 90.0f
        imageView.setImageResource(R.drawable.camera_overlay)
        imageView.layoutParams = tvlp2
        imageView.id = R.id.acu_help_image

        setTextFromState(this, AcuantBaseCameraFragment.CameraState.Align, instructionView, imageView)
        parent.addView(instructionView, tvlp)
        parent.addView(imageView, tvlp2)

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        val rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource(autoFocus = true, useFlash = false)
        } else {
            requestCameraPermission()
        }

        mOrientationEventListener = object : OrientationEventListener(this.applicationContext) {
            var lastOrientation = 0
            override fun onOrientationChanged(orientation: Int) {
                if (orientation < 0) {
                    return  // Flip screen, Not take account
                }
                val curOrientation: Int = when {
                    orientation <= 45 -> ORIENTATION_PORTRAIT
                    orientation <= 135 -> ORIENTATION_LANDSCAPE_REVERSE
                    orientation <= 225 -> ORIENTATION_PORTRAIT_REVERSE
                    orientation <= 315 -> ORIENTATION_LANDSCAPE
                    else -> ORIENTATION_PORTRAIT
                }
                if (curOrientation != lastOrientation) {
                    onChanged(lastOrientation, curOrientation)
                    lastOrientation = curOrientation
                }
            }
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

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM)
            return
        }

        val thisActivity = this

        View.OnClickListener {
            ActivityCompat.requestPermissions(thisActivity, permissions, RC_HANDLE_CAMERA_PERM)
        }

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
    private fun createCameraSource(autoFocus: Boolean, useFlash: Boolean) {
        // Creates and starts the camera.  Note that this uses a higher resolution in comparison
        // to other detection examples to enable the barcode detector to detect small barcodes
        // at long distances.
        documentDetector = createDocumentDetector()
        var builder: DocumentCameraSource.Builder = DocumentCameraSource.Builder(applicationContext, documentDetector)
                .setFacing(DocumentCameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(1600, 1200)
                .setRequestedFps(60.0f)

        // make sure that auto focus is an available option
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            builder = builder.setFocusMode(
                    if (autoFocus) Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE else Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)
        }

        documentCameraSource = builder
                .setFlashMode(if (useFlash) Camera.Parameters.FLASH_MODE_TORCH else Camera.Parameters.FLASH_MODE_OFF)
                .build()
    }

    private fun createDocumentDetector(): DocumentDetector {
        documentProcessor = LiveDocumentProcessor()
        documentProcessor.setOcrDetector(AcuantOcrDetector(this, this))
        return documentProcessor.getDetector(applicationContext)

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
        handler.removeCallbacksAndMessages(null)
        mOrientationEventListener.disable()
    }

    /**
     * Releases the resources associated with the camera source, the associated detectors, and the
     * rest of the processing pipeline.
     */
    override fun onDestroy() {
        super.onDestroy()
        documentProcessor.stop()
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
            createCameraSource(autoFocus = true, useFlash = false)
            return
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.size +
                " Result code = " + if (grantResults.isNotEmpty()) grantResults[0] else "(empty)")

        DialogInterface.OnClickListener { _, _ -> finish() }

    }

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    @Throws(SecurityException::class)
    private fun startCameraSource() {
        // check that the device has play services available.
        val code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                applicationContext)
        if (code != ConnectionResult.SUCCESS) {
            val dlg = GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS)
            dlg.show()
        }

        if (documentCameraSource != null) {
            try {
                mPreview!!.start(documentCameraSource)
            } catch (e: IOException) {
                Log.e(TAG, "Unable to start camera source.", e)
                documentCameraSource!!.release()
                documentCameraSource = null
            }

        }
    }
    override fun onBackPressed() {
        this@DocumentCaptureActivity.finish()
    }

    private fun onChanged(lastOrientation: Int, curOrientation: Int) {

        //TODO fix side of help text

        runOnUiThread {
            if (curOrientation == ORIENTATION_LANDSCAPE_REVERSE) {
                rotateView(instructionView, 0f, 270f)
                rotateView(imageView, 0f, 270f)
            } else if (curOrientation == ORIENTATION_LANDSCAPE) {
                rotateView(instructionView, 360f, 90f)
                rotateView(imageView, 360f, 90f)
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
        Thread(Runnable {
            val mgr = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            mgr.setStreamMute(AudioManager.STREAM_SYSTEM, false)
            this@DocumentCaptureActivity.runOnUiThread {
                val result = Intent()
                val file = File(this.getExternalFilesDir(null),  "${UUID.randomUUID()}.jpg")
                saveFile(file, data)
                result.putExtra(ACUANT_EXTRA_IMAGE_URL, file.absolutePath)
                result.putExtra(ACUANT_EXTRA_PDF417_BARCODE, this.capturedbarcodeString)
                setResult(AcuantCameraActivity.RESULT_SUCCESS_CODE, result)
                finish()
            }
        }).start()

    }

    private fun saveFile(file: File, data: ByteArray){
        var output: FileOutputStream? = null
        try {
            output = FileOutputStream(file).apply {write(data)}
        } catch (e: IOException) {
        } finally {
            output?.let {
                try {
                    it.close()
                } catch (e: IOException) {
                }
            }
        }
    }

    override fun onShutter() {

        Log.d("onShutter", "onShutter")

    }

    companion object {
        private const val TAG = "Barcode-reader"

        // intent request code to handle updating play services if needed.
        private const val RC_HANDLE_GMS = 9001

        const val ACUANT_EXTRA_IMAGE_URL = "img-url"
        const val ACUANT_EXTRA_PDF417_BARCODE = "barcode"

        // permission request codes need to be < 256
        private const val RC_HANDLE_CAMERA_PERM = 2
        private const val ORIENTATION_PORTRAIT_REVERSE = 4
        private const val ORIENTATION_LANDSCAPE_REVERSE = 3
    }
}
