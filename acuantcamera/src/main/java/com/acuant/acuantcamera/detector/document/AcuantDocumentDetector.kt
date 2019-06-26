package com.acuant.acuantcamera.detector.document

import android.graphics.Bitmap
import android.util.Size
import com.acuant.acuantcamera.detector.IAcuantDetector
import com.acuant.acuantcommon.model.Image
import com.acuant.acuantimagepreparation.AcuantImagePreparation
import com.acuant.acuantimagepreparation.model.CroppingData

class AcuantDocumentDetector(private val callback: AcuantDocumentDectectorHandler): IAcuantDetector {
    override fun detect(bitmap: Bitmap?){
        val cropStart = System.currentTimeMillis()
        var croppedImage: Image? = null

        if(bitmap != null){
            val data = CroppingData()
            data.image = bitmap

            try{
                croppedImage = AcuantImagePreparation.crop(data)
            }
            catch(e: Exception){
                e.printStackTrace()
            }
        }

        callback.onDetected(croppedImage, System.currentTimeMillis() - cropStart)
    }
    override fun clean() {}
}