package com.acuant.acuantfacecapture.camera.facecapture

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.camera.core.ImageAnalysis
import com.acuant.acuantcommon.model.AcuantError
import com.acuant.acuantcommon.model.ErrorCodes
import com.acuant.acuantcommon.model.ErrorDescriptions
import com.acuant.acuantfacecapture.camera.AcuantBaseFaceCameraFragment
import com.acuant.acuantfacecapture.databinding.FaceFragmentUiBinding
import com.acuant.acuantfacecapture.detector.FaceFrameAnalyzer
import com.acuant.acuantfacecapture.detector.FaceState
import com.acuant.acuantfacecapture.helper.RectHelper
import com.acuant.acuantfacecapture.interfaces.IAcuantSavedImage
import com.acuant.acuantfacecapture.model.FaceCaptureOptions
import com.acuant.acuantfacecapture.overlays.FacialGraphic
import com.acuant.acuantfacecapture.overlays.FacialGraphicOverlay
import kotlin.math.ceil

enum class FaceCameraState {
    Align, MoveCloser, MoveBack, FixRotation, KeepSteady, Hold, Blink, Capturing
}

class AcuantFaceCaptureFragment: AcuantBaseFaceCameraFragment() {

    private var cameraUiContainerBinding: FaceFragmentUiBinding? = null
    private var mFacialGraphicOverlay: FacialGraphicOverlay? = null
    private var mFacialGraphic: FacialGraphic? = null
    private var faceImage: ImageView? = null
    private var lastFacePosition: Rect? = null
    private var startTime = System.currentTimeMillis()
    private var requireBlink = false
    private var userHasBlinked = false
    private var userHasHadOpenEyes = false
    private var lastState: FaceState = FaceState.NoFace

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraUiContainerBinding?.root?.let {
            fragmentCameraBinding!!.root.removeView(it)
        }

        cameraUiContainerBinding = FaceFragmentUiBinding.inflate(
            LayoutInflater.from(requireContext()),
            fragmentCameraBinding!!.root,
            true
        )

        mFacialGraphicOverlay = cameraUiContainerBinding?.faceOverlay
        faceImage = cameraUiContainerBinding?.blankFaceImage
        faceImage?.imageAlpha = 153

