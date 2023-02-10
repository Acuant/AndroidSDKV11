package com.acuant.acuantcamera.camera.document

import android.graphics.Color
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.camera.core.ImageAnalysis
import com.acuant.acuantcamera.R
import com.acuant.acuantcamera.camera.AcuantBaseCameraFragment
import com.acuant.acuantcamera.camera.AcuantCameraOptions
import com.acuant.acuantcamera.constant.MINIMUM_DPI
import com.acuant.acuantcamera.constant.TARGET_DPI
import com.acuant.acuantcamera.interfaces.IAcuantSavedImage
import com.acuant.acuantcamera.databinding.DocumentFragmentUiBinding
import com.acuant.acuantcamera.detector.DocumentFrameAnalyzer
import com.acuant.acuantcamera.detector.DocumentState
import com.acuant.acuantcamera.helper.PointsUtils
import com.acuant.acuantcamera.overlay.DocRectangleView
import com.acuant.acuantcommon.model.AcuantError
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class DocumentCameraState { Align, NotInFrame, MoveCloser, MoveBack, HoldSteady, CountingDown, Capturing }

class AcuantDocCameraFragment: AcuantBaseCameraFragment() {

    private var rectangleView: DocRectangleView? = null
    private var textView: TextView? = null
    private var cameraUiContainerBinding: DocumentFragmentUiBinding? = null
    private var latestBarcode: String? = null
    private var capturingTextDrawable: Drawable? = null
    private var defaultTextDrawable: Drawable? = null
    private var holdTextDrawable: Drawable? = null
    private var tapToCapture = false
    private var currentDigit: Int = 0
    private var lastTime: Long = System.currentTimeMillis()
    private var greenTransparent: Int = 0
    private var firstThreeTimings: Array<Long> = arrayOf(-1, -1, -1)
    private var hasFinishedTest = false
    private var oldPoints: Array<Point>? = null
    private var initialDpi: Int = 0
    private var hasFoundCorrectBounds = false
    private var instancesOfCorrectDpi = 0
    private lateinit var frameAnalyzer: DocumentFrameAnalyzer

    private fun drawBorder(points: Array<Point>?) {
        activity?.runOnUiThread {
            if (points != null) {
                rectangleView?.setAndDrawPoints(points)
            } else {
                rectangleView?.setAndDrawPoints(null)
            }
        }
    }

