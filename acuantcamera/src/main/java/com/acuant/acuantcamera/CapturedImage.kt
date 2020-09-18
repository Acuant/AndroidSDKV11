package com.acuant.acuantcamera

import android.graphics.Bitmap
import com.acuant.acuantcommon.model.Image

/**
 * Created by tapasbehera on 4/30/18.
 */
@Suppress("DEPRECATION")
@Deprecated("Not used by SDK, does not belong in this module, implement in app if desired")
class CapturedImage {
    companion object {
        @Deprecated("Not used by SDK, does not belong in this module, implement in app if desired")
        var bitmapImage: Bitmap? = null
        @Deprecated("Not used by SDK, does not belong in this module, implement in app if desired")
        var acuantImage: Image? = null
        @Suppress("unused")
        @Deprecated("Not used by SDK, does not belong in this module, implement in app if desired")
        var barcodeString : String? = null
        @Suppress("unused")
        @Deprecated("Not used by SDK, does not belong in this module, implement in app if desired")
        var sharpnessScore : Int = 0
        @Suppress("unused")
        @Deprecated("Not used by SDK, does not belong in this module, implement in app if desired")
        var glareScore : Int = 0

        @Suppress("DeprecatedCallableAddReplaceWith")
        @Deprecated("Not used by SDK, does not belong in this module, implement in app if desired")
        fun setImage(image:Bitmap?){
            bitmapImage = image
        }

        @Suppress("DeprecatedCallableAddReplaceWith")
        @Deprecated("Not used by SDK, does not belong in this module, implement in app if desired")
        fun setImage(image:Image?){
            acuantImage = image
        }

        @Deprecated("Not used by SDK, does not belong in this module, implement in app if desired")
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