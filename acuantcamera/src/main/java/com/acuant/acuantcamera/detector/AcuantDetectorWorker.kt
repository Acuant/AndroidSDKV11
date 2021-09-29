package com.acuant.acuantcamera.detector

import android.graphics.*
import android.os.AsyncTask
import com.acuant.acuantcamera.detector.document.AcuantDocumentDetector
import kotlin.concurrent.thread

class AcuantDetectorWorker(private val detectors: List<IAcuantDetector>, private val bitmap: Bitmap, private val runDocumentDetection: Boolean = true): AsyncTask<Void, Void, Void>(){
    override fun doInBackground(vararg params: Void?): Void? {
        detectors.forEach{
            if (runDocumentDetection || it !is AcuantDocumentDetector) {
                thread {
                    it.detect(bitmap)
                }
            }
        }
        return null
    }
}
