package com.acuant.acuantcamera.camera.document.cameraone

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.util.Log
import android.util.Size
import com.acuant.acuantcamera.camera.document.AcuantDocCameraFragment
import com.acuant.acuantimagepreparation.AcuantImagePreparation
import com.acuant.acuantimagepreparation.model.DetectData
import com.google.android.gms.vision.MultiProcessor
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector


class LiveDocumentProcessor : DocumentGraphicTracker.BarcodeUpdateListener {
    private var feedbackListener: ((AcuantDocumentFeedback) -> Unit)? = null
    private var finishedCapturing = false
    private var documentDetector: DocumentDetector? = null

    fun getBarcodeDetector(context: Context, listener: (AcuantDocumentFeedback) -> Unit): DocumentDetector {
        this.feedbackListener = listener
        val barcodeDetectorDelagte = BarcodeDetector.Builder(context).setBarcodeFormats(Barcode.PDF417).build()
        val barcodeFactory = DocumentTrackerFactory(this)
        val processor = MultiProcessor.Builder(barcodeFactory).build()
        documentDetector = DocumentDetector(barcodeDetectorDelagte)
        documentDetector!!.setProcessor(processor)

        if (!documentDetector!!.isOperational) {
            // Note: The first time that an app using the barcode or face API is installed on a
            // device, GMS will download a native libraries to the device in order to do detection.
            // Usually this completes before the app is run for the first time.  But if that
            // download has not yet completed, then the above call will not detect any barcodes
            // and/or faces.
            //
            // isOperational() can be used to check if the required native libraries are currently
            // available.  The detectors will automatically become operational once the library
            // downloads complete on device.

            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            val lowstorageFilter = IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW)
            val hasLowStorage = context.registerReceiver(null, lowstorageFilter) != null

            if (hasLowStorage) {
                Log.d("Live Doc Processor", "Received low storage warning")
            }
        }
        provideFeedback()

        return documentDetector as DocumentDetector
    }

    fun stop(){
        finishedCapturing = true
        feedbackListener = null
        thread?.join()
        documentDetector?.release()
        thread = null
        documentDetector = null
       // thread = null
    }

    private var thread : Thread? = null

    private fun provideFeedback() {
        thread = Thread(object : Runnable {
            private var flag = true
            private var processing = false
            private var frame: Bitmap? = null
            override fun run() {
                while (flag) {
                    if (!processing) {
                        processing = true
                        frame = documentDetector?.frame
                        if (frame != null) {
                            val data = DetectData(frame!!)
                            val frameSize = Size(data.image.width, data.image.height)
                            try {
                                val startTime = System.currentTimeMillis()
                                val acuantImage = AcuantImagePreparation.detect(data)
                                val elapsed = System.currentTimeMillis() - startTime
                                var feedback: AcuantDocumentFeedback?

                                feedback = if (acuantImage.points == null || acuantImage.dpi < 20) {
                                    AcuantDocumentFeedback(DocumentFeedback.NoDocument, null, frameSize, null, elapsed)
                                } else if (!AcuantDocCameraFragment.isAcceptableDistance(acuantImage.points, frameSize)) {
                                    AcuantDocumentFeedback(DocumentFeedback.SmallDocument, acuantImage.points, frameSize, null, elapsed)
                                } else if (!acuantImage.isCorrectAspectRatio) {
                                    AcuantDocumentFeedback(DocumentFeedback.BadDocument, acuantImage.points, frameSize, null, elapsed)
                                } else {
                                    AcuantDocumentFeedback(DocumentFeedback.GoodDocument, acuantImage.points, frameSize, null, elapsed)
                                }

                                feedbackListener?.let { it(feedback) }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

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
        if(barcode?.rawValue != null){
            feedbackListener?.let { it(AcuantDocumentFeedback(DocumentFeedback.Barcode, null, null, barcode.rawValue)) }
        }
    }
}
