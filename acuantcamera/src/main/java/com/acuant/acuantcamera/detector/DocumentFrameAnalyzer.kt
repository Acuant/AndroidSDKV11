package com.acuant.acuantcamera.detector

import android.graphics.*
import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.acuant.acuantimagepreparation.helper.ImageUtils
import com.acuant.acuantcamera.helper.PointsUtils
import com.acuant.acuantimagepreparation.AcuantImagePreparation
import com.acuant.acuantimagepreparation.model.DetectData
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlin.concurrent.thread

typealias DocumentFrameListener = (result: DocumentFrameResult, detectTime: Long) -> Unit

class DocumentFrameAnalyzer internal constructor(private val trueScreenRatio: Float, private val listener: DocumentFrameListener) : ImageAnalysis.Analyzer {

    private var runningThreads = 0
    private var disableDocDetect = false
    private var minDist = DEFAULT_MIN_DIST
    private var maxDist = DEFAULT_MAX_DIST
    private var minDistBound = DEFAULT_MIN_DIST_BOUND
    private var maxDistBound = DEFAULT_MAX_DIST_BOUND

    fun disableDocumentDetection() {
        disableDocDetect = true
    }

    //we eventually want to set the min and max to be within the focusable distance of the camera.
    // However as of alpha12 there does not seem to be a stable way of accessing these values
    @Suppress("unused")
    fun setNewMaxDistBound(maxBound: Float) {
        maxDistBound = maxBound
        capBounds()
    }

    @Suppress("unused")
    fun setNewMinDistBound(minBound: Float) {
        minDistBound = minBound
        capBounds()
    }

    fun setNewMinDist(minDist: Float) {
        this.maxDist = minDist + ALLOWED_RATIO_VARIANCE
        this.minDist = minDist
        capBounds()
    }

    fun setNewMaxDist(maxDist: Float) {
        this.maxDist = maxDist
        this.minDist = maxDist - ALLOWED_RATIO_VARIANCE
        capBounds()
    }

    private fun capBounds() {
        if (minDist < minDistBound) {
            minDist = minDistBound
            maxDist = minDistBound + ALLOWED_RATIO_VARIANCE
        }
        if (maxDist > maxDistBound) {
            minDist = maxDistBound - ALLOWED_RATIO_VARIANCE
            maxDist = maxDistBound
        }
    }

    //this is experimental annotation is required due to the weird ownership of the internal image
    // essentially it is unsafe to close the image wrapped by the ImageProxy and by adding this
    // annotation we acknowledge that
    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(image: ImageProxy) {
        //this should not happen, just a sanity check
        if (runningThreads > 0) {
            image.close()
            return
        }
        val startTime = System.currentTimeMillis()
        var state: DocumentState = DocumentState.NoDocument
        var points: Array<Point>? = null
        var barcode: String? = null
        var documentType: DocumentType = DocumentType.Other
        var dpi = 0
        var currentDistRatio: Float? = null
        val mediaImage = image.image //don't close this one
        runningThreads = 0
        if (mediaImage != null) {
            runningThreads = if (disableDocDetect) 1 else 2

            if (!disableDocDetect) {
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
                    val detectResult = AcuantImagePreparation.detect(detectData)
                    points = detectResult.points
                    if (points != null) {
                        documentType = if (detectResult.isPassport) DocumentType.Passport else DocumentType.Id
                        dpi = detectResult.dpi
                        currentDistRatio = PointsUtils.getLargeRatio(points!!, origSize)
                    }
                    state = when {
                        points == null || !detectResult.isCorrectAspectRatio || !isParallel(points, detectAspectRatio) -> {
                            documentType = DocumentType.Other
                            DocumentState.NoDocument
                        }
                        !PointsUtils.isNotTooClose(points, origSize, maxDist) -> DocumentState.TooClose
                        !PointsUtils.isCloseEnough(points, origSize, minDist) -> DocumentState.TooFar
                        else -> DocumentState.GoodDocument
                    }
                    finishThread(points, currentDistRatio, documentType, dpi, state, barcode, startTime)
                }
            }

            //barcode detection
            val barcodeInput = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
            barcodeScanner.process(barcodeInput).addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    barcode = barcodes[0].rawValue
                }
            }.addOnCompleteListener { //finally
                finishThread(points, currentDistRatio, documentType, dpi, state, barcode, startTime)
                image.close()
            }
        } else {
            finishThread(points, currentDistRatio, documentType, dpi, state, barcode, startTime)
            image.close()
        }
    }

    private fun isParallel(points: Array<Point>?, detectAspectRatio: Float): Boolean {
        if (points == null || points.size != 4)
            return false
        val fixedPoints = PointsUtils.fixPoints(points)
        val width = PointsUtils.distance(fixedPoints[0], fixedPoints[1])
        val height = PointsUtils.distance(fixedPoints[0], fixedPoints[3])
        return if (detectAspectRatio < 1) {
            width / height >= 1
        } else {
            width / height < 1
        }
    }

    private fun finishThread(points: Array<Point>?, currentDistRatio: Float?, documentType: DocumentType, dpi: Int, state: DocumentState, barcode: String?, startTime: Long) {
        --runningThreads
        if (runningThreads <= 0) {
            val result = DocumentFrameResult(points, currentDistRatio, documentType, dpi, state, barcode)
            listener(result, System.currentTimeMillis() - startTime)
        }
    }

    companion object {
        private const val ALLOWED_RATIO_VARIANCE = 0.09f
        private const val DEFAULT_MIN_DIST = 0.71f
        private const val DEFAULT_MAX_DIST = DEFAULT_MIN_DIST + ALLOWED_RATIO_VARIANCE
        private const val DEFAULT_MIN_DIST_BOUND = 0.51f
        private const val DEFAULT_MAX_DIST_BOUND = 0.91f

        private val barcodeScanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_PDF417)
                .build())
    }
}