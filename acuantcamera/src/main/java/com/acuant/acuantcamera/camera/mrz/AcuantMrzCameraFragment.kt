package com.acuant.acuantcamera.camera.mrz

import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.camera.core.ImageAnalysis
import com.acuant.acuantcamera.R
import com.acuant.acuantcamera.camera.AcuantBaseCameraFragment
import com.acuant.acuantcamera.camera.AcuantCameraOptions
import com.acuant.acuantcamera.databinding.MrzFragmentUiBinding
import com.acuant.acuantcamera.detector.MrzFrameAnalyzer
import com.acuant.acuantcamera.detector.MrzState
import com.acuant.acuantcamera.helper.MrzResult
import com.acuant.acuantcamera.helper.PointsUtils
import com.acuant.acuantcamera.interfaces.IAcuantSavedImage
import com.acuant.acuantcamera.overlay.MrzRectangleView
import com.acuant.acuantcommon.model.AcuantError
import java.lang.ref.WeakReference
import kotlin.math.*

enum class MrzCameraState { Align, MoveCloser, Reposition, Trying, Capturing }

class AcuantMrzCameraFragment: AcuantBaseCameraFragment() {
    private var rectangleView: MrzRectangleView? = null
    private var textView: TextView? = null
    private var imageView: ImageView? = null
    private var cameraUiContainerBinding: MrzFragmentUiBinding? = null
    private var defaultTextDrawable: Drawable? = null
    private var tries = 0
    private var handler: Handler? = null
    private var oldPoints: Array<Point>? = null
    private var capturedBytes: ByteArray? = null
    private var validMrzResult: MrzResult? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraUiContainerBinding?.root?.let {
            fragmentCameraBinding!!.root.removeView(it)
        }

        cameraUiContainerBinding = MrzFragmentUiBinding.inflate(
            LayoutInflater.from(requireContext()),
            fragmentCameraBinding!!.root,
            true
        )

        rectangleView = cameraUiContainerBinding?.mrzRectangle
        textView = cameraUiContainerBinding?.mrzTextView
        imageView = cameraUiContainerBinding?.mrzImage

        setOptions(rectangleView)

