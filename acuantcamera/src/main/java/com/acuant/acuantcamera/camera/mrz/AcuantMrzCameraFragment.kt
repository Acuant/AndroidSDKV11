package com.acuant.acuantcamera.camera.mrz

import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.acuant.acuantcamera.R
import com.acuant.acuantcamera.camera.AcuantBaseCameraFragment
import com.acuant.acuantcamera.camera.AcuantCameraOptions
import com.acuant.acuantcamera.camera.ICameraActivityFinish
import com.acuant.acuantcamera.constant.ACUANT_EXTRA_CAMERA_OPTIONS
import com.acuant.acuantcamera.detector.ocr.AcuantOcrDetector
import com.acuant.acuantcamera.detector.ocr.AcuantOcrDetectorHandler
import com.acuant.acuantcamera.helper.MrzParser
import com.acuant.acuantcamera.helper.MrzResult
import com.acuant.acuantcamera.overlay.MrzRectangleView
import kotlin.math.*


class AcuantMrzCameraFragment : AcuantBaseCameraFragment(), ActivityCompat.OnRequestPermissionsResultCallback, AcuantOcrDetectorHandler {

    /**
     * This is the output file for our picture.
     */
    private val mrzParser = MrzParser()
    private var tries = 0
    private var handler: Handler? = Handler()
    private var mrzResult : MrzResult? = null
    private var capturing = false
    private var allowCapture = false


    override fun setTextFromState(state: CameraState) {
        setTextFromState(activity!!, state, textView, imageView)
    }

