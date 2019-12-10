package com.acuant.acuantcamera.camera.cameraone

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Point
import android.hardware.Camera
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.util.Size
import android.view.Gravity
import android.view.MotionEvent
import android.view.OrientationEventListener
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView

import com.acuant.acuantcamera.camera.AcuantCameraFragment
import com.acuant.acuantcamera.overlay.RectangleView
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

import java.io.IOException

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.graphics.drawable.Drawable
import com.acuant.acuantcamera.R
import com.acuant.acuantcamera.camera.AcuantCameraActivity
import com.acuant.acuantcamera.constant.ACUANT_EXTRA_IMAGE_URL
import com.acuant.acuantcamera.constant.ACUANT_EXTRA_IS_AUTO_CAPTURE
import com.acuant.acuantcamera.constant.ACUANT_EXTRA_PDF417_BARCODE
import com.acuant.acuantcamera.helper.ImageSaver
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Activity for the multi-tracker app.  This app detects barcodes and displays the value with the
 * rear facing camera. During detection overlay graphics are drawn to indicate the position,
 * size, and ID of each barcode.
 */
class DocumentCaptureActivity : AppCompatActivity(), DocumentCameraSource.PictureCallback, DocumentCameraSource.ShutterCallback {
    private val ORIENTATION_PORTRAIT_REVERSE = 4
    private val ORIENTATION_LANDSCAPE_REVERSE = 3
    private var documentCameraSource: DocumentCameraSource? = null
    private var mPreview: DocumentCameraSourcePreview? = null
    private var capturing = false
    private var waitTime = 2
    private var autoCapture = false
    private lateinit var documentProcessor: LiveDocumentProcessor
    private var documentDetector: DocumentDetector? = null

    private lateinit var instructionView: TextView

    private var capturedbarcodeString: String? = null

    private lateinit var rectangleView: RectangleView

    private var capturingTextDrawable: Drawable? = null
    private var defaultTextDrawable: Drawable? = null
    private var holdTextDrawable: Drawable? = null
    private var currentDigit: Int = 2
    private var lastTime: Long = System.currentTimeMillis()
    private var timeInMsPerDigit: Int = 800
    private var oldPoints : Array<Point>? = null
    private var digitsToShow: Int = 2
    /**
     * Initializes the UI and creates the detector pipeline.
     */
    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        waitTime = intent.getIntExtra("WAIT", 2)
        autoCapture = intent.getBooleanExtra(ACUANT_EXTRA_IS_AUTO_CAPTURE, true)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        supportActionBar!!.title = ""
        supportActionBar!!.hide()
        val parent = RelativeLayout(this)
        parent.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        parent.keepScreenOn = true

        setContentView(parent)
        mPreview = DocumentCameraSourcePreview(this, null)
        parent.addView(mPreview)

        capturingTextDrawable = this.getDrawable(R.drawable.camera_text_config_capturing)
        defaultTextDrawable = this.getDrawable(R.drawable.camera_text_config_default)
        holdTextDrawable = this.getDrawable(R.drawable.camera_text_config_hold)

