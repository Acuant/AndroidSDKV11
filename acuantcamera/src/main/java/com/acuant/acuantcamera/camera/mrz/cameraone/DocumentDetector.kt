package com.acuant.acuantcamera.camera.mrz.cameraone

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.SparseArray

import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.barcode.Barcode

import java.io.ByteArrayOutputStream

class DocumentDetector(private val mDelegate: Detector<Barcode>) : Detector<Barcode>() {
    var frame: Bitmap? = null
        private set

    override fun detect(frame: Frame): SparseArray<Barcode> {
        val yuvImage = YuvImage(frame.grayscaleImageData.array(), ImageFormat.NV21, frame.metadata.width, frame.metadata.height, null)
        val byteArrayOutputStream = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, frame.metadata.width, frame.metadata.height), 100, byteArrayOutputStream)
        val jpegArray = byteArrayOutputStream.toByteArray()
        this.frame = BitmapFactory.decodeByteArray(jpegArray, 0, jpegArray.size)
        return mDelegate.detect(frame)
    }

    override fun isOperational(): Boolean {
        return mDelegate.isOperational
    }

    override fun setFocus(id: Int): Boolean {
        return mDelegate.setFocus(id)
    }

    private fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

}
