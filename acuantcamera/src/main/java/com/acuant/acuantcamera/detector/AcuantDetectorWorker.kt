package com.acuant.acuantcamera.detector

import android.graphics.*
import android.media.Image
import android.os.AsyncTask
import com.acuant.acuantcamera.detector.document.AcuantDocumentDetector
import java.lang.Exception
import kotlin.concurrent.thread

class AcuantDetectorWorker(private val detectors: List<IAcuantDetector>, private val image: Image, private val runDocumentDetection: Boolean = true): AsyncTask<Void, Void, Void>(){
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
                if (runDocumentDetection || it !is AcuantDocumentDetector) {
                    thread {
                        it.detect(bitmap)
                    }
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
