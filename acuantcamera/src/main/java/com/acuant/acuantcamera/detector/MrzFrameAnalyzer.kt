package com.acuant.acuantcamera.detector

import android.content.Context
import android.graphics.*
import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.acuant.acuantimagepreparation.helper.ImageUtils
import com.acuant.acuantcamera.helper.MrzParser
import com.acuant.acuantcamera.helper.MrzResult
import com.acuant.acuantcamera.helper.PointsUtils
import com.acuant.acuantimagepreparation.AcuantImagePreparation
import com.acuant.acuantimagepreparation.model.DetectData
import com.googlecode.tesseract.android.TessBaseAPI
import java.lang.ref.WeakReference
import kotlin.concurrent.thread

typealias MrzFrameListener = (points: Array<Point>?, result: MrzResult?, state: MrzState) -> Unit

enum class MrzState { NoMrz, TooFar, GoodMrz }

class MrzFrameAnalyzer internal constructor(contextWeak: WeakReference<Context>, private val trueScreenRatio: Float, private val listener: MrzFrameListener) : ImageAnalysis.Analyzer {
    private var running = false
    private var tryFlip = false
    private val textRecognizer = TessBaseAPI()
    private var internalStorage : String? = null
    init {
        val context = contextWeak.get()
        if (context != null) {
            internalStorage = context.filesDir?.absolutePath
            textRecognizer.init(internalStorage, "ocrb")
            textRecognizer.setVariable(
                TessBaseAPI.VAR_CHAR_WHITELIST,
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890<"
            )
        }
    }

    //this is experimental annotation is required due to the weird ownership of the internal image
    // essentially it is unsafe to close the image wrapped by the ImageProxy and by adding this
    // annotation we acknowledge that
    @androidx.camera.core.ExperimentalGetImage
    //must close ImageProxy in each exit path or camera will freeze
    override fun analyze(image: ImageProxy) {
        //this should not happen, just a sanity check
        if (running) {
            image.close()
            return
        }
        var state: MrzState = MrzState.NoMrz
        var points: Array<Point>? = null
        var result: MrzResult? = null
        val mediaImage = image.image //don't close this one
        running = true
        if (mediaImage != null) {

            val bitmap: Bitmap = ImageUtils.imageToBitmapWithoutClosingSource(mediaImage)

            //document detection
            thread {
                val detectAspectRatio = bitmap.width / bitmap.height.toFloat()
                val origSize = if (detectAspectRatio < trueScreenRatio) {
                    Size(bitmap.width, (bitmap.width / trueScreenRatio).toInt())
                } else {
                    Size((bitmap.height / trueScreenRatio).toInt(), bitmap.height)
                }
                val detectData = DetectData(bitmap)
                val detectResult = AcuantImagePreparation.detectMrz(detectData)
                points = detectResult.points
                state = when {
                    points == null || !PointsUtils.isAligned(points) || !isAcceptableAspectRatio(points) -> MrzState.NoMrz
                    !PointsUtils.isCloseEnough(points, origSize, MIN_DIST) -> MrzState.TooFar
                    else -> MrzState.GoodMrz
                }
                if (state == MrzState.GoodMrz) {

                    val croppedImage = AcuantImagePreparation.cropMrz(detectData, detectResult)
                    val processedImg = AcuantImagePreparation.threshold(croppedImage.image)

                    val processedImgWithBorder = ImageUtils.addWhiteBorder(processedImg)
                    //TODO: this method could use improvement, it is too brute force of a solution
                    val processedImgFinal = if (tryFlip) {
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

                    if (mrz.isNotBlank()) {
                        result = parser.parseMrz(mrz)
                    }
                }
                finishDetect(points, result, state)
            }
        } else {
            finishDetect(points, result, state)
        }

        image.close()
    }

    private fun finishDetect(points: Array<Point>?, result: MrzResult?, state: MrzState) {
        running = false
        listener(points, result, state)
    }

    companion object {
        private val parser = MrzParser()
        private const val MIN_DIST = 0.75f

        fun isAcceptableAspectRatio(points: Array<Point>?) : Boolean {
            if (points == null)
                return false
            val ratio = PointsUtils.distance(points[0], points[3]) / PointsUtils.distance(
                points[0],
                points[1]
            )
            return ratio > 4f && ratio < 10f
        }
    }
}