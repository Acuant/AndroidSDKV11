package com.acuant.acuantfacecapture.detector

import android.graphics.Rect
import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

typealias FaceFrameListener = (boundingBox: Rect?, state: FaceState) -> Unit

enum class FaceState {
    NoFace, FaceTooFar, FaceTooClose, FaceAngled, EyesClosed, GoodFace
}

class FaceFrameAnalyzer internal constructor(private val listener: FaceFrameListener) : ImageAnalysis.Analyzer {

    private var running = false

    //this is experimental annotation is required due to the weird ownership of the internal image
    // essentially it is unsafe to close the image wrapped by the ImageProxy and by adding this
    // annotation we acknowledge that
    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(image: ImageProxy) {
        if (running) {
            image.close()
            return
        }
        var faceState = FaceState.NoFace
        var detectedBounds: Rect? = null
        running = true

        val mediaImage = image.image //don't close this one
        if (mediaImage != null) {
            val inputImage = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
            faceDetector.process(inputImage)
                .addOnSuccessListener { faces ->
                    if (faces.isNotEmpty()) {
                        val face = faces[0]
                        if (face != null) {
                            val origSize = Size(inputImage.width, inputImage.height)
                            var bounds = face.boundingBox
                            //this is being done because faces seem to be detected as squares,
                            // this just takes the width and cuts it down a bit to make it more
                            // like the shape of a face
                            bounds = Rect((bounds.centerX() - bounds.width() * 0.5f * 0.85f).toInt(), bounds.top, (bounds.centerX() + bounds.width() * 0.5f * 0.85f).toInt(), bounds.bottom)
                            detectedBounds = bounds
                            val sizeRatio = sizeRatio(bounds, origSize)
                            val rotY = face.headEulerAngleY
                            val rotZ = face.headEulerAngleZ

                            when {
                                sizeRatio < TOO_FAR_THRESH -> {
                                    faceState = FaceState.FaceTooFar
                                }
                                sizeRatio > TOO_CLOSE_THRESH -> {
                                    faceState = FaceState.FaceTooClose
                                }
                                abs(rotY) > Y_ROT_ANGLE || abs(rotZ) > Z_ROT_ANGLE -> {
                                    faceState = FaceState.FaceAngled
                                }
                                face.rightEyeOpenProbability ?: 1f < EYE_CLOSED_THRESHOLD && face.leftEyeOpenProbability ?: 1f < EYE_CLOSED_THRESHOLD -> {
                                    faceState = FaceState.EyesClosed
                                }
                                else -> {
                                    faceState = FaceState.GoodFace
                                }
                            }
                        }
                    }
                }
//                .addOnFailureListener { e -> //catch
//                }
                .addOnCompleteListener { //finally
                    listener(detectedBounds, faceState)
                    running = false
                    image.close()
                }
        } else {
            running = false
            listener(detectedBounds, faceState)
            image.close()
        }
    }

    companion object {
        private val faceDetector = FaceDetection.getClient(FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.5f)
            .build())

        private const val TOO_CLOSE_THRESH = 0.815f
        private const val TOO_FAR_THRESH = 0.685f
        private const val EYE_CLOSED_THRESHOLD = 0.3f
        private const val Y_ROT_ANGLE = 10
        private const val Z_ROT_ANGLE = 15

        private fun sizeRatio(bounds: Rect, imgSize: Size): Float {

            val shortSide = min(bounds.width(), bounds.height())
            val largeSide = max(bounds.width(), bounds.height())
            val screenShortSide = min(imgSize.width, imgSize.height).toFloat()
            val screenLargeSide = max(imgSize.width, imgSize.height).toFloat()

            return max(shortSide/screenShortSide, largeSide/screenLargeSide)
        }
    }
}