        mFacialGraphicOverlay?.setOptions(acuantOptions)
    }

    override fun onResume() {
        super.onResume()
        val facialGraphicOverlay = mFacialGraphicOverlay
        if (facialGraphicOverlay != null) {
            if (mFacialGraphic == null) {
                val facialGraphic = FacialGraphic(facialGraphicOverlay)
                facialGraphic.setOptions(acuantOptions)
                mFacialGraphic = facialGraphic
            }
            facialGraphicOverlay.add(mFacialGraphic!!)
        }
    }

    override fun onPause() {
        resetTimer()
        mFacialGraphicOverlay?.clear()
        mFacialGraphic?.updateLiveFaceDetails(null, FaceCameraState.Align)
        super.onPause()
    }

    override fun onDestroy() {
        mFacialGraphicOverlay?.clear()
        mFacialGraphicOverlay = null
        mFacialGraphic = null
        super.onDestroy()
    }

    fun changeToHGLiveness() {
        requireBlink = true
    }

    private fun resetTimer(resetBlinkState: Boolean = true) {
        if (resetBlinkState) {
            userHasBlinked = false
            userHasHadOpenEyes = false
        }
        startTime = System.currentTimeMillis()
    }

    private fun onFaceDetected(rect: Rect?, state: FaceState) {
        if (capturing || !isAdded)
            return
        val analyzerSize = imageAnalyzer?.resolutionInfo?.resolution
        val previewView = fragmentCameraBinding?.viewFinder
        val boundingBox = if (rect != null && analyzerSize != null && previewView != null) {
            mFacialGraphic?.setWidth(previewView.width)
            RectHelper.scaleRect(rect, analyzerSize, previewView)
        } else {
            null
        }
        val realState = if (previewView != null && boundingBox != null) {
            val view = Rect(previewView.left, previewView.top, previewView.right, previewView.bottom)
            if (!view.contains(boundingBox)) {
                FaceState.NoFace
            } else {
                state
            }
        } else {
            state
        }
        faceImage?.visibility = View.INVISIBLE
        when (realState) {
            FaceState.NoFace -> {
                faceImage?.visibility = View.VISIBLE
                mFacialGraphicOverlay?.setState(FaceCameraState.Align)
                mFacialGraphic?.updateLiveFaceDetails(null, FaceCameraState.Align)
                resetTimer()
            }
            FaceState.FaceTooFar -> {
                mFacialGraphicOverlay?.setState(FaceCameraState.MoveCloser)
                mFacialGraphic?.updateLiveFaceDetails(boundingBox, FaceCameraState.MoveCloser)
                resetTimer()
            }
            FaceState.FaceTooClose -> {
                mFacialGraphicOverlay?.setState(FaceCameraState.MoveBack)
                mFacialGraphic?.updateLiveFaceDetails(boundingBox, FaceCameraState.MoveBack)
                resetTimer()
            }
            FaceState.FaceAngled -> {
                mFacialGraphicOverlay?.setState(FaceCameraState.FixRotation)
                mFacialGraphic?.updateLiveFaceDetails(boundingBox, FaceCameraState.FixRotation)
                resetTimer()
            }
            else -> { //good face or closed eyes
                when {
                    didFaceMove(boundingBox, lastFacePosition) -> {
                        mFacialGraphicOverlay?.setState(FaceCameraState.KeepSteady)
                        mFacialGraphic?.updateLiveFaceDetails(boundingBox, FaceCameraState.KeepSteady)
                        resetTimer()
                    }
                    requireBlink && !userHasBlinked -> {
                        if (userHasHadOpenEyes && lastState == FaceState.EyesClosed && realState == FaceState.GoodFace) {
                            userHasBlinked = true
                        }
                        if (realState == FaceState.GoodFace) {
                            userHasHadOpenEyes = true
                        }
                        mFacialGraphicOverlay?.setState(FaceCameraState.Blink)
                        mFacialGraphic?.updateLiveFaceDetails(boundingBox, FaceCameraState.Blink)
                        resetTimer(resetBlinkState = false)
                    }
                    System.currentTimeMillis() - startTime < acuantOptions.totalCaptureTime * 1000 -> {
                        mFacialGraphicOverlay?.setState(FaceCameraState.Hold, acuantOptions.totalCaptureTime - ceil(((System.currentTimeMillis() - startTime) / 1000).toDouble()).toInt())
                        mFacialGraphic?.updateLiveFaceDetails(boundingBox, FaceCameraState.Hold)
                    }
                    else -> {
                        mFacialGraphicOverlay?.setState(FaceCameraState.Capturing)
                        mFacialGraphic?.updateLiveFaceDetails(boundingBox, FaceCameraState.Capturing)
                        if (!capturing) {
                            captureImage(object : IAcuantSavedImage {
                                override fun onSaved(uri: String) {
                                    cameraActivityListener.onCameraDone(uri)
                                }

                                override fun onError(error: AcuantError) {
                                    cameraActivityListener.onError(error)
                                }
                            })
                        }
                    }
                }
            }
        }
        lastState = realState
        lastFacePosition = boundingBox
    }

    override fun buildImageAnalyzer(screenAspectRatio: Int, rotation: Int) {
        val frameAnalyzer = try {
                FaceFrameAnalyzer { boundingBox, state ->
                    onFaceDetected(boundingBox, state)
                }
        } catch (e: IllegalStateException) {
            cameraActivityListener.onError(AcuantError(ErrorCodes.ERROR_UnexpectedError, ErrorDescriptions.ERROR_DESC_UnexpectedError, e.toString()))
            return
        }

        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, frameAnalyzer)
            }
    }

    companion object {
        private const val MOVEMENT_THRESHOLD = 22

        private fun didFaceMove(facePosition: Rect?, lastFacePosition: Rect?): Boolean {
            if (facePosition == null || lastFacePosition == null)
                return false
            return RectHelper.distance(facePosition, lastFacePosition) > MOVEMENT_THRESHOLD
        }


        @JvmStatic fun newInstance(acuantOptions: FaceCaptureOptions): AcuantFaceCaptureFragment {
            val frag = AcuantFaceCaptureFragment()
            val args = Bundle()
            args.putSerializable(INTERNAL_OPTIONS, acuantOptions)
            frag.arguments = args
            return frag
        }
    }
}