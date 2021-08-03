package com.acuant.acuantcamera.detector.barcode

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.acuant.acuantcamera.detector.BaseAcuantDetector
import com.google.android.gms.vision.barcode.BarcodeDetector
import com.acuant.acuantcamera.detector.barcode.tracker.BarcodeTrackerFactory
import com.google.android.gms.vision.MultiProcessor
import com.google.android.gms.vision.Frame


class AcuantBarcodeDetector(context: Context, callback: AcuantBarcodeDetectorHandler) : BaseAcuantDetector() {
    private lateinit var barcodeDetector: BarcodeDetector
    private var isInitialized = false

    init {
        try {
            barcodeDetector = BarcodeDetector.Builder(context).build()
            barcodeDetector.setProcessor(MultiProcessor.Builder(BarcodeTrackerFactory(callback)).build())
            isInitialized = true
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("barcode", "Error initializing barcode")
        }
    }

    override fun detect(bitmap: Bitmap?) {
        if (isInitialized && bitmap != null && !isProcessing) {
            try {
                barcodeDetector.receiveFrame(Frame.Builder()
                        .setBitmap(bitmap)
                        .build())
            }
            catch (e: Exception) {
                Log.e("Barcode", e.toString())
            }
        }
    }

    override fun clean() {
        super.clean()
        if (isInitialized) {
            isInitialized = false
            barcodeDetector.release()
        }
    }
}