    private fun onDocumentDetection(points: Array<Point>?, ratio: Float?, analyzerDPI: Int, docState: DocumentState, cropDuration: Long) {
        if (!capturing && !tapToCapture) {
            activity?.runOnUiThread {

                if (!hasFinishedTest) {
                    rectangleView?.setViewFromState(DocumentCameraState.Align)
                    setTextFromState(DocumentCameraState.Align)
                    resetWorkflow()

                    for (i in firstThreeTimings.indices) {
                        if (firstThreeTimings[i] == (-1).toLong()) {
                            firstThreeTimings[i] = cropDuration
                            break
                        }
                    }

                    if (!firstThreeTimings.contains((-1).toLong())) {
                        hasFinishedTest = true
                        if ((firstThreeTimings.minOrNull() ?: (TOO_SLOW_FOR_AUTO_THRESHOLD + 10)) > TOO_SLOW_FOR_AUTO_THRESHOLD) {
                            setTapToCapture()
                        }
                    }
                }

                if (hasFinishedTest && !tapToCapture) {
                    var detectedPoints = points
                    var realDpi = 0

                    val camContainer = fragmentCameraBinding?.root
                    val analyzerSize = imageAnalyzer?.resolutionInfo?.resolution
                    val previewSize = fragmentCameraBinding?.viewFinder

                    val state = if (detectedPoints != null && detectedPoints.size == 4) {
                        detectedPoints = PointsUtils.fixPoints(PointsUtils.scalePoints(detectedPoints, camContainer, analyzerSize, previewSize, rectangleView))
                        realDpi = PointsUtils.scaleDpi(analyzerDPI, analyzerSize, imageCapture?.resolutionInfo?.resolution)
                        if (previewSize != null) {
                            val insetFromEdges = 0.02f
                            val view = Rect((previewSize.left * (1 + insetFromEdges)).toInt(), (previewSize.top * (1 + insetFromEdges)).toInt(), (previewSize.right * (1 - insetFromEdges)).toInt(), (previewSize.bottom * (1 - insetFromEdges)).toInt())
                            var isContained = true
                            detectedPoints.forEach {
                                if (!view.contains(it.y, it.x)) {
                                    isContained = false
                                }
                            }
                            if (isContained) {
                                docState
                            } else {
                                DocumentState.OutOfBounds
                            }
                        } else {
                            docState
                        }
                    } else {
                        docState
                    }

                    when (state) {
                        DocumentState.NoDocument -> {
                            rectangleView?.setViewFromState(DocumentCameraState.Align)
                            setTextFromState(DocumentCameraState.Align)
                            resetWorkflow()
                        }
                        DocumentState.OutOfBounds -> {
                            rectangleView?.setViewFromState(DocumentCameraState.NotInFrame)
                            setTextFromState(DocumentCameraState.NotInFrame)
                            resetWorkflow()
                        }
                        DocumentState.TooClose -> {
                            rectangleView?.setViewFromState(DocumentCameraState.MoveBack)
                            setTextFromState(DocumentCameraState.MoveBack)
                            resetWorkflow()
                        }
                        DocumentState.TooFar -> {
                            rectangleView?.setViewFromState(DocumentCameraState.MoveCloser)
                            setTextFromState(DocumentCameraState.MoveCloser)
                            resetWorkflow()
                        }
                        else -> { // good document
                            if (System.currentTimeMillis() - lastTime > (currentDigit + 1) * acuantOptions.timeInMsPerDigit)
                                ++currentDigit

                            var dist = 0
                            if (oldPoints != null && oldPoints!!.size == 4 && detectedPoints != null && detectedPoints.size == 4) {
                                for (i in 0..3) {
                                    dist += sqrt(
                                        ((oldPoints!![i].x - detectedPoints[i].x).toDouble()
                                            .pow(2) + (oldPoints!![i].y - detectedPoints[i].y).toDouble()
                                            .pow(2))
                                    ).toInt()
                                }
                            }

                            when {
                                dist > TOO_MUCH_MOVEMENT -> {
                                    rectangleView?.setViewFromState(DocumentCameraState.HoldSteady)
                                    setTextFromState(DocumentCameraState.HoldSteady)
                                    resetWorkflow()
                                }
                                System.currentTimeMillis() - lastTime < acuantOptions.digitsToShow * acuantOptions.timeInMsPerDigit -> {
                                    rectangleView?.setViewFromState(DocumentCameraState.CountingDown)
                                    setTextFromState(DocumentCameraState.CountingDown)
                                }
                                else -> {
                                    val middle = PointsUtils.findMiddleForCamera(points, fragmentCameraBinding?.root?.width, fragmentCameraBinding?.root?.height)
                                    captureImage(object : IAcuantSavedImage {
                                        override fun onSaved(bytes: ByteArray) {
                                            cameraActivityListener.onCameraDone(bytes, latestBarcode)
                                        }

                                        override fun onError(error: AcuantError) {
                                            cameraActivityListener.onError(error)
                                        }

                                    }, middle, captureType = "AUTO")
                                    rectangleView?.setViewFromState(DocumentCameraState.Capturing)
                                    setTextFromState(DocumentCameraState.Capturing)
                                }
                            }
                        }
                    }

                    //this adjusts the too close/too far bounds based on dpi
                    if (!hasFoundCorrectBounds && initialDpi > MINIMUM_DPI && ratio != null) {
                        if (initialDpi >= TARGET_DPI) {
                            if (realDpi < TARGET_DPI) {
                                if (instancesOfCorrectDpi == 0) {
                                    frameAnalyzer.setNewMinDist(ratio - 0.01f) //we used to be too close and have zoomed out enough
                                }
                                ++instancesOfCorrectDpi
                                if (instancesOfCorrectDpi >= 3) {
                                    hasFoundCorrectBounds = true
                                }
                            } else {
                                instancesOfCorrectDpi = 0
                                frameAnalyzer.setNewMaxDist(ratio - 0.3f) //we used to be too close and are still too close
                            }
                        } else {
                            if (realDpi >= TARGET_DPI) {
                                if (instancesOfCorrectDpi == 0) {
                                    frameAnalyzer.setNewMinDist(ratio - 0.01f) //we used to be too far and have zoomed in enough
                                }
                                ++instancesOfCorrectDpi
                                if (instancesOfCorrectDpi >= 3) {
                                    hasFoundCorrectBounds = true
                                }
                            } else {
                                instancesOfCorrectDpi = 0
                                frameAnalyzer.setNewMinDist(ratio + 0.03f) //we used to be too far and are still too far
                            }
                        }
                    }

                    if (initialDpi == 0 && realDpi > MINIMUM_DPI) {
                        initialDpi = realDpi
                    } else if (realDpi < MINIMUM_DPI) { //if real dpi is negligible (document left screen) reset the detected bounds state
                        initialDpi = 0
                        hasFoundCorrectBounds = false
                        instancesOfCorrectDpi = 0
                    }

                    oldPoints = detectedPoints
                    drawBorder(detectedPoints)
                }
            }
        }
    }