    override fun onPointsDetected(points: Array<Point>?) {
        activity!!.runOnUiThread {
            if (points != null) {
                if (points.size == 4) {
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

                    MrzRectangleView.fixPoints(points)

                    var dist = 0
                    if (oldPoints != null && oldPoints!!.size == 4 && points.size == 4) {
                        for (i in 0..3) {
                            dist += sqrt(((oldPoints!![i].x - points[i].x).toDouble().pow(2) + (oldPoints!![i].y - points[i].y).toDouble().pow(2))).toInt()
                        }
                    }

                    if (dist > TOO_MUCH_MOVEMENT) {
                        resetCapture()
                    }

                    if (capturing) {
                        setTextFromState(CameraState.MrzCapturing)
                        rectangleView.setViewFromState(CameraState.MrzCapturing)
                    } else if (!isAligned(points) || !isAcceptableAspectRatio(points)) {
                        resetCapture()
                        setTextFromState(CameraState.MrzAlign)
                        rectangleView.setViewFromState(CameraState.MrzAlign)
                        rectangleView.setAndDrawPoints(null)
                    } else if(!isAcceptableDistance(points, textureView.height.toFloat())) {
                        resetCapture()
                        setTextFromState(CameraState.MrzMoveCloser)
                        rectangleView.setViewFromState(CameraState.MrzMoveCloser)
                        rectangleView.setAndDrawPoints(points)
                    }else if (tries < ALLOWED_ERRORS) {
                        allowCapture = true
                        setTextFromState(CameraState.MrzTrying)
                        rectangleView.setViewFromState(CameraState.MrzTrying)
                        rectangleView.setAndDrawPoints(points)
                    } else {
                        allowCapture = true
                        setTextFromState(CameraState.MrzReposition)
                        rectangleView.setViewFromState(CameraState.MrzReposition)
                        rectangleView.setAndDrawPoints(points)
                    }
                } else if(!capturing) {
                    resetCapture()
                    setTextFromState(CameraState.MrzNone)
                    rectangleView.setViewFromState(CameraState.MrzNone)
                    rectangleView.setAndDrawPoints(null)
                }
            } else if(!capturing) {
                resetCapture()
                setTextFromState(CameraState.MrzNone)
                rectangleView.setViewFromState(CameraState.MrzNone)
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
                    this.isCapturing = true
                    setTextFromState(CameraState.MrzCapturing)
                    rectangleView.setViewFromState(CameraState.MrzCapturing)
                    if (mrzResult == null || (mrzResult?.country == "" && result.country != "")) {
                        mrzResult = result
                    }
                    handler?.postDelayed({
                        handler?.removeCallbacksAndMessages(null)
                        if (activity is ICameraActivityFinish) {
                            (activity as ICameraActivityFinish).onActivityFinish(result)
                        }
                    }, 750)
                }
            }
            ++tries
        }
        this.isProcessing = false
    }

    override fun onPause() {
        handler?.removeCallbacksAndMessages(null)
        handler = null
        super.onPause()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        detectors = listOf(AcuantOcrDetector(activity!!.applicationContext, this))
        tries = 0


        options = arguments?.getSerializable(ACUANT_EXTRA_CAMERA_OPTIONS) as AcuantCameraOptions?
                ?: AcuantCameraOptions.MrzCameraOptionsBuilder()
                        .setAllowBox(isBorderEnabled)
                        .build()

        capturingTextDrawable = activity!!.getDrawable(R.drawable.camera_text_config_capturing)
        defaultTextDrawable = activity!!.getDrawable(R.drawable.camera_text_config_default)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        textureView = view.findViewById(R.id.texture)
        rectangleView = view.findViewById(R.id.acu_mrz_rectangle) as MrzRectangleView

        super.onViewCreated(view, savedInstanceState)
    }

    override fun setTapToCapture() {
        //mrz does not currently support tap to capture
    }

    companion object {
        const val ALLOWED_ERRORS = 7

        const val TOO_MUCH_MOVEMENT = 200



        private fun distance(pointA: Point, pointB: Point): Float {
            return sqrt( (pointA.x - pointB.x).toFloat().pow(2) + (pointA.y - pointB.y).toFloat().pow(2))
        }

        fun isAligned(points: Array<Point>) : Boolean {
            if (points.size != 4)
                return false
            val val1 = distance(points[0], points[2])
            val val2 = distance(points[1], points[3])
            return abs(val1 - val2) < 15
        }

        fun isAcceptableAspectRatio(points: Array<Point>) : Boolean {
            val ratio = distance(points[0], points[3]) / distance(points[0], points[1])
            return ratio > 8f && ratio < 10f
        }

        fun isAcceptableDistance(points: Array<Point>, screenSize: Float): Boolean {
            val dist = max(distance(points[0], points[1]), distance(points[0], points[3]))
            return dist > 0.65 * screenSize
        }

        @JvmStatic fun newInstance(): AcuantBaseCameraFragment = AcuantMrzCameraFragment()
        fun setTextFromState(context: Context, state: CameraState, textView: TextView, imageView: ImageView) {

            imageView.visibility = View.INVISIBLE
            textView.visibility = View.VISIBLE

            when (state) {
                CameraState.MrzReposition -> {
                    textView.background = context.getDrawable(R.drawable.camera_text_config_default)
                    textView.text = context.resources.getString(R.string.acuant_glare_mrz)
                    textView.setTextColor(Color.WHITE)
                    textView.textSize = 24f
                    textView.layoutParams.width = context.resources.getDimension(R.dimen.cam_error_width).toInt()
                }
                CameraState.MrzTrying -> {
                    textView.background = context.getDrawable(R.drawable.camera_text_config_default)
                    textView.text = context.resources.getString(R.string.acuant_reading_mrz)
                    textView.setTextColor(Color.WHITE)
                    textView.textSize = 24f
                    textView.layoutParams.width = context.resources.getDimension(R.dimen.cam_info_width).toInt()
                }
                CameraState.MrzCapturing -> {
                    textView.background = context.getDrawable(R.drawable.camera_text_config_default)
                    textView.text = context.resources.getString(R.string.acuant_read_mrz)
                    textView.setTextColor(Color.WHITE)
                    textView.textSize = 24f
                    textView.layoutParams.width = context.resources.getDimension(R.dimen.cam_info_width).toInt()
                }
                CameraState.MrzMoveCloser -> {
                    textView.background = context.getDrawable(R.drawable.camera_text_config_default)
                    textView.text = context.resources.getString(R.string.acuant_closer_mrz)
                    textView.setTextColor(Color.WHITE)
                    textView.textSize = 24f
                }
                else -> {//none //align
                    textView.visibility = View.INVISIBLE
                    imageView.visibility = View.VISIBLE
                }
            }
        }
    }
}