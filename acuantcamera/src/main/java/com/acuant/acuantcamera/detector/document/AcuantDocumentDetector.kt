package com.acuant.acuantcamera.detector.document

import android.graphics.Bitmap
import com.acuant.acuantcamera.detector.IAcuantDetector
import com.acuant.acuantcommon.model.Image
import com.acuant.acuantimagepreparation.AcuantImagePreparation
import com.acuant.acuantimagepreparation.model.CroppingData

class AcuantDocumentDetector constructor (private val callback: AcuantDocumentDetectorHandler): IAcuantDetector {
    @Deprecated("Handler class had a spelling mistake, pass in AcuantDocumentDetectorHandler instead.")
    constructor(callback: AcuantDocumentDectectorHandler) : this(callback as AcuantDocumentDetectorHandler)
    override fun detect(bitmap: Bitmap?){
        val cropStart = System.currentTimeMillis()
        var croppedImage: Image? = null

        if(bitmap != null){
            val data = CroppingData()
            data.image = bitmap

            try{
                croppedImage = AcuantImagePreparation.detect(data)
            }
            catch(e: Exception){
                e.printStackTrace()
            }
        }

        callback.onDetected(croppedImage, System.currentTimeMillis() - cropStart)
    }
    override fun clean() {}
}