    private fun setTextFromState(state: DocumentCameraState) {
        textView?.visibility = View.VISIBLE
        if (!isAdded)
            return

        val textView = this.textView

        if (textView != null) {
            when (state) {
                DocumentCameraState.MoveCloser -> {
                    textView.background = defaultTextDrawable
                    textView.layoutParams?.width =
                        context?.resources?.getDimension(R.dimen.cam_info_width)?.toInt() ?: 300
                    textView.textSize =
                        context?.resources?.getDimension(R.dimen.cam_doc_font) ?: 24f
                    textView.text = getString(R.string.acuant_camera_move_closer)
                    textView.setTextColor(Color.WHITE)
                }
                DocumentCameraState.MoveBack -> {
                    textView.background = defaultTextDrawable
                    textView.layoutParams?.width =
                        context?.resources?.getDimension(R.dimen.cam_info_width)?.toInt() ?: 300
                    textView.textSize =
                        context?.resources?.getDimension(R.dimen.cam_doc_font) ?: 24f
                    textView.text = getString(R.string.acuant_camera_too_close)
                    textView.setTextColor(Color.WHITE)
                }
                DocumentCameraState.NotInFrame -> {
                    textView.background = defaultTextDrawable
                    textView.layoutParams?.width =
                        context?.resources?.getDimension(R.dimen.cam_info_width)?.toInt() ?: 300
                    textView.textSize =
                        context?.resources?.getDimension(R.dimen.cam_doc_font) ?: 24f
                    textView.text = getString(R.string.acuant_camera_out_of_bounds)
                    textView.setTextColor(Color.WHITE)
                }
                DocumentCameraState.CountingDown -> {
                    textView.background = holdTextDrawable
                    textView.layoutParams?.width =
                        context?.resources?.getDimension(R.dimen.cam_info_width)?.toInt() ?: 300
                    textView.textSize =
                        context?.resources?.getDimension(R.dimen.cam_doc_font_big) ?: 48f
                    textView.text = resources.getQuantityString(
                        R.plurals.acuant_camera_timer,
                        acuantOptions.digitsToShow - currentDigit,
                        acuantOptions.digitsToShow - currentDigit
                    )
                    textView.setTextColor(Color.RED)
                }
                DocumentCameraState.HoldSteady -> {
                    textView.background = defaultTextDrawable
                    textView.layoutParams?.width =
                        context?.resources?.getDimension(R.dimen.cam_info_width)?.toInt() ?: 300
                    textView.textSize =
                        context?.resources?.getDimension(R.dimen.cam_doc_font) ?: 24f
                    textView.text = getString(R.string.acuant_camera_hold_steady)
                    textView.setTextColor(Color.WHITE)
                }
                DocumentCameraState.Capturing -> {
                    textView.background = capturingTextDrawable
                    textView.layoutParams?.width =
                        context?.resources?.getDimension(R.dimen.cam_info_width)?.toInt() ?: 300
                    textView.textSize =
                        context?.resources?.getDimension(R.dimen.cam_doc_font_big) ?: 48f
                    textView.text = getString(R.string.acuant_camera_capturing)
                    textView.setTextColor(Color.RED)
                }
                else -> {//align
                    textView.background = defaultTextDrawable
                    textView.layoutParams?.width =
                        context?.resources?.getDimension(R.dimen.cam_info_width)?.toInt() ?: 300
                    textView.textSize =
                        context?.resources?.getDimension(R.dimen.cam_doc_font) ?: 24f
                    textView.text = getString(R.string.acuant_camera_align)
                    textView.setTextColor(Color.WHITE)
                }
            }
        }
    }