        defaultTextDrawable = AppCompatResources.getDrawable(requireContext(), R.drawable.camera_text_config_default)

    }

    override fun onPause() {
        handler?.removeCallbacksAndMessages(null)
        handler = null
        super.onPause()
    }

    override fun onResume() {
        handler = Handler(Looper.getMainLooper())
        capturing = false
        super.onResume()
    }

    private fun onMrzDetection(points: Array<Point>?, result: MrzResult?, state: MrzState) {
        if (validMrzResult == null) {
            activity?.runOnUiThread {

                var detectedPoints = points

                if (detectedPoints != null) {
                    val camContainer = fragmentCameraBinding?.root
                    val analyzerSize = imageAnalyzer?.resolutionInfo?.resolution
                    val previewSize = fragmentCameraBinding?.viewFinder
                    detectedPoints = PointsUtils.fixPoints(
                        PointsUtils.scalePoints(
                            detectedPoints,
                            camContainer,
                            analyzerSize,
                            previewSize,
                            rectangleView
                        )
                    )

                    var dist = 0
                    if (oldPoints != null && oldPoints!!.size == 4 && detectedPoints.size == 4) {
                        for (i in 0..3) {
                            dist += sqrt(
                                ((oldPoints!![i].x - detectedPoints[i].x).toDouble()
                                    .pow(2) + (oldPoints!![i].y - detectedPoints[i].y).toDouble()
                                    .pow(2))
                            ).toInt()
                        }
                    }

                    if (dist > TOO_MUCH_MOVEMENT) {
                        resetWorkflow()
                    }

                    if (capturedBytes == null &&
                        !capturing &&
                        state != MrzState.NoMrz &&
                        PointsUtils.correctDirection(points, previewSize)
                    ) {
                        captureImage(object : IAcuantSavedImage {
                            override fun onSaved(bytes: ByteArray) {
                                capturing = false
                                capturedBytes = bytes
                                validMrzResult?.let {
                                    cameraActivityListener.onCameraDone(bytes, it)
                                }
                            }

                            override fun onError(error: AcuantError) {
                                capturing = false
                                validMrzResult?.let {
                                    cameraActivityListener.onCameraDone(it)
                                }
                            }
                        }, refocus = false)
                    }

                    when {
                        state == MrzState.NoMrz || !PointsUtils.correctDirection(
                            points,
                            previewSize
                        ) -> {
                            resetWorkflow()
                            setTextFromState(MrzCameraState.Align)
                            rectangleView?.setViewFromState(MrzCameraState.Align)
                        }

                        state == MrzState.TooFar -> {
                            resetWorkflow()

                            setTextFromState(MrzCameraState.MoveCloser)
                            rectangleView?.setViewFromState(MrzCameraState.MoveCloser)
                        }

                        else -> { //good mrz
                            when {
                                result != null && result.allCheckSumsPassed -> {
                                    validMrzResult = result
                                    setTextFromState(MrzCameraState.Capturing)
                                    rectangleView?.setViewFromState(MrzCameraState.Capturing)
                                    if (!capturing) {
                                        val handler = handler
                                        if (handler != null) {
                                            handler.postDelayed({
                                                this.handler?.removeCallbacksAndMessages(null)
                                                capturedBytes?.let {
                                                    cameraActivityListener.onCameraDone(it, result)
                                                } ?: kotlin.run {
                                                    cameraActivityListener.onCameraDone(result)
                                                }
                                            }, 750)
                                        } else {
                                            capturedBytes?.let {
                                                cameraActivityListener.onCameraDone(it, result)
                                            } ?: kotlin.run {
                                                cameraActivityListener.onCameraDone(result)
                                            }

                                        }
                                    }
                                }

                                tries < ALLOWED_ERRORS -> {
                                    ++tries
                                    setTextFromState(MrzCameraState.Trying)
                                    rectangleView?.setViewFromState(MrzCameraState.Trying)
                                }

                                else -> { //too many errors
                                    setTextFromState(MrzCameraState.Reposition)
                                    rectangleView?.setViewFromState(MrzCameraState.Reposition)
                                }
                            }
                        }
                    }
                }

                oldPoints = detectedPoints
                rectangleView?.setAndDrawPoints(detectedPoints)
            }
        }
    }

    override fun resetWorkflow() {
        tries = 0
    }
    
    private fun setTextFromState(state: MrzCameraState) {
        if (!isAdded)
            return
        val imageView = this.imageView
        val textView = this.textView

        if (imageView != null && textView != null) {
            imageView.visibility = View.INVISIBLE
            textView.visibility = View.VISIBLE

            when (state) {
                MrzCameraState.MoveCloser -> {
                    textView.background = defaultTextDrawable
                    textView.text = getString(R.string.acuant_closer_mrz)
                    textView.setTextColor(Color.WHITE)
                    textView.textSize = 24f
                }
                MrzCameraState.Reposition -> {
                    textView.background = defaultTextDrawable
                    textView.text = getString(R.string.acuant_glare_mrz)
                    textView.setTextColor(Color.WHITE)
                    textView.textSize = 24f
                    textView.layoutParams.width =
                        resources.getDimension(R.dimen.cam_error_width).toInt()
                }
                MrzCameraState.Trying -> {
                    textView.background = defaultTextDrawable
                    textView.text = getString(R.string.acuant_reading_mrz)
                    textView.setTextColor(Color.WHITE)
                    textView.textSize = 24f
                    textView.layoutParams.width = resources.getDimension(R.dimen.cam_info_width).toInt()
                }
                MrzCameraState.Capturing -> {
                    textView.background = defaultTextDrawable
                    textView.text = getString(R.string.acuant_read_mrz)
                    textView.setTextColor(Color.WHITE)
                    textView.textSize = 24f
                    textView.layoutParams.width = resources.getDimension(R.dimen.cam_info_width).toInt()
                }
                else -> {
                    textView.visibility = View.INVISIBLE
                    imageView.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun rotateUi(rotation: Int) {
        textView?.rotation = rotation.toFloat()
        imageView?.rotation = rotation.toFloat()
    }

    override fun buildImageAnalyzer(screenAspectRatio: Int, trueScreenRatio: Float,  rotation: Int) {
        val frameAnalyzer = MrzFrameAnalyzer (WeakReference(requireContext()), trueScreenRatio) { points, result, state ->
            onMrzDetection(points, result, state)
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
        const val ALLOWED_ERRORS = 7

        const val TOO_MUCH_MOVEMENT = 200

        @JvmStatic fun newInstance(acuantOptions: AcuantCameraOptions): AcuantMrzCameraFragment {
            val frag = AcuantMrzCameraFragment()
            val args = Bundle()
            args.putSerializable(INTERNAL_OPTIONS, acuantOptions)
            frag.arguments = args
            return frag
        }
    }
}