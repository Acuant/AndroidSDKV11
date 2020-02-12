package com.acuant.acuantfacecapture.detector

import android.graphics.*
import android.util.SparseArray
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.face.Face
import java.io.ByteArrayOutputStream

class FaceDetector internal constructor(private val mDelegate: Detector<Face>) : Detector<Face>() {
    var frame: Bitmap? = null
        private set

    override fun detect(frame: Frame): SparseArray<Face> {
        val yuvImage = YuvImage(frame.grayscaleImageData.array(), ImageFormat.NV21, frame.metadata.width, frame.metadata.height, null)
        val byteArrayOutputStream = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, frame.metadata.width, frame.metadata.height), 100, byteArrayOutputStream)
        val jpegArray = byteArrayOutputStream.toByteArray()
        this.frame = BitmapFactory.decodeByteArray(jpegArray, 0, jpegArray.size)
        this.frame = rotateBitmap(this.frame as Bitmap, 270f)
        return mDelegate.detect(frame)
    }

    override fun isOperational(): Boolean {
        return mDelegate.isOperational
    }

    override fun setFocus(id: Int): Boolean {
        return mDelegate.setFocus(id)
    }

    @Suppress("SameParameterValue")
    private fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }
}