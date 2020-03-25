package com.acuant.acuantcamera.camera.document.cameraone

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.util.Size

import com.acuant.acuantimagepreparation.AcuantImagePreparation
import com.acuant.acuantimagepreparation.model.CroppingData
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
                            val data = CroppingData()
                            data.image = frame
                            val frameSize = Size(data.image.width, data.image.height)
                            try {
                                val acuantImage = AcuantImagePreparation.detect(data)
                                var feedback: AcuantDocumentFeedback? = null

                                feedback = if (acuantImage?.points == null || acuantImage.dpi < 20) {
                                    AcuantDocumentFeedback(DocumentFeedback.NoDocument, null, frameSize)
                                } else if ((!acuantImage.isPassport && acuantImage.dpi < (SMALL_DOC_DPI_SCALE_VALUE * frame!!.width)) || (acuantImage.isPassport && acuantImage.dpi < (LARGE_DOC_DPI_SCALE_VALUE * frame!!.width))) {
                                    AcuantDocumentFeedback(DocumentFeedback.SmallDocument, acuantImage.points, frameSize)
                                } else if (!acuantImage.isCorrectAspectRatio) {
                                    AcuantDocumentFeedback(DocumentFeedback.BadDocument, acuantImage.points, frameSize)
                                } else {
                                    AcuantDocumentFeedback(DocumentFeedback.GoodDocument, acuantImage.points, frameSize)
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
    /**
     * Target DPI for preview size 1920x1080 = 350
     * SMALL_DOC_DPI_SCALE_VALUE = target_dpi/preview_size_width
     */
    private  val SMALL_DOC_DPI_SCALE_VALUE = .18229

    /**
     * Target DPI for preview size 1920x1080 = 225
     * LARGE_DOC_DPI_SCALE_VALUE = target_dpi/preview_size_width
     */
    private  val LARGE_DOC_DPI_SCALE_VALUE = .11719
    override fun onBarcodeDetected(barcode: Barcode?) {
        if(barcode?.rawValue != null){
            feedbackListener?.let { it(AcuantDocumentFeedback(DocumentFeedback.Barcode, null, null, barcode.rawValue)) }
        }
    }
}
