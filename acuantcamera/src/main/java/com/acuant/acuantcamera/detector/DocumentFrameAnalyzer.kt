package com.acuant.acuantcamera.detector

import android.graphics.*
import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.acuant.acuantcamera.constant.MINIMUM_DPI
import com.acuant.acuantimagepreparation.helper.ImageUtils
import com.acuant.acuantcamera.helper.PointsUtils
import com.acuant.acuantimagepreparation.AcuantImagePreparation
import com.acuant.acuantimagepreparation.model.DetectData
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlin.concurrent.thread

typealias DocumentFrameListener = (points: Array<Point>?, state: DocumentState, barcode: String?, detectTime: Long) -> Unit

enum class DocumentState { NoDocument, TooFar, TooClose, GoodDocument }

class DocumentFrameAnalyzer internal constructor(private val listener: DocumentFrameListener) : ImageAnalysis.Analyzer {

    private var runningThreads = 0
    private var disableDocDetect = false

    fun disableDocumentDetection() {
        disableDocDetect = true
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
        val mediaImage = image.image //don't close this one
        runningThreads = 0
        if (mediaImage != null) {
            runningThreads = if (disableDocDetect) 1 else 2

            if (!disableDocDetect) {
                val bitmap: Bitmap = ImageUtils.imageToBitmapWithoutClosingSource(mediaImage)

                //document detection
                thread {
                    val origSize = Size(bitmap.width, bitmap.height)
                    val detectData = DetectData(bitmap)
                    val detectResult = AcuantImagePreparation.detect(detectData)
                    points = detectResult.points
                    state = when {
                        points == null || detectResult.dpi < MINIMUM_DPI || !detectResult.isCorrectAspectRatio -> DocumentState.NoDocument
                        PointsUtils.isTooClose(points, origSize, MAX_DIST) -> DocumentState.TooClose
                        !PointsUtils.isCloseEnough(points, origSize, MIN_DIST) -> DocumentState.TooFar
                        else -> DocumentState.GoodDocument
                    }
                    finishThread(points, state, barcode, startTime)
                }
            }

            //barcode detection
            val barcodeInput =
                InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
            barcodeScanner.process(barcodeInput).addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    barcode = barcodes[0].rawValue
                }
            }.addOnCompleteListener { //finally
                finishThread(points, state, barcode, startTime)
                image.close()
            }
        } else {
            finishThread(points, state, barcode, startTime)
            image.close()
        }
    }

    private fun finishThread(points: Array<Point>?, state: DocumentState, barcode: String?, startTime: Long) {
        --runningThreads
        if (runningThreads <= 0) {
            listener(points, state, barcode, System.currentTimeMillis() - startTime)
        }
    }

    companion object {
        private const val MIN_DIST = 0.75f
        private const val MAX_DIST = 0.9f

        private val barcodeScanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_PDF417)
                .build())
    }
}