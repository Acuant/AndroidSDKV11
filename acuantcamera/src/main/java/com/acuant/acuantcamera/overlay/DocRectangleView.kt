package com.acuant.acuantcamera.overlay

import android.content.Context
import android.util.AttributeSet
import com.acuant.acuantcamera.camera.document.DocumentCameraState

class DocRectangleView(context: Context, attr: AttributeSet?) : BaseRectangleView(context, attr) {
    fun setViewFromState(state: DocumentCameraState) {
        when(state) {
            DocumentCameraState.MoveCloser -> {
                setDrawBox(false)
                paint.color = paintColorCloser
                paintBracket.color = paintColorBracketCloser
                animateTarget = false
            }
            DocumentCameraState.MoveBack -> {
                setDrawBox(false)
                paint.color = paintColorCloser
                paintBracket.color = paintColorBracketCloser
                animateTarget = false
            }
            DocumentCameraState.CountingDown -> {
                setDrawBox(true)
                paint.color = paintColorHold
                paintBracket.color = paintColorBracketHold
                animateTarget = true
            }
            DocumentCameraState.HoldSteady -> {
                setDrawBox(true)
                paint.color = paintColorHold
                paintBracket.color = paintColorBracketHold
                animateTarget = true
            }
            DocumentCameraState.Capturing -> {
                setDrawBox(true)
                paint.color = paintColorCapturing
                paintBracket.color = paintColorBracketCapturing
                animateTarget = true
            }
            else -> {//align
                setDrawBox(false)
                paint.color = paintColorAlign
                paintBracket.color = paintColorBracketAlign
                animateTarget = false
            }
        }
    }
}