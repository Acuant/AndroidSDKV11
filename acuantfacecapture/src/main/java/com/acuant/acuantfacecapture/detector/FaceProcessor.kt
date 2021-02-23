package com.acuant.acuantfacecapture.detector

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.PointF
import android.os.Handler
import com.acuant.acuantfacecapture.model.FaceDetailState
import com.acuant.acuantfacecapture.model.FaceDetails
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.Detector.Detections
import com.google.android.gms.vision.Tracker
import com.google.android.gms.vision.face.Face
import com.google.android.gms.vision.face.LargestFaceFocusingProcessor
import java.lang.ref.WeakReference
import java.util.*
import kotlin.math.abs
import kotlin.math.min

class FaceProcessor private constructor(context: Context, listener: FaceListener, private val countdown: Int) : Tracker<Face>() {
    private val liveFaceListener: FaceListener?
    private val tracker: Tracker<Face>
    private val context: WeakReference<Context>
    private var faceDetector: FaceDetector? = null
    private var openEyeFrame: Bitmap? = null
    // Record the previously seen proportions of the landmark locations relative to the bounding box
// of the face.  These proportions can be used to approximate where the landmarks are within the
// face bounding box if the eye landmark is missing in a future update.
    private val previousProportions: MutableMap<Int, PointF> = HashMap()
    // Similarly, keep track of the previous eye open state so that it can be reused for
// intermediate frames which lack eye landmarks and corresponding eye state.
    private var previousIsLeftOpen = true
    private var previousIsRightOpen = true
    private var lastFacePosition: Point? = null
    private var startedCapture = false
    private var handler: Handler? = null
    private var captureProgress = -1
    //==============================================================================================
// Detector
//==============================================================================================
    /**
     * Creates the face detector and associated processing pipeline to support either front facing
     * mode or rear facing mode.  Checks if the detector is ready to use, and displays a low storage
     * warning if it was not possible to download the face library.
     */
    private fun createFaceDetector(): FaceDetector { // For both front facing and rear facing modes, the detector is initialized to do landmark
        // detection (to find the eyes), classification (to determine if the eyes are open), and
        // tracking.
        //
        // Use of "fast mode" enables faster detection for frontward faces, at the expense of not
        // attempting to detect faces at more varied angles (e.g., faces in profile).  Therefore,
        // faces that are turned too far won't be detected under fast mode.
        //
        // For front facing mode only, the detector will use the "prominent face only" setting,
        // which is optimized for tracking a single relatively large face.  This setting allows the
        // detector to take some shortcuts to make tracking faster, at the expense of not being able
        // to track multiple faces.
        //
        // Setting the minimum face size not only controls how large faces must be in order to be
        // detected, it also affects performance.  Since it takes longer to scan for smaller faces,
        // we increase the minimum face size for the rear facing mode a little bit in order to make
        // tracking faster (at the expense of missing smaller faces).  But this optimization is less
        // important for the front facing case, because when "prominent face only" is enabled, the
        // detector stops scanning for faces after it has found the first (large) face.
        val detector = com.google.android.gms.vision.face.FaceDetector.Builder(context.get())
                .setLandmarkType(com.google.android.gms.vision.face.FaceDetector.NO_LANDMARKS)
                .setClassificationType(com.google.android.gms.vision.face.FaceDetector.ALL_CLASSIFICATIONS)
                .setTrackingEnabled(true)
                .setMode(com.google.android.gms.vision.face.FaceDetector.ACCURATE_MODE)
                .setProminentFaceOnly(true)
                .setMinFaceSize(0.6f)
                .build()
        val processor: Detector.Processor<Face>
        // For front facing mode, a single tracker instance is used with an associated focusing
        // processor.  This configuration allows the face detector to take some shortcuts to
        // speed up detection, in that it can quit after finding a single face and can assume
        // that the nextIrisPosition face position is usually relatively close to the last seen
        // face position.
        processor = LargestFaceFocusingProcessor.Builder(detector, tracker).build()
        //detector.setProcessor(processor);
        faceDetector = FaceDetector(detector)
        faceDetector!!.setProcessor(processor)
        if (handler != null) {
            handler!!.removeCallbacksAndMessages(null)
        } else {
            handler = Handler()
        }
//        if (!detector.isOperational) {
//            // Note: The first time that an app using face API is installed on a device, GMS will
//            // download a native library to the device in order to do detection.  Usually this
//            // completes before the app is run for the first time.  But if that download has not yet
//            // completed, then the above call will not detect any faces.
//            //
//            // isOperational() can be used to check if the required native library is currently
//            // available.  The detector will automatically become operational once the library
//            // download completes on device.
//            // Check for low storage.  If there is low storage, the native library will not be
//            // downloaded, so detection will not become operational.
////            val lowStorageFilter = IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW)
////            val hasLowStorage = context.get()?.registerReceiver(null,
////                    lowStorageFilter) != null
////            if (hasLowStorage) {
////                //TODO: Handle low memory error here
////            }
//        }
        return faceDetector as FaceDetector
    }

