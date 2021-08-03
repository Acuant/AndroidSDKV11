package com.acuant.acuantcamera.detector

import android.graphics.Bitmap

interface IAcuantDetector{

    var isProcessing: Boolean

    fun detect(bitmap: Bitmap?)
    fun clean()
}