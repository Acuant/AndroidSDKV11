package com.acuant.acuantcamera

import android.graphics.Bitmap
import android.graphics.BitmapFactory.*
import com.acuant.acuantcommon.model.Image

/**
 * Created by tapasbehera on 4/30/18.
 */
class CapturedImage {
    companion object {
        var bitmapImage: Bitmap? = null
        var acuantImage: Image? = null
        var barcodeString : String? = null
        var sharpnessScore : Int = 0
        var glareScore : Int = 0

        fun setImage(image:Bitmap?){
            bitmapImage = image
        }

        fun setImage(image:Image?){
            acuantImage = image
        }

        fun clear(){
            if(bitmapImage!=null){
                if(!bitmapImage!!.isRecycled) {
                    bitmapImage!!.recycle()
                }
            }
            if(acuantImage!=null && acuantImage!!.image!=null){
                if(!acuantImage!!.image.isRecycled) {
                    acuantImage!!.image.recycle()
                }
            }
        }
    }

}