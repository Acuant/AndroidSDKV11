package com.acuant.acuantcamera.camera.document.cameraone

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Point
import android.hardware.Camera
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.Gravity
import android.view.OrientationEventListener
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView

import java.io.IOException

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import com.acuant.acuantcamera.R
import com.acuant.acuantcamera.camera.AcuantCameraActivity
import com.acuant.acuantcamera.camera.AcuantCameraOptions
import com.acuant.acuantcamera.constant.*
import java.io.ByteArrayOutputStream
import com.acuant.acuantcamera.overlay.DocRectangleView
import com.acuant.acuantcamera.camera.AcuantBaseCameraFragment.CameraState
import com.acuant.acuantcamera.detector.ImageSaver
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
    private var autoCapture = false
    private lateinit var documentProcessor: LiveDocumentProcessor
    private var documentDetector: DocumentDetector? = null

    private lateinit var instructionView: TextView

    private var capturedbarcodeString: String? = null

    private lateinit var rectangleView: DocRectangleView

    private var capturingTextDrawable: Drawable? = null
    private var defaultTextDrawable: Drawable? = null
    private var holdTextDrawable: Drawable? = null
    private var currentDigit: Int = 2
    private var lastTime: Long = System.currentTimeMillis()
    private var timeInMsPerDigit: Int = 800
    private var oldPoints: Array<Point>? = null
    private var digitsToShow: Int = 2
    private lateinit var displaySize: Point
    private var lastOrientation = ORIENTATION_LANDSCAPE

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
        rectangleView = DocRectangleView(this, null)
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
        setTextFromState(CameraState.Align)
        parent.addView(instructionView, tvlp)

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        val rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource(autoFocus = true, useFlash = false)
        } else {
            requestCameraPermission()
        }

        setOptions(intent.getSerializableExtra(ACUANT_EXTRA_CAMERA_OPTIONS) as AcuantCameraOptions? ?: AcuantCameraOptions())

        if (!autoCapture) {
            instructionView.text = getString(R.string.acuant_camera_align_and_tap)
            parent.setOnClickListener {
                instructionView.setBackgroundColor(getColorWithAlpha(Color.GREEN, .50f))
                instructionView.text = getString(R.string.acuant_camera_capturing)
                capturing = true
                lockFocus()
            }
        }

        displaySize = Point()
        this.windowManager.defaultDisplay.getSize(displaySize)

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

    private fun lockFocus() {
        documentCameraSource?.autoFocus {
            if (it) {
                capture()
            }
        }
    }

    private fun capture() {
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



    private fun isDocumentInPreviewFrame(points: Array<Point>): Boolean {
        points.apply {
            this.forEach { p ->
                if (p.x < 0 || (mPreview!!.mSurfaceView.width - p.y) < 0 || p.x > displaySize.y || (mPreview!!.mSurfaceView.width - p.y) > displaySize.x) {
                    return false
                }
            }
        }
        return true
    }

    private fun scalePoints(points: Array<Point>, frameSize: Size): Array<Point> {
        val scaledPoints = points.copyOf()
        val scaleX = mPreview!!.mSurfaceView.width / frameSize.height.toFloat()
        val scaleY = mPreview!!.mSurfaceView.height / frameSize.width.toFloat()

        scaledPoints.apply {
            this.forEach { p ->
                p.x = (p.x * scaleY).toInt()
                p.y = (p.y * scaleX).toInt()
            }
        }

        return scaledPoints
    }

    private fun createDocumentDetector(): DocumentDetector {
        documentProcessor = LiveDocumentProcessor()
        return documentProcessor.getBarcodeDetector(applicationContext) {
            if (it.feedback == DocumentFeedback.Barcode) {
                this.capturedbarcodeString = it.barcode
            } else {
                runOnUiThread {
                    var points = it.point
                    val frameSize = it.frameSize!!
                    var feedback = it.feedback
                    rectangleView.setWidth(mPreview!!.mSurfaceView.width.toFloat())
                    if (points != null && points.size == 4) {

                        points = scalePoints(points, frameSize)
                        points = fixPoints(points)

                        if (!isDocumentInPreviewFrame(points)) {
                            feedback = DocumentFeedback.NotInFrame
                        }
                    }


                    if (!capturing && autoCapture) {
                        when (feedback) {
                            DocumentFeedback.NoDocument -> {
                                rectangleView.setViewFromState(CameraState.Align)
                                setTextFromState(CameraState.Align)
                                resetTimer()
                            }
                            DocumentFeedback.NotInFrame -> {
                                rectangleView.setViewFromState(CameraState.NotInFrame)
                                setTextFromState(CameraState.NotInFrame)
                                resetTimer()
                            }
                            DocumentFeedback.SmallDocument -> {
                                rectangleView.setViewFromState(CameraState.MoveCloser)
                                setTextFromState(CameraState.MoveCloser)
                                resetTimer()
                            }
                            DocumentFeedback.BadDocument -> {
                                rectangleView.setViewFromState(CameraState.Align)
                                setTextFromState(CameraState.MoveCloser)
                                resetTimer()
                            }
                            else -> {

                                if (System.currentTimeMillis() - lastTime > (digitsToShow - currentDigit + 2) * timeInMsPerDigit)
                                    --currentDigit

                                var dist = 0
                                if (oldPoints != null && oldPoints!!.size == 4 && points != null && points.size == 4) {
                                    for (i in 0..3) {
                                        dist += sqrt(((oldPoints!![i].x - points[i].x).toDouble().pow(2) + (oldPoints!![i].y - points[i].y).toDouble().pow(2))).toInt()
                                    }
                                }

                                when {
                                    dist > 350 -> {
                                        rectangleView.setViewFromState(CameraState.Steady)
                                        setTextFromState(CameraState.Steady)
                                        resetTimer()

                                    }
                                    System.currentTimeMillis() - lastTime < digitsToShow * timeInMsPerDigit -> {
                                        rectangleView.setViewFromState(CameraState.Hold)
                                        setTextFromState(CameraState.Hold)
                                    }
                                    else -> {
                                        rectangleView.setViewFromState(CameraState.Capturing)
                                        setTextFromState(CameraState.Capturing)
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

    private fun setOptions(options : AcuantCameraOptions) {
        this.timeInMsPerDigit = options.timeInMsPerDigit
        this.digitsToShow = options.digitsToShow
        autoCapture = options.autoCapture
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

    private fun setTextFromState(state: CameraState) {
        when(state) {
            CameraState.MoveCloser -> {
                instructionView.background = defaultTextDrawable
                instructionView.text = getString(R.string.acuant_camera_move_closer)
                instructionView.setTextColor(Color.WHITE)
                instructionView.textSize = 24f
            }
            CameraState.NotInFrame -> {
                instructionView.background = defaultTextDrawable
                instructionView.text = getString(R.string.acuant_camera_not_in_frame)
                instructionView.setTextColor(Color.WHITE)
                instructionView.textSize = 24f
            }
            CameraState.Hold -> {
                instructionView.background = holdTextDrawable
                instructionView.text = resources.getQuantityString(R.plurals.acuant_camera_timer, currentDigit, currentDigit)
                instructionView.setTextColor(Color.RED)
                instructionView.textSize = 48f
            }
            CameraState.Steady -> {
                instructionView.background = defaultTextDrawable
                instructionView.text = getString(R.string.acuant_camera_hold_steady)
                instructionView.setTextColor(Color.WHITE)
                instructionView.textSize = 24f
            }
            CameraState.Capturing -> {
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

    private fun fixPoints(points: Array<Point>): Array<Point> {
        val fixedPoints = points.copyOf()
        if (fixedPoints.size == 4) {
            if (fixedPoints[0].y > fixedPoints[2].y && fixedPoints[0].x < fixedPoints[2].x) {
                //rotate 2
                var tmp = fixedPoints[0]
                fixedPoints[0] = fixedPoints[2]
                fixedPoints[2] = tmp

                tmp = fixedPoints[1]
                fixedPoints[1] = fixedPoints[3]
                fixedPoints[3] = tmp

            } else if (fixedPoints[0].y > fixedPoints[2].y && fixedPoints[0].x > fixedPoints[2].x) {
                //rotate 3
                val tmp = fixedPoints[0]
                fixedPoints[0] = fixedPoints[1]
                fixedPoints[1] = fixedPoints[2]
                fixedPoints[2] = fixedPoints[3]
                fixedPoints[3] = tmp

            } else if (fixedPoints[0].y < fixedPoints[2].y && fixedPoints[0].x < fixedPoints[2].x) {
                //rotate 1
                val tmp = fixedPoints[0]
                fixedPoints[0] = fixedPoints[3]
                fixedPoints[3] = fixedPoints[2]
                fixedPoints[2] = fixedPoints[1]
                fixedPoints[1] = tmp

            }
        }
        return fixedPoints
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
        var updated = data
        val image = BitmapFactory.decodeByteArray(data, 0, data.size)

        if((image.width > image.height && this.lastOrientation == ORIENTATION_LANDSCAPE_REVERSE) ||
                 (image.width < image.height && this.lastOrientation == ORIENTATION_LANDSCAPE)){
            val rotated = ImageSaver.rotateImage(image, 180f)
            val stream = ByteArrayOutputStream()
            rotated.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            rotated.recycle()
            updated = stream.toByteArray()

        }

        val file = File(this.cacheDir, "${UUID.randomUUID()}.jpg")
        saveFile(file, updated)

        this@DocumentCaptureActivity.runOnUiThread {
            val result = Intent()
            result.putExtra(ACUANT_EXTRA_IMAGE_URL, file.absolutePath)
            result.putExtra(ACUANT_EXTRA_PDF417_BARCODE, this.capturedbarcodeString)
            setResult(AcuantCameraActivity.RESULT_SUCCESS_CODE, result)
            finish()
        }
    }

    private fun saveFile(file: File, data: ByteArray) {
        var output: FileOutputStream? = null
        try {
            output = FileOutputStream(file).apply { write(data) }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            output?.let {
                try {
                    it.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onShutter() {

        Log.d("onShutter", "onShutter")

    }

    companion object {
        private const val TAG = "Barcode-reader"

        // permission request codes need to be < 256
        private const val RC_HANDLE_CAMERA_PERM = 2
    }
}
