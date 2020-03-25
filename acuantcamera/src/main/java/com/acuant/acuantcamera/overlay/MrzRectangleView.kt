package com.acuant.acuantcamera.overlay

import android.content.Context
import android.graphics.Point
import android.util.AttributeSet
import android.util.Log
import com.acuant.acuantcamera.camera.AcuantBaseCameraFragment

class MrzRectangleView(context: Context, attr: AttributeSet?) : BaseRectangleView(context, attr) {

    override fun setViewFromState(state: AcuantBaseCameraFragment.CameraState) {

        Log.d("CamStateView", state.toString())
        when(state) {
            AcuantBaseCameraFragment.CameraState.MrzNone -> {
                setDrawBox(false)
                paint.color = paintColorAlign
                paintBracket.color = paintColorBracketAlign
                animateTarget = false
            }
            AcuantBaseCameraFragment.CameraState.MrzAlign -> {
                setDrawBox(false)
                paintBracket.color = paintColorBracketAlign
                paint.color = paintColorAlign
                animateTarget = false
            }
            else -> {
                setDrawBox(true)
                when (state) {
                    AcuantBaseCameraFragment.CameraState.MrzTrying -> {
                        paintBracket.color = paintColorBracketHold
                        paint.color = paintColorHold
                    }
                    AcuantBaseCameraFragment.CameraState.MrzCapturing -> {
                        paintBracket.color = paintColorBracketCapturing
                        paint.color = paintColorCapturing
                    }
                    else -> {
                        paintBracket.color = paintColorBracketCloser
                        paint.color = paintColorCloser
                    }
                }
                animateTarget = true
            }
        }
        visibility = VISIBLE
    }

    companion object {
        fun fixPoints(points: Array<Point>?) {
            if(points != null && points.size == 4) {
                if(points[0].y > points[2].y && points[0].x < points[2].x) {
                    //rotate 2
                    var tmp = points[0]
                    points[0] = points[2]
                    points[2] = tmp

                    tmp = points[1]
                    points[1] = points[3]
                    points[3] = tmp

                } else if(points[0].y > points[2].y && points[0].x > points[2].x) {
                    //rotate 3
                    val tmp = points[0]
                    points[0] = points[1]
                    points[1] = points[2]
                    points[2] = points[3]
                    points[3] = tmp

                } else if(points[0].y < points[2].y && points[0].x < points[2].x) {
                    //rotate 1
                    val tmp = points[0]
                    points[0] = points[3]
                    points[3] = points[2]
                    points[2] = points[1]
                    points[1] = tmp

                }
            }
        }
    }
}