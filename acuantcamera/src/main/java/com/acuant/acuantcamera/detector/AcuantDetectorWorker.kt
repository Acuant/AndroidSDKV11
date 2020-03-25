package com.acuant.acuantcamera.detector

import android.graphics.*
import android.media.Image
import android.os.AsyncTask
import java.lang.Exception
import kotlin.concurrent.thread

class AcuantDetectorWorker(private val detectors: List<IAcuantDetector>, private val image: Image): AsyncTask<Void, Void, Void>(){
    override fun doInBackground(vararg params: Void?): Void? {
        var bitmap:Bitmap? = null
        try{
            bitmap = imageToBitmap(image)
        }
        catch(e:Exception){
            e.printStackTrace()
        }
        finally {
            image.close()

            detectors.forEach{
                thread{
                    it.detect(bitmap)
                }
            }
        }
        return null
    }

    private fun imageToBitmap(image: Image):Bitmap {
        val imageBytes = ImageSaver.imageToByteArray(image)
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
}