        val vfvp = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT)
        rectangleView = RectangleView(this, null)
        rectangleView.layoutParams = vfvp
        parent.addView(rectangleView)

        // UI Customization
        val tvlp = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        tvlp.addRule(RelativeLayout.CENTER_IN_PARENT)
        instructionView = TextView(this)

        instructionView.setPadding(60, 15, 60, 15)
        instructionView.gravity = Gravity.CENTER
        instructionView.rotation = 90.0f
        instructionView.layoutParams = tvlp
        setTextFromState(AcuantCameraFragment.CameraState.Align)
        parent.addView(instructionView, tvlp)

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        val rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource(true, false)
        } else {
            requestCameraPermission()
        }

        if(!autoCapture){
            instructionView.text = getString(R.string.acuant_camera_align_and_tap)
            parent.setOnClickListener{
                instructionView.setBackgroundColor(getColorWithAlpha(Color.GREEN, .50f))
                instructionView.text = getString(R.string.acuant_camera_capturing)
                capturing = true
                lockFocus()
            }

        }

        mOrientationEventListener = object : OrientationEventListener(this.applicationContext) {
            var lastOrientation = 0
            override fun onOrientationChanged(orientation: Int) {
                if (orientation < 0) {
                    return  // Flip screen, Not take account
                }
                val curOrientation: Int
                if (orientation <= 45) {
                    curOrientation = ORIENTATION_PORTRAIT
                } else if (orientation <= 135) {
                    curOrientation = ORIENTATION_LANDSCAPE_REVERSE
                } else if (orientation <= 225) {
                    curOrientation = ORIENTATION_PORTRAIT_REVERSE
                } else if (orientation <= 315) {
                    curOrientation = ORIENTATION_LANDSCAPE
                } else {
                    curOrientation = ORIENTATION_PORTRAIT
                }
                if (curOrientation != lastOrientation) {
                    onChanged(lastOrientation, curOrientation)
                    lastOrientation = curOrientation
                }
            }
        }
    }

    private fun lockFocus(){
        documentCameraSource?.autoFocus {
            if(it){
                capture()
            }
        }
    }

    private fun capture(){
        documentCameraSource?.takePicture(this@DocumentCaptureActivity, this@DocumentCaptureActivity)
    }

    @Suppress("SameParameterValue")
    private fun getColorWithAlpha(color: Int, ratio: Float): Int {
        return Color.argb((Color.alpha(color) * ratio).roundToInt(), Color.red(color), Color.green(color), Color.blue(color))
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

        val listener = View.OnClickListener {
            ActivityCompat.requestPermissions(thisActivity, permissions,
                    RC_HANDLE_CAMERA_PERM)
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
                    if (autoFocus) Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE else "")
        }

        documentCameraSource = builder
                .setFlashMode(if (useFlash) Camera.Parameters.FLASH_MODE_TORCH else "")
                .build()
    }

    private fun createDocumentDetector(): DocumentDetector {
        documentProcessor = LiveDocumentProcessor()
        return documentProcessor.getBarcodeDetector(applicationContext){
            if(it.feedback == DocumentFeedback.Barcode){
                this.capturedbarcodeString = it.barcode
            }
            else{
                runOnUiThread {
                    val points = it.point
                    val frameSize = it.frameSize!!
                    val feedback = it.feedback
                    rectangleView.setWidth(mPreview!!.mSurfaceView.width.toFloat())
                    if (points != null && points.size == 4) {

                        val scaleX = mPreview!!.mSurfaceView.width / frameSize.height.toFloat()
                        val scaleY = mPreview!!.mSurfaceView.height / frameSize.width.toFloat()

                        points.apply {
                            this.forEach { p ->
                                p.x = (p.x * scaleY).toInt()
                                p.y = (p.y * scaleX).toInt()
                            }
                        }

                        fixPoints(points)
                    }

                    if (!capturing && autoCapture) {
                        when (feedback) {
                            DocumentFeedback.NoDocument -> {
                                rectangleView.setColorByState(AcuantCameraFragment.CameraState.Align)
                                setTextFromState(AcuantCameraFragment.CameraState.Align)
                                resetTimer()
                            }
                            DocumentFeedback.SmallDocument -> {
                                rectangleView.setColorByState(AcuantCameraFragment.CameraState.MoveCloser)
                                setTextFromState(AcuantCameraFragment.CameraState.MoveCloser)
                                resetTimer()
                            }
                            DocumentFeedback.BadDocument -> {
                                rectangleView.setColorByState(AcuantCameraFragment.CameraState.Align)
                                setTextFromState(AcuantCameraFragment.CameraState.MoveCloser)
                                resetTimer()
                            }
                            else -> {

                                if(System.currentTimeMillis() - lastTime > (digitsToShow - currentDigit + 2) * timeInMsPerDigit)
                                    --currentDigit

                                var dist = 0
                                if(oldPoints != null && oldPoints!!.size == 4 && points != null && points.size == 4) {
                                    for (i in 0..3) {
                                        dist += sqrt( ((oldPoints!![i].x-points[i].x).toDouble().pow(2) + (oldPoints!![i].y - points[i].y).toDouble().pow(2) )).toInt()
                                    }
                                }

                                when {
                                    dist > 350 -> {
                                        rectangleView.setColorByState(AcuantCameraFragment.CameraState.Steady)
                                        setTextFromState(AcuantCameraFragment.CameraState.Steady)
                                        resetTimer()

                                    }
                                    System.currentTimeMillis() - lastTime < digitsToShow * timeInMsPerDigit -> {
                                        rectangleView.setColorByState(AcuantCameraFragment.CameraState.Hold)
                                        setTextFromState(AcuantCameraFragment.CameraState.Hold)
                                    }
                                    else -> {
                                        rectangleView.setColorByState(AcuantCameraFragment.CameraState.Capturing)
                                        setTextFromState(AcuantCameraFragment.CameraState.Capturing)
                                        capturing = true
                                        lockFocus()
                                    }
                                }
                            }
                        }
                        oldPoints = points
                        rectangleView.setAndDrawPoints(points)
                    }

                }
            }

        }

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

        if (grantResults.size != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source")
            createCameraSource(true, false)
            return
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.size +
                " Result code = " + if (grantResults.size > 0) grantResults[0] else "(empty)")

        val listener = DialogInterface.OnClickListener { dialog, id -> finish() }

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

    private fun setTextFromState(state: AcuantCameraFragment.CameraState) {
        when(state) {
            AcuantCameraFragment.CameraState.MoveCloser -> {
                instructionView.background = defaultTextDrawable
                instructionView.text = getString(R.string.acuant_camera_move_closer)
                instructionView.setTextColor(Color.WHITE)
                instructionView.textSize = 24f
            }
            AcuantCameraFragment.CameraState.Hold -> {
                instructionView.background = holdTextDrawable
                instructionView.text = resources.getQuantityString(R.plurals.acuant_camera_timer, currentDigit, currentDigit)
                instructionView.setTextColor(Color.RED)
                instructionView.textSize = 48f
            }
            AcuantCameraFragment.CameraState.Steady -> {
                instructionView.background = defaultTextDrawable
                instructionView.text = getString(R.string.acuant_camera_hold_steady)
                instructionView.setTextColor(Color.WHITE)
                instructionView.textSize = 24f
            }
            AcuantCameraFragment.CameraState.Capturing -> {
                instructionView.background = capturingTextDrawable
                instructionView.text = resources.getQuantityString(R.plurals.acuant_camera_timer, currentDigit, currentDigit)
                instructionView.setTextColor(Color.RED)
                instructionView.textSize = 48f
            }
            else -> {//align
                instructionView.background = defaultTextDrawable
                instructionView.text = getString(R.string.acuant_camera_align)
                instructionView.setTextColor(Color.WHITE)
                instructionView.textSize = 24f
            }
        }
    }

    private fun resetTimer() {
        lastTime = System.currentTimeMillis()
        currentDigit = digitsToShow
    }

    private fun fixPoints(points: Array<Point>?): Array<Point>? {
        if (points != null && points.size == 4) {
            if (points[0].y > points[2].y && points[0].x < points[2].x) {
                //rotate 2
                var tmp = points[0]
                points[0] = points[2]
                points[2] = tmp

                tmp = points[1]
                points[1] = points[3]
                points[3] = tmp

            } else if (points[0].y > points[2].y && points[0].x > points[2].x) {
                //rotate 3
                val tmp = points[0]
                points[0] = points[1]
                points[1] = points[2]
                points[2] = points[3]
                points[3] = tmp

            } else if (points[0].y < points[2].y && points[0].x < points[2].x) {
                //rotate 1
                val tmp = points[0]
                points[0] = points[3]
                points[3] = points[2]
                points[2] = points[1]
                points[1] = tmp

            }
        }
        return points
    }

    protected fun onChanged(lastOrientation: Int, curOrientation: Int) {

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
        private val TAG = "Barcode-reader"

        // intent request code to handle updating play services if needed.
        private val RC_HANDLE_GMS = 9001

        // permission request codes need to be < 256
        private val RC_HANDLE_CAMERA_PERM = 2
    }
}
