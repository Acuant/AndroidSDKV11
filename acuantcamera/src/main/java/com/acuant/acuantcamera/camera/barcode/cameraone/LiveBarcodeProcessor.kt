package com.acuant.acuantcamera.camera.barcode.cameraone

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.util.Log
import android.util.Size
import com.google.android.gms.vision.MultiProcessor
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector


class LiveBarcodeProcessor : BarcodeGraphicTracker.BarcodeUpdateListener {
    private var feedbackListener: ((AcuantBarcodeFeedback) -> Unit)? = null
    private var finishedCapturing = false
    private var barcodeDetector: com.acuant.acuantcamera.camera.barcode.cameraone.BarcodeDetector? = null

    fun getBarcodeDetector(context: Context, listener: (AcuantBarcodeFeedback) -> Unit): com.acuant.acuantcamera.camera.barcode.cameraone.BarcodeDetector {
        this.feedbackListener = listener
        val barcodeDetectorDelagte = BarcodeDetector.Builder(context).setBarcodeFormats(Barcode.PDF417).build()
        val barcodeFactory = BarcodeTrackerFactory(this)
        val processor = MultiProcessor.Builder(barcodeFactory).build()
        barcodeDetector = BarcodeDetector(barcodeDetectorDelagte)
        barcodeDetector!!.setProcessor(processor)

        if (!barcodeDetector!!.isOperational) {
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

        return barcodeDetector as com.acuant.acuantcamera.camera.barcode.cameraone.BarcodeDetector
    }

    fun stop(){
        finishedCapturing = true
        feedbackListener = null
        thread?.join()
        barcodeDetector?.release()
        thread = null
        barcodeDetector = null
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
                        frame = barcodeDetector?.frame
                        if (frame != null) {
                            val frameSize = Size(0, 0)
                            try {
                                var feedback: AcuantBarcodeFeedback?

                                feedback = AcuantBarcodeFeedback(BarcodeFeedback.NoDocument, null, frameSize, null, 0)

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
            feedbackListener?.let { it(AcuantBarcodeFeedback(BarcodeFeedback.Barcode, null, null, barcode.rawValue)) }
        }
    }
}
