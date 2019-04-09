package com.acuant.acuantcamera.helper

import android.graphics.*
import android.media.Image
import android.util.Log
import java.io.ByteArrayOutputStream

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*

/**
 * Saves a JPEG [Image] into the specified [File].
 */
internal interface ImageSaveHandler{
    fun onSave()
}

internal class ImageSaver(
        /**
         * The JPEG image
         */
        private val image: Image,

        /**
         * The file we save the image into.
         */
        private val file: File,
        private val callback: ImageSaveHandler

) : Runnable {

    override fun run() {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        var output: FileOutputStream? = null
        try {
            output = FileOutputStream(file).apply {
                write(bytes)
            }
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
        private val TAG = "ImageSaver"

        @JvmStatic fun imageToByteArray(image: Image, quality: Int = 50): ByteArray {
            val ib = convertYUV420ToN21(image, false)

            val yuvImage =  YuvImage(ib!!.array(), ImageFormat.NV21, image.width, image.height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), quality, out)
            return out.toByteArray()
        }

        private fun getRawCopy(buff: ByteBuffer) : ByteArray {
            val rawCopy = ByteBuffer.allocate(buff.capacity())
            rawCopy.put(buff)
            return rawCopy.array()
        }

        private fun convertYUV420ToN21(imgYUV420: Image, grayscale: Boolean): ByteBuffer? {

            val yPlane = imgYUV420.planes[0]
            val yData = getRawCopy(yPlane.buffer)

            val uPlane = imgYUV420.planes[1]
            val uData = getRawCopy(uPlane.buffer)

            val vPlane = imgYUV420.planes[2]
            val vData = getRawCopy(vPlane.buffer)

            // NV21 stores a full frame luma (y) and half frame chroma (u,v), so total size is
            // size(y) + size(y) / 2 + size(y) / 2 = size(y) + size(y) / 2 * 2 = size(y) + size(y) = 2 * size(y)
            val npix = imgYUV420.width * imgYUV420.height
            val nv21Image = ByteArray(npix * 2)
            Arrays.fill(nv21Image, 127.toByte()) // 127 -> 0 chroma (luma will be overwritten in either case)

            try{
                val nv21Buffer = ByteBuffer.wrap(nv21Image)
                for(i in 0 until imgYUV420.height) {
                    nv21Buffer.put(yData, i * yPlane.rowStride, imgYUV420.width);
                }
                // Copy the u and v planes interlaced
                if(!grayscale) {
                    for (row in 0 until (imgYUV420.height / 2)) {
                        var upix = 0
                        var vpix = 0
                        for(cnt in 0 until imgYUV420.width / 2){
                            nv21Buffer.put(uData[row * uPlane.rowStride + upix])
                            nv21Buffer.put(vData[row * vPlane.rowStride + vpix])
                            upix += uPlane.pixelStride
                            vpix += vPlane.pixelStride
                        }
                    }
                }

                return nv21Buffer;
            }
            catch(e:Exception){
                e.printStackTrace()
            }
            return null
        }
    }
}
