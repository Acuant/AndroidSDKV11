package com.acuant.acuantcamera.camera.barcode

import android.graphics.*
import android.os.*
import android.support.v4.app.ActivityCompat
import android.support.v4.content.res.ResourcesCompat
import android.util.Log
import android.view.*
import com.acuant.acuantcamera.R
import com.acuant.acuantcamera.camera.AcuantBaseCameraFragment
import com.acuant.acuantcamera.camera.AcuantCameraOptions
import com.acuant.acuantcamera.camera.ICameraActivityFinish
import com.acuant.acuantcamera.constant.*
import com.acuant.acuantcamera.detector.barcode.AcuantBarcodeDetector
import com.acuant.acuantcamera.detector.barcode.AcuantBarcodeDetectorHandler
import com.acuant.acuantcamera.overlay.DocRectangleView

class AcuantBarcodeCameraFragment : AcuantBaseCameraFragment(),
        ActivityCompat.OnRequestPermissionsResultCallback, AcuantBarcodeDetectorHandler {

    private var done = false
    private lateinit var autoCancel: CountDownTimer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        options = arguments?.getSerializable(ACUANT_EXTRA_CAMERA_OPTIONS) as AcuantCameraOptions? ?: AcuantCameraOptions.BarcodeCameraOptionsBuilder().build()

        detectors = listOf(AcuantBarcodeDetector(this.activity!!.applicationContext, this))

        defaultTextDrawable = activity!!.getDrawable(R.drawable.camera_text_config_default)
        capturingTextDrawable = activity!!.getDrawable(R.drawable.camera_text_config_capturing)
    }

    override fun setTextFromState(state: CameraState) {

        textView.visibility = View.VISIBLE
        textView.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT

        when(state) {
            CameraState.Capturing -> {
                imageView.visibility = View.GONE
                textView.background = defaultTextDrawable
                textView.layoutParams.width = context?.resources?.getDimension(R.dimen.cam_info_width)?.toInt() ?: 300
                textView.textSize = context?.resources?.getDimension(R.dimen.cam_doc_font) ?: 24f
                textView.text = resources.getString(R.string.acuant_camera_capturing_barcode)
                textView.setTextColor(options?.colorCapturing ?: Color.GREEN)
            }
            else -> {//align
                textView.background = defaultTextDrawable
                textView.layoutParams.width = context?.resources?.getDimension(R.dimen.cam_info_width)?.toInt() ?: 300
                textView.textSize = context?.resources?.getDimension(R.dimen.cam_doc_font) ?: 24f
                textView.text = getString(R.string.acuant_camera_align_barcode)
                textView.setTextColor(options?.colorHold ?: Color.WHITE)
                imageView.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.barcode, null))
                imageView.rotation = 0f
                imageView.alpha = 0.4f
                imageView.visibility = View.VISIBLE
                textView.bringToFront()
            }
        }
    }

    override fun onBarcodeDetected(barcode: String) {
        this.barCodeString = barcode
        detectors.forEach {
            if (it is AcuantBarcodeDetector) {
                it.isProcessing = false
            }
        }

        Log.d("BarcodeCamera","Barcode Detected, Done: $done")
        if (!done) {
            done = true
            activity?.runOnUiThread {
                setTextFromState(CameraState.Capturing)
                autoCancel.cancel()
                object : CountDownTimer(timeInMsPerDigit.toLong(), 100) {
                    override fun onFinish() {
                        finish()
                    }

                    override fun onTick(millisUntilFinished: Long) {
                        Log.d("BarcodeCamera","Capturing: $millisUntilFinished")
                    }
                }.start()
            }
        }
    }

    fun finish() {
        if (activity is ICameraActivityFinish) {
            (activity as ICameraActivityFinish).onActivityFinish(barCodeString)
        }
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_camera2_basic, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        textureView = view.findViewById(R.id.texture)
        rectangleView = view.findViewById(R.id.acu_doc_rectangle) as DocRectangleView
        rectangleView.visibility = View.GONE
        barcodeOnly = true

        super.onViewCreated(view, savedInstanceState)


        autoCancel = object : CountDownTimer(digitsToShow.toLong(), 1000) {
            override fun onFinish() {
                finish()
            }

            override fun onTick(millisUntilFinished: Long) {
                Log.d("BarcodeCamera","Giving Up In: $millisUntilFinished")
            }
        }
        autoCancel.start()

        setTextFromState(CameraState.Align)
    }

    override fun setTapToCapture() {
        //n/a
    }

    companion object {

        @JvmStatic fun newInstance(): AcuantBarcodeCameraFragment = AcuantBarcodeCameraFragment()
    }
}