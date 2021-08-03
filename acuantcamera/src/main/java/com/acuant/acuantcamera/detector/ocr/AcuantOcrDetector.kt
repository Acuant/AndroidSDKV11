package com.acuant.acuantcamera.detector.ocr

import android.content.Context
import com.acuant.acuantimagepreparation.AcuantImagePreparation
import java.lang.Exception
import android.graphics.*
import com.acuant.acuantcommon.model.Image
import android.graphics.Bitmap.CompressFormat
import com.acuant.acuantcamera.detector.BaseAcuantDetector
import com.acuant.acuantimagepreparation.model.DetectData
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import java.io.FileOutputStream


class AcuantOcrDetector(context: Context, private val callback: AcuantOcrDetectorHandler): BaseAcuantDetector() {
    private val textRecognizer = TessBaseAPI()
    private var isInitialized = true
    private var extStorage : String? = null
    private var tryFlip = false

    init {
        extStorage = context.getExternalFilesDir(null)?.absolutePath
        textRecognizer.init(extStorage, "ocrb")
        textRecognizer.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890<")
    }

    @Suppress("ConstantConditionIf")
    override fun detect(bitmap: Bitmap?) {
        var processedImg : Bitmap? = null
        var processedImgWithBorder : Bitmap? = null
        var processedImgFinal : Bitmap? = null
        var croppedImage : Image? = null
        if (isInitialized && bitmap != null && !isProcessing) {
            try {
                isProcessing = true
                val data = DetectData(bitmap)

                croppedImage = AcuantImagePreparation.detectMrz(data)

                if (croppedImage.isPassport) {

                    croppedImage = AcuantImagePreparation.cropMrz(data, croppedImage)

                    callback.onPointsDetected(croppedImage.points)

                    processedImg = AcuantImagePreparation.threshold(croppedImage.image)

                    if (saveDebug) {
                        try {
                            var output = FileOutputStream(File(extStorage, "preThresh.jpg"))
                            croppedImage.image.compress(CompressFormat.JPEG, 100, output)
                            output.flush()
                            output.close()
                            output = FileOutputStream(File(extStorage, "postThresh.jpg"))
                            processedImg.compress(CompressFormat.JPEG, 100, output)
                            output.flush()
                            output.close()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    processedImgWithBorder = addWhiteBorder(processedImg)
                    //TODO: this method could use improvement, it is too brute force of a solution
                    processedImgFinal = if (tryFlip) {
                        val matrix = Matrix()
                        matrix.postRotate(180f)
                        tryFlip = false
                        Bitmap.createBitmap(processedImgWithBorder, 0, 0, processedImgWithBorder.width, processedImgWithBorder.height, matrix, true)
                    } else {
                        tryFlip = true
                        processedImgWithBorder
                    }
                    textRecognizer.setImage(processedImgFinal)

                    val mrz = textRecognizer.utF8Text

                    if (!mrz.isBlank()) {
                        callback.onOcrDetected(mrz)
                    }
                    else {
                        callback.onOcrDetected("")
                    }
                } else {
                    callback.onOcrDetected(null)
                    callback.onPointsDetected(null)
                }
            } catch (e:Exception) {
                callback.onOcrDetected(null)
                callback.onPointsDetected(null)
                e.printStackTrace()
            } finally {
                processedImg?.recycle()
                processedImgWithBorder?.recycle()
                processedImgFinal?.recycle()
                croppedImage?.image?.recycle()
            }
        }
    }

    private fun addWhiteBorder(bmp: Bitmap, borderSize: Int = 10): Bitmap {
        val bmpWithBorder = Bitmap.createBitmap(bmp.width + borderSize * 2, bmp.height + borderSize * 2, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmpWithBorder)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(bmp, borderSize.toFloat(), borderSize.toFloat(), null)
        return bmpWithBorder
    }

    override fun clean() {
        super.clean()
        if (isInitialized) {
            isInitialized = false
            textRecognizer.end()
        }
    }

    companion object {
        private const val saveDebug = false
    }
}