    override fun rotateUi(rotation: Int) {
        textView?.rotation = rotation.toFloat()
    }

    override fun resetWorkflow() {
        lastTime = System.currentTimeMillis()
        currentDigit = 0
        if (tapToCapture) {
            setTextFromState(DocumentCameraState.Align)
            textView?.text = getString(R.string.acuant_camera_align_and_tap)
            textView?.layoutParams?.width = context?.resources?.getDimension(R.dimen.cam_info_width)?.toInt() ?: 300
        }
    }

    override fun onResume() {
        lastTime = System.currentTimeMillis()
        super.onResume()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        greenTransparent = getColorWithAlpha(Color.GREEN, .50f)

        cameraUiContainerBinding?.root?.let {
                fragmentCameraBinding!!.root.removeView(it)
            }

        cameraUiContainerBinding = DocumentFragmentUiBinding.inflate(
            LayoutInflater.from(requireContext()),
            fragmentCameraBinding!!.root,
            true
        )

        rectangleView = cameraUiContainerBinding?.documentRectangle
        textView = cameraUiContainerBinding?.documentText

        setOptions(rectangleView)
        currentDigit = 0

        capturingTextDrawable =  AppCompatResources.getDrawable(requireContext(), R.drawable.camera_text_config_capturing)
        defaultTextDrawable = AppCompatResources.getDrawable(requireContext(), R.drawable.camera_text_config_default)
        holdTextDrawable = AppCompatResources.getDrawable(requireContext(), R.drawable.camera_text_config_hold)

    }

    private fun setTapToCapture() {
        frameAnalyzer.disableDocumentDetection()
        tapToCapture = true
        setTextFromState(DocumentCameraState.Align)
        textView?.text = getString(R.string.acuant_camera_align_and_tap)
        textView?.layoutParams?.width = context?.resources?.getDimension(R.dimen.cam_info_width)?.toInt() ?: 300
        fragmentCameraBinding?.root?.setOnClickListenerWithPoint { point ->
            activity?.runOnUiThread {
                textView?.setBackgroundColor(greenTransparent)
                textView?.text = getString(R.string.acuant_camera_capturing)
                captureImage(object : IAcuantSavedImage {
                    override fun onSaved(bytes: ByteArray) {
                        cameraActivityListener.onCameraDone(bytes, latestBarcode)
                    }

                    override fun onError(error: AcuantError) {
                        cameraActivityListener.onError(error)
                    }
                }, point, captureType = "TAP")
            }
        }
    }

    private fun onBarcodeDetection(barcode: String?) {
        if (barcode != null) {
            latestBarcode = barcode
        }
    }

    override fun buildImageAnalyzer(screenAspectRatio: Int, trueScreenRatio: Float, rotation: Int) {
        frameAnalyzer = DocumentFrameAnalyzer (trueScreenRatio) { result, detectTime ->
            onBarcodeDetection(result.barcode)
            onDocumentDetection(result.points, result.currentDistRatio, result.analyzerDpi, result.state, detectTime)
        }
        if (!acuantOptions.autoCapture) {
            setTapToCapture()
        }
        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetResolution(Size(1280, 960))
//            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, frameAnalyzer)
            }
    }

    companion object {
        internal const val TOO_SLOW_FOR_AUTO_THRESHOLD: Long = 200

        /**
         * How much total x/y movement between frames is too much
         */
        internal const val TOO_MUCH_MOVEMENT = 350

        @Suppress("SameParameterValue")
        private fun getColorWithAlpha(color: Int, ratio: Float): Int {
            return Color.argb((Color.alpha(color) * ratio).roundToInt(), Color.red(color), Color.green(color), Color.blue(color))
        }

        @JvmStatic fun newInstance(acuantOptions: AcuantCameraOptions): AcuantDocCameraFragment {
            val frag = AcuantDocCameraFragment()
            val args = Bundle()
            args.putSerializable(INTERNAL_OPTIONS, acuantOptions)
            frag.arguments = args
            return frag
        }
    }
}