package com.acuant.acuantcamera.detector.document

import android.graphics.Bitmap
import com.acuant.acuantcamera.detector.BaseAcuantDetector
import com.acuant.acuantcommon.model.Image
import com.acuant.acuantimagepreparation.AcuantImagePreparation
import com.acuant.acuantimagepreparation.model.DetectData

class AcuantDocumentDetector constructor (private val callback: AcuantDocumentDetectorHandler) : BaseAcuantDetector() {
    @Suppress("DEPRECATION")
    @Deprecated("Handler class had a spelling mistake, pass in AcuantDocumentDetectorHandler instead.")
    constructor (callback: AcuantDocumentDectectorHandler) : this (callback as AcuantDocumentDetectorHandler)

    override fun detect(bitmap: Bitmap?) {
        if (!isProcessing) {
            val cropStart = System.currentTimeMillis()
            var croppedImage: Image? = null

            if (bitmap != null) {
                val data = DetectData(bitmap)

                try {
                    croppedImage = AcuantImagePreparation.detect(data)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            callback.onDetected(croppedImage, System.currentTimeMillis() - cropStart)
        }
    }
}