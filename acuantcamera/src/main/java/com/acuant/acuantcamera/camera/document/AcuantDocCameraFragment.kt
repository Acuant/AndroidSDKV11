package com.acuant.acuantcamera.camera.document

import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.*
import android.support.v4.app.ActivityCompat
import android.util.Size
import android.util.SparseIntArray
import android.view.*
import com.acuant.acuantcamera.R
import com.acuant.acuantcamera.camera.AcuantBaseCameraFragment
import com.acuant.acuantcamera.camera.AcuantCameraOptions
import com.acuant.acuantcamera.constant.*
import com.acuant.acuantcamera.detector.barcode.AcuantBarcodeDetector
import com.acuant.acuantcamera.detector.barcode.AcuantBarcodeDetectorHandler
import com.acuant.acuantcamera.detector.document.AcuantDocumentDetectorHandler
import com.acuant.acuantcamera.detector.document.AcuantDocumentDetector
import com.acuant.acuantcamera.overlay.DocRectangleView
import kotlin.math.*

class AcuantDocCameraFragment : AcuantBaseCameraFragment(),
        ActivityCompat.OnRequestPermissionsResultCallback, AcuantDocumentDetectorHandler, AcuantBarcodeDetectorHandler {

    private var holdTextDrawable: Drawable? = null

    //private variables
    private var currentDigit: Int = digitsToShow
    private var lastTime: Long = System.currentTimeMillis()
    private var greenTransparent: Int = 0
    private var firstThreeTimings: Array<Long> = arrayOf(-1, -1, -1)
    private var hasFinishedTest = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        options = arguments?.getSerializable(ACUANT_EXTRA_CAMERA_OPTIONS) as AcuantCameraOptions? ?: AcuantCameraOptions.DocumentCameraOptionsBuilder().setAutoCapture(isAutoCapture).setAllowBox(isBorderEnabled).build()
        if(options != null) {
            isAutoCapture = options!!.autoCapture
            isBorderEnabled = options!!.allowBox
        }

        detectors = if (options?.useGMS == false) {
            listOf(AcuantDocumentDetector(this))
        } else {
            listOf(AcuantDocumentDetector(this), AcuantBarcodeDetector(this.activity!!.applicationContext, this))
        }

        capturingTextDrawable = activity!!.getDrawable(R.drawable.camera_text_config_capturing)
        defaultTextDrawable = activity!!.getDrawable(R.drawable.camera_text_config_default)
        holdTextDrawable = activity!!.getDrawable(R.drawable.camera_text_config_hold)
    }

    private fun scalePoints(points: Array<Point>) : Array<Point> {
        val scaledPoints = points.copyOf()
        val scaledPointY = textureView.height.toFloat() / previewSize.width.toFloat()
        val scaledPointX = textureView.width.toFloat() / previewSize.height.toFloat()
        rectangleView.setWidth(textureView.width.toFloat())

        points.apply {
            this.forEach {
                it.x = (it.x * scaledPointY).toInt()
                it.y = (it.y * scaledPointX).toInt()
                it.y -= pointYOffset
                it.x += pointXOffset
            }
        }

        return scaledPoints
    }

    private fun drawBorder(points: Array<Point>?){
        if(points != null) {
            rectangleView.setAndDrawPoints(points)
        }
        else{
            rectangleView.setAndDrawPoints(null)
        }
    }

    /**
     * Callback from AcuantDocumentDetector's Detect()
     */
    override fun onDetected(croppedImage: com.acuant.acuantcommon.model.Image?, cropDuration: Long) {
        activity?.runOnUiThread {

            if (!hasFinishedTest) {
                for (i in firstThreeTimings.indices) {
                    if (firstThreeTimings[i] == (-1).toLong()) {
                        firstThreeTimings[i] = cropDuration
                        break
                    }
                }
                if (!firstThreeTimings.contains((-1).toLong())) {
                    hasFinishedTest = true
                    if (firstThreeTimings.min() ?: (TOO_SLOW_FOR_AUTO_THRESHOLD + 10) > TOO_SLOW_FOR_AUTO_THRESHOLD) {
                        isAutoCapture = false
                        setTapToCapture()
                    }
                }
            }

            if (!hasFinishedTest) {
                rectangleView.setViewFromState(CameraState.Align)
                setTextFromState(CameraState.Align)
                detectors.forEach {
                    if (it is AcuantDocumentDetector) {
                        it.isProcessing = false
                    }
                }
            }

            if (hasFinishedTest && isAutoCapture) {
                var detectedPoints = croppedImage?.points

                if (detectedPoints != null && croppedImage?.points != null) {
                    detectedPoints = DocRectangleView.fixPoints(scalePoints(croppedImage.points))
                }

                when {
                    croppedImage == null || croppedImage.dpi < MINIMUM_DPI -> {
                        unlockFocus()
                        rectangleView.setViewFromState(CameraState.Align)
                        setTextFromState(CameraState.Align)
                        resetTimer()
                    }
                    !isDocumentInFrame(detectedPoints) -> {
                        unlockFocus()
                        rectangleView.setViewFromState(CameraState.NotInFrame)
                        setTextFromState(CameraState.NotInFrame)
                        resetTimer()
                    }
                    !isAcceptableDistance(detectedPoints, Size(textureView.width, textureView.height)) -> {
                        unlockFocus()
                        rectangleView.setViewFromState(CameraState.MoveCloser)
                        setTextFromState(CameraState.MoveCloser)
                        resetTimer()
                    }
                    !croppedImage.isCorrectAspectRatio -> {
                        unlockFocus()
                        rectangleView.setViewFromState(CameraState.Align)
                        setTextFromState(CameraState.Align)
                        resetTimer()
                    }
                    else -> {
                        if (System.currentTimeMillis() - lastTime > (digitsToShow - currentDigit + 2) * timeInMsPerDigit)
                            --currentDigit

                        var dist = 0
                        if (oldPoints != null && oldPoints!!.size == 4 && detectedPoints != null && detectedPoints.size == 4) {
                            for (i in 0..3) {
                                dist += sqrt(((oldPoints!![i].x - detectedPoints[i].x).toDouble().pow(2) + (oldPoints!![i].y - detectedPoints[i].y).toDouble().pow(2))).toInt()
                            }
                        }

                        when {
                            dist > TOO_MUCH_MOVEMENT -> {
                                rectangleView.setViewFromState(CameraState.Steady)
                                setTextFromState(CameraState.Steady)
                                resetTimer()
                            }
                            System.currentTimeMillis() - lastTime < digitsToShow * timeInMsPerDigit -> {
                                rectangleView.setViewFromState(CameraState.Hold)
                                setTextFromState(CameraState.Hold)
                            }
                            else -> {
                                this.isCapturing = true
                                rectangleView.setViewFromState(CameraState.Capturing)
                                setTextFromState(CameraState.Capturing)
                                lockFocus()
                            }
                        }
                    }
                }
                oldPoints = detectedPoints
                drawBorder(detectedPoints)

                detectors.forEach {
                    if (it is AcuantDocumentDetector) {
                        it.isProcessing = false
                    }
                }
            }
        }
    }

    override fun setTextFromState(state: CameraState) {

        textView.visibility = View.VISIBLE
        textView.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT

        when(state) {
            CameraState.MoveCloser -> {
                textView.background = defaultTextDrawable
                textView.layoutParams.width = context?.resources?.getDimension(R.dimen.cam_info_width)?.toInt() ?: 300
                textView.textSize = context?.resources?.getDimension(R.dimen.cam_doc_font) ?: 24f
                textView.text = getString(R.string.acuant_camera_move_closer)
                textView.setTextColor(Color.WHITE)
            }
            CameraState.NotInFrame -> {
                textView.background = defaultTextDrawable
                textView.layoutParams.width = context?.resources?.getDimension(R.dimen.cam_info_width)?.toInt() ?: 300
                textView.textSize = context?.resources?.getDimension(R.dimen.cam_doc_font) ?: 24f
                textView.text = getString(R.string.acuant_camera_not_in_frame)
                textView.setTextColor(Color.WHITE)
            }
            CameraState.Hold -> {
                textView.background = holdTextDrawable
                textView.layoutParams.width = context?.resources?.getDimension(R.dimen.cam_info_width)?.toInt() ?: 300
                textView.textSize = context?.resources?.getDimension(R.dimen.cam_doc_font_big) ?: 48f
                textView.text = resources.getQuantityString(R.plurals.acuant_camera_timer, currentDigit, currentDigit)
                textView.setTextColor(Color.RED)
            }
            CameraState.Steady -> {
                textView.background = defaultTextDrawable
                textView.layoutParams.width = context?.resources?.getDimension(R.dimen.cam_info_width)?.toInt() ?: 300
                textView.textSize = context?.resources?.getDimension(R.dimen.cam_doc_font) ?: 24f
                textView.text = getString(R.string.acuant_camera_hold_steady)
                textView.setTextColor(Color.WHITE)
            }
            CameraState.Capturing -> {
                textView.background = capturingTextDrawable
                textView.layoutParams.width = context?.resources?.getDimension(R.dimen.cam_info_width)?.toInt() ?: 300
                textView.textSize = context?.resources?.getDimension(R.dimen.cam_doc_font_big) ?: 48f
                textView.text = resources.getQuantityString(R.plurals.acuant_camera_timer, currentDigit, currentDigit)
                textView.setTextColor(Color.RED)
            }
            else -> {//align
                textView.background = defaultTextDrawable
                textView.layoutParams.width = context?.resources?.getDimension(R.dimen.cam_info_width)?.toInt() ?: 300
                textView.textSize = context?.resources?.getDimension(R.dimen.cam_doc_font) ?: 24f
                textView.text = getString(R.string.acuant_camera_align)
                textView.setTextColor(Color.WHITE)
            }
        }
    }

    private fun resetTimer() {
        lastTime = System.currentTimeMillis()
        currentDigit = digitsToShow
    }

    override fun onBarcodeDetected(barcode: String){
        this.barCodeString = barcode
        detectors.forEach {
            if (it is AcuantBarcodeDetector) {
                it.isProcessing = false
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_camera2_basic, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        textureView = view.findViewById(R.id.texture)
        rectangleView = view.findViewById(R.id.acu_doc_rectangle) as DocRectangleView
        rectangleView.visibility = View.VISIBLE

        greenTransparent = getColorWithAlpha(Color.GREEN, .50f)

        super.onViewCreated(view, savedInstanceState)
    }

    @Suppress("SameParameterValue")
    private fun getColorWithAlpha(color: Int, ratio: Float): Int {
        return Color.argb((Color.alpha(color) * ratio).roundToInt(), Color.red(color), Color.green(color), Color.blue(color))
    }

    override fun setTapToCapture(){
        if(!isAutoCapture){
            setTextFromState(CameraState.Align)
            textView.text = getString(R.string.acuant_camera_align_and_tap)
            textView.layoutParams.width = context?.resources?.getDimension(R.dimen.cam_info_width)?.toInt() ?: 300
            textureView.setOnClickListener{
                activity?.runOnUiThread{
                    this.isCapturing = true
                    textView.setBackgroundColor(greenTransparent)
                    textView.text = getString(R.string.acuant_camera_capturing)
                    lockFocus()
                }
            }
        }
    }

    companion object {

        internal const val TOO_SLOW_FOR_AUTO_THRESHOLD: Long = 130

        fun isAcceptableDistance(points: Array<Point>?, screenSize: Size): Boolean {
            if (points != null) {
                val shortSide = min(distance(points[0], points[1]), distance(points[0], points[3]))
                val largeSide = max(distance(points[0], points[1]), distance(points[0], points[3]))
                val screenShortSide = min(screenSize.width, screenSize.height).toFloat()
                val screenLargeSide = max(screenSize.width, screenSize.height).toFloat()

                if (shortSide > 0.75 * screenShortSide || largeSide > 0.75 * screenLargeSide) {
                    return true
                }
            }
            return false
        }

        private fun distance(pointA: Point, pointB: Point): Float {
            return sqrt( (pointA.x - pointB.x).toFloat().pow(2) + (pointA.y - pointB.y).toFloat().pow(2))
        }

        /**
         * Conversion from screen rotation to JPEG orientation.
         */
        private val ORIENTATIONS = SparseIntArray()

        init {
            ORIENTATIONS.append(Surface.ROTATION_0, 90)
            ORIENTATIONS.append(Surface.ROTATION_90, 0)

            ORIENTATIONS.append(Surface.ROTATION_180, 270)
            ORIENTATIONS.append(Surface.ROTATION_270, 180)
        }

        /**
         * How much total x/y movement between frames is too much
         */
        internal const val TOO_MUCH_MOVEMENT = 350

        @JvmStatic fun newInstance(): AcuantDocCameraFragment = AcuantDocCameraFragment()
    }
}