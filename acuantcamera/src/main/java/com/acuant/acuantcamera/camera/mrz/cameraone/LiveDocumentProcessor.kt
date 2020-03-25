package com.acuant.acuantcamera.camera.mrz.cameraone

import android.content.Context
import android.graphics.Bitmap
import com.acuant.acuantcamera.detector.ocr.AcuantOcrDetector
import com.google.android.gms.vision.MultiProcessor
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector


class LiveDocumentProcessor : DocumentGraphicTracker.BarcodeUpdateListener {
    private var finishedCapturing = false
    private var documentDetector: DocumentDetector? = null
    private var ocrDetector: AcuantOcrDetector? = null
    var frame: Bitmap? = null

    fun setOcrDetector(detector: AcuantOcrDetector) {
        ocrDetector = detector
    }

    fun getDetector(context: Context): DocumentDetector {
        val barcodeDetectorDelagte = BarcodeDetector.Builder(context).setBarcodeFormats(Barcode.PDF417).build()
        val barcodeFactory = DocumentTrackerFactory(this)
        val processor = MultiProcessor.Builder(barcodeFactory).build()
        documentDetector = DocumentDetector(barcodeDetectorDelagte)
        documentDetector!!.setProcessor(processor)

        provideFeedback()

        return documentDetector as DocumentDetector
    }

    fun stop(){
        finishedCapturing = true
        thread?.join()
        documentDetector?.release()
        thread = null
        documentDetector = null
       // thread = null
    }

    private var thread : Thread? = null

    private fun provideFeedback() {
        if(ocrDetector == null) {
            //TODO: return error
        }
        thread = Thread(object : Runnable {
            private var flag = true
            private var processing = false
            override fun run() {
                while (flag) {
                    if (!processing) {
                        processing = true
                        frame = documentDetector?.frame
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