    override fun onNewItem(id: Int, face: Face) {}
    private fun isGoodSizeFace(x: Float, y: Float, width: Float, height: Float,
                               screenWidth: Float, screenHeight: Float, face: Face?): FaceDetailState {
        val ratioToEdges = min(1-height/screenHeight, 1-width/screenWidth)
        //Log.d("ratios", ratioToEdges.toString())
        @Suppress("unused")
        return if (ratioToEdges < TOO_CLOSE_THRESH) {
            FaceDetailState.FACE_TOO_CLOSE
        } else if (ratioToEdges > TOO_FAR_THRESH) {
            FaceDetailState.FACE_TOO_FAR
        } else if (!isInBounds(x, y, width, height, screenWidth, screenHeight)) {
            FaceDetailState.FACE_NOT_IN_FRAME
        } else if (face != null && (abs(face.eulerY) > 15 || abs(face.eulerZ) > 10)) {
            FaceDetailState.FACE_ANGLE_TOO_SKEWED
        } else if (didFaceMove(x.toInt(), y.toInt())) {
            FaceDetailState.FACE_MOVED
        } else {
            FaceDetailState.FACE_GOOD_DISTANCE
        }
    }

    private fun didFaceMove(x: Int, y: Int): Boolean {
        var didFaceMove = false
        if (lastFacePosition != null) {
            val moveThreshold = 10
            didFaceMove = lastFacePosition!!.x < x - moveThreshold || lastFacePosition!!.x > x + moveThreshold || lastFacePosition!!.y < y - moveThreshold || lastFacePosition!!.y > y + moveThreshold
        }
        lastFacePosition = Point(x, y)
        return didFaceMove
    }

    override fun onUpdate(detectionResults: Detections<Face>, face: Face) {
        updatePreviousProportions(face)
        val leftOpenScore = face.isLeftEyeOpenProbability
        val isLeftOpen: Boolean
        if (leftOpenScore == Face.UNCOMPUTED_PROBABILITY) {
            isLeftOpen = previousIsLeftOpen
        } else {
            isLeftOpen = leftOpenScore > EYE_CLOSED_THRESHOLD
            previousIsLeftOpen = isLeftOpen
        }
        val rightOpenScore = face.isRightEyeOpenProbability
        val isRightOpen: Boolean
        if (rightOpenScore == Face.UNCOMPUTED_PROBABILITY) {
            isRightOpen = previousIsRightOpen
        } else {
            isRightOpen = rightOpenScore > EYE_CLOSED_THRESHOLD
            previousIsRightOpen = isRightOpen
        }
        if (liveFaceListener != null) {
            if (isLeftOpen && isRightOpen) {
                openEyeFrame = faceDetector!!.frame
            }
            var state = FaceDetailState.NONE
            if (openEyeFrame != null) {
                state = isGoodSizeFace(face.position.x, face.position.y, face.width,
                        face.height, openEyeFrame!!.width.toFloat(), openEyeFrame!!.height.toFloat(), face)
            }
            if (state == FaceDetailState.FACE_GOOD_DISTANCE) {
                val faceDetails = FaceDetails()
                faceDetails.face = face
                faceDetails.state = state
                if (!startedCapture) {
                    startedCapture = true
                    captureProgress = countdown
                    handler!!.postDelayed(object : Runnable{
                        override fun run() {
                            captureProgress -= 1
                            if (captureProgress != 0) {
                                handler!!.postDelayed(this, 1000)
                            }
                        }
                    }, 1000)
                }
                faceDetails.countdownToCapture = captureProgress
                faceDetails.image = openEyeFrame
                liveFaceListener.faceCaptured(faceDetails)
            } else {
                handler!!.removeCallbacksAndMessages(null)
                startedCapture = false
                captureProgress = -1
                val faceDetails = FaceDetails()
                faceDetails.state = state
                liveFaceListener.faceCaptured(faceDetails)
            }
        }
    }

    private fun isInBounds(x: Float, y: Float, width: Float, height: Float,
                           screenWidth: Float, screenHeight: Float): Boolean {
        return x >= 0 && x + width <= screenWidth && y >= 0 && y + height <= screenHeight
    }

    override fun onMissing(detectionResults: Detections<Face>) {
        val faceDetails = FaceDetails()
        liveFaceListener!!.faceCaptured(faceDetails)
    }

    override fun onDone() {
        val faceDetails = FaceDetails()
        liveFaceListener!!.faceCaptured(faceDetails)
    }

    @Suppress("unused")
    fun release() {
        handler!!.removeCallbacksAndMessages(null)
        faceDetector!!.release()
    }

    //==============================================================================================
    // Private
    //==============================================================================================
    private fun updatePreviousProportions(face: Face) {
        for (landmark in face.landmarks) {
            val position = landmark.position
            val posX = (position.x - face.position.x) / face.width
            val posY = (position.y - face.position.y) / face.height
            previousProportions[landmark.type] = PointF(posX, posY)
        }
    }

    companion object {
        private const val EYE_CLOSED_THRESHOLD = 0.3f
        private const val TOO_CLOSE_THRESH = 0.185f
        private const val TOO_FAR_THRESH = 0.315f

        //==============================================================================================
        // Methods
        //==============================================================================================
        fun initLiveFaceDetector(context: Context,
                                 listener: FaceListener,
                                 countdown: Int = 2): FaceDetector? {
            return FaceProcessor(context, listener, countdown).faceDetector
        }
    }

    init {
        liveFaceListener = listener
        this.context = WeakReference(context)
        tracker = this
        createFaceDetector()
    }
}