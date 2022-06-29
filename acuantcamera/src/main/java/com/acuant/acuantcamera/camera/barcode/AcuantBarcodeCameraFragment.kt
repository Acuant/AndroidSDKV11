package com.acuant.acuantcamera.camera.barcode

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.camera.core.ImageAnalysis
import androidx.core.content.res.ResourcesCompat
import com.acuant.acuantcamera.R
import com.acuant.acuantcamera.camera.AcuantBaseCameraFragment
import com.acuant.acuantcamera.camera.AcuantCameraOptions
import com.acuant.acuantcamera.databinding.BarcodeFragmentUiBinding
import com.acuant.acuantcamera.detector.DocumentFrameAnalyzer

enum class BarcodeCameraState { Align, Capturing }

class AcuantBarcodeCameraFragment: AcuantBaseCameraFragment() {

    private var cameraUiContainerBinding: BarcodeFragmentUiBinding? = null
    private var textView: TextView? = null
    private var imageView: ImageView? = null
    private var autoCancelCountdown: CountDownTimer? = null
    private var captureCountdown: CountDownTimer? = null
    private var barcodeString: String? = null
    private var defaultTextDrawable: Drawable? = null
    private var timeToCancel: Long = 20000

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraUiContainerBinding?.root?.let {
            fragmentCameraBinding!!.root.removeView(it)
        }

        cameraUiContainerBinding = BarcodeFragmentUiBinding.inflate(
            LayoutInflater.from(requireContext()),
            fragmentCameraBinding!!.root,
            true
        )

        textView = cameraUiContainerBinding?.barcodeText
        imageView = cameraUiContainerBinding?.barcodeImage

        timeToCancel = acuantOptions.digitsToShow.toLong()

        defaultTextDrawable = AppCompatResources.getDrawable(requireContext(), R.drawable.camera_text_config_default)

    }

    override fun onPause() {
        autoCancelCountdown = null
        captureCountdown = null
        super.onPause()
    }

    override fun onResume() {
        if (autoCancelCountdown == null) {
            setTextFromState(BarcodeCameraState.Align)
            autoCancelCountdown = object : CountDownTimer(timeToCancel, INTERVAL) {
                override fun onFinish() {
                    cameraActivityListener.onCancel()
                }

                override fun onTick(millisUntilFinished: Long) {
                    timeToCancel -= INTERVAL
                }
            }.start()
        }
        super.onResume()
    }

    private fun setTextFromState(state: BarcodeCameraState) {
        if (!isAdded)
            return
        val imageView = this.imageView
        val textView = this.textView

        if (imageView != null && textView != null) {

            textView.visibility = View.VISIBLE

            when (state) {
                BarcodeCameraState.Capturing -> {
                    imageView.visibility = View.GONE
                    textView.background = defaultTextDrawable
                    textView.layoutParams?.width =
                        context?.resources?.getDimension(R.dimen.cam_info_width)?.toInt() ?: 300
                    textView.textSize =
                        context?.resources?.getDimension(R.dimen.cam_doc_font) ?: 24f
                    textView.text = resources.getString(R.string.acuant_camera_capturing_barcode)
                    textView.setTextColor(acuantOptions.colorCapturing)
                }
                else -> {//align
                    textView.background = defaultTextDrawable
                    textView.layoutParams?.width =
                        context?.resources?.getDimension(R.dimen.cam_info_width)?.toInt() ?: 300
                    textView.textSize =
                        context?.resources?.getDimension(R.dimen.cam_doc_font) ?: 24f
                    textView.text = getString(R.string.acuant_camera_align_barcode)
                    textView.setTextColor(acuantOptions.colorHold)
                    imageView.setImageDrawable(
                        ResourcesCompat.getDrawable(
                            resources,
                            R.drawable.barcode,
                            null
                        )
                    )
                    imageView.rotation = 90f
                    imageView.alpha = 0.4f
                    imageView.visibility = View.VISIBLE
                    textView.bringToFront()
                }
            }
        }
    }

    override fun rotateUi(rotation: Int) {
        textView?.rotation = rotation.toFloat()
    }

    private fun onBarcodeDetection(barcode: String?) {
        if (barcode != null) {
            barcodeString = barcode
            if (captureCountdown == null) {
                setTextFromState(BarcodeCameraState.Capturing)
                captureCountdown = object : CountDownTimer(acuantOptions.timeInMsPerDigit.toLong(), 100) {
                    override fun onFinish() {
                        cameraActivityListener.onCameraDone(barcodeString ?: barcode)
                    }

                    override fun onTick(millisUntilFinished: Long) {
                        //do nothing
                    }
                }.start()
            }
        }
    }

    override fun buildImageAnalyzer(screenAspectRatio: Int, trueScreenRatio: Float,  rotation: Int) {
        val frameAnalyzer = DocumentFrameAnalyzer (trueScreenRatio) { result, _ ->
            onBarcodeDetection(result.barcode)
        }
        frameAnalyzer.disableDocumentDetection()
        imageAnalyzer = ImageAnalysis.Builder()
//            .setTargetAspectRatio(screenAspectRatio)
            .setTargetResolution(Size(1280, 960))
            .setTargetRotation(rotation)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, frameAnalyzer)
            }
    }

    override fun resetWorkflow() {
        autoCancelCountdown?.cancel()
        captureCountdown?.cancel()
    }

    companion object {
        const val INTERVAL = 1000.toLong()

        @JvmStatic fun newInstance(acuantOptions: AcuantCameraOptions): AcuantBarcodeCameraFragment {
            val frag = AcuantBarcodeCameraFragment()
            val args = Bundle()
            args.putSerializable(INTERNAL_OPTIONS, acuantOptions)
            frag.arguments = args
            return frag
        }
    }
}