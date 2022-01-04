package com.acuant.acuantcamera.overlay

import android.content.Context
import android.util.AttributeSet
import com.acuant.acuantcamera.camera.mrz.MrzCameraState

class MrzRectangleView(context: Context, attr: AttributeSet?) : BaseRectangleView(context, attr) {

    fun setViewFromState(state: MrzCameraState) {
        when (state) {
            MrzCameraState.Align -> {
                setDrawBox(false)
                animateTarget = false
                paintBracket.color = paintColorBracketAlign
                paint.color = paintColorAlign
            }
            MrzCameraState.Trying -> {
                setDrawBox(true)
                animateTarget = true
                paintBracket.color = paintColorBracketHold
                paint.color = paintColorHold
            }
            MrzCameraState.Capturing -> {
                setDrawBox(true)
                animateTarget = true
                paintBracket.color = paintColorBracketCapturing
                paint.color = paintColorCapturing
            }
            else -> { //Move Closer or Reposition
                setDrawBox(true)
                animateTarget = true
                paintBracket.color = paintColorBracketCloser
                paint.color = paintColorCloser
            }
        }
        visibility = VISIBLE
    }
}