package com.acuant.acuantcamera.detector

import android.graphics.Bitmap

interface IAcuantDetector{
    fun detect(bitmap: Bitmap?)
    fun clean()
}