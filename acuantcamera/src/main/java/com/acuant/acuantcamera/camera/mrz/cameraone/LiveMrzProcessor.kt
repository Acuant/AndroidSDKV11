package com.acuant.acuantcamera.camera.mrz.cameraone

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.acuant.acuantcamera.detector.ocr.AcuantOcrDetector
import com.google.android.gms.vision.MultiProcessor
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector


class LiveMrzProcessor : MrzGraphicTracker.BarcodeUpdateListener {
    private var finishedCapturing = false
    private var mrzDetector: MrzDetector? = null
    private var ocrDetector: AcuantOcrDetector? = null
    var frame: Bitmap? = null

    fun setOcrDetector(detector: AcuantOcrDetector) {
        ocrDetector = detector
    }

    fun getDetector(context: Context): MrzDetector {
        val barcodeDetectorDelagte = BarcodeDetector.Builder(context).setBarcodeFormats(Barcode.PDF417).build()
        val barcodeFactory = MrzTrackerFactory(this)
        val processor = MultiProcessor.Builder(barcodeFactory).build()
        mrzDetector = MrzDetector(barcodeDetectorDelagte)
        mrzDetector!!.setProcessor(processor)

        provideFeedback()

        return mrzDetector as MrzDetector
    }

    fun stop(){
        finishedCapturing = true
        thread?.join()
        mrzDetector?.release()
        thread = null
        mrzDetector = null
       // thread = null
    }

    private var thread : Thread? = null

    private fun provideFeedback() {
        if (ocrDetector == null) {
            Log.e("ocrDetector", "OCR detector can not be null")
            //TODO: return error
        }
        thread = Thread(object : Runnable {
            private var flag = true
            private var processing = false
            override fun run() {
                while (flag) {
                    if (!processing) {
                        processing = true
                        frame = mrzDetector?.frame
                        if (frame != null) {
                            ocrDetector?.detect(frame)

                        }
                        processing = false

                    }
                    flag = !finishedCapturing
                }
            }
        })

        thread!!.start()
    }

    override fun onBarcodeDetected(barcode: Barcode?) {
        //do nothing eventually refactor the code
    }
}
