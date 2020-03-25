package com.acuant.acuantcamera.detector

import android.graphics.*
import android.media.Image
import android.util.Log

import android.graphics.ImageFormat
import android.graphics.YuvImage
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import java.io.*


/**
 * Saves a JPEG [Image] into the specified [File].
 */
interface ImageSaveHandler{
    fun onSave()
}

class ImageSaver(
        /**
         * The JPEG image
         */
        private val orientation: Int,
        private val image: Image,

        /**
         * The file we save the image into.
         */
        private val file: File,
        private val callback: ImageSaveHandler

) : Runnable {

    override fun run() {
        val buffer = image.planes[0].buffer
        var bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        //check if it needs to be rotated
        if(orientation < DEGREES_TO_ROTATE_IMAGE) {
            //code for rotating the image
            val rotated = rotateImage(BitmapFactory.decodeByteArray(bytes, 0, bytes.size), 180f)
            val stream = ByteArrayOutputStream()
            rotated.compress(CompressFormat.JPEG, 100, stream)
            rotated.recycle()
            bytes = stream.toByteArray()
        }

        var output: FileOutputStream? = null
        try {
            output = FileOutputStream(file).apply {write(bytes)}
        } catch (e: IOException) {
            Log.e(TAG, e.toString())
        } finally {
            image.close()
            output?.let {
                try {
                    it.close()
                } catch (e: IOException) {
                    Log.e(TAG, e.toString())
                }
            }
            callback.onSave()
        }
    }

    companion object {
        /**
         * Tag for the [Log].
         */
        private const val TAG = "ImageSaver"
        private const val DEGREES_TO_ROTATE_IMAGE = 181;

        @JvmStatic fun imageToByteArray(image: Image, quality: Int = 50): ByteArray {
            return NV21toJPEG(YUV420toNV21(image), image.getWidth(), image.getHeight(), 100)
        }

        fun rotateImage(img: Bitmap, degree: Float): Bitmap {
            val matrix = Matrix()
            matrix.setRotate(degree)
            val rotatedImg = Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
            img.recycle()
            return rotatedImg
        }

        private fun NV21toJPEG(nv21: ByteArray, width: Int, height: Int, quality: Int): ByteArray {
            val out = ByteArrayOutputStream()
            val yuv = YuvImage(nv21, ImageFormat.NV21, width, height, null)
            yuv.compressToJpeg(Rect(0, 0, width, height), quality, out)
            return out.toByteArray()
        }

        private fun YUV420toNV21(image: Image): ByteArray {
            val crop = image.cropRect
            val format = image.format
            val width = crop.width()
            val height = crop.height()
            val planes = image.planes
            val data = ByteArray(width * height * ImageFormat.getBitsPerPixel(format) / 8)
            val rowData = ByteArray(planes[0].rowStride)

            var channelOffset = 0
            var outputStride = 1
            for (i in planes.indices) {
                when (i) {
                    0 -> {
                        channelOffset = 0
                        outputStride = 1
                    }
                    1 -> {
                        channelOffset = width * height + 1
                        outputStride = 2
                    }
                    2 -> {
                        channelOffset = width * height
                        outputStride = 2
                    }
                }

                val buffer = planes[i].buffer
                val rowStride = planes[i].rowStride
                val pixelStride = planes[i].pixelStride

                val shift = if (i == 0) 0 else 1
                val w = width shr shift
                val h = height shr shift
                buffer.position(rowStride * (crop.top shr shift) + pixelStride * (crop.left shr shift))
                for (row in 0 until h) {
                    val length: Int
                    if (pixelStride == 1 && outputStride == 1) {
                        length = w
                        buffer.get(data, channelOffset, length)
                        channelOffset += length
                    } else {
                        length = (w - 1) * pixelStride + 1
                        buffer.get(rowData, 0, length)
                        for (col in 0 until w) {
                            data[channelOffset] = rowData[col * pixelStride]
                            channelOffset += outputStride
                        }
                    }
                    if (row < h - 1) {
                        buffer.position(buffer.position() + rowStride - length)
                    }
                }
            }
            return data
        }
    }
}
