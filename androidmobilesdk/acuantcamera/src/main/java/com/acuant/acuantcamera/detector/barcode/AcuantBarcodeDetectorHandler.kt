package com.acuant.acuantcamera.detector.barcode

import android.support.annotation.UiThread

interface AcuantBarcodeDetectorHandler{
    @UiThread
    fun onBarcodeDetected(barcode: String)
}
