package com.acuant.acuantcamera.overlay

import android.content.Context
import android.graphics.Point
import android.util.AttributeSet
import com.acuant.acuantcamera.camera.AcuantBaseCameraFragment

class DocRectangleView(context: Context, attr: AttributeSet?) : BaseRectangleView(context, attr) {

    override fun setViewFromState(state: AcuantBaseCameraFragment.CameraState) {
        when(state) {
            AcuantBaseCameraFragment.CameraState.MoveCloser -> {
                setDrawBox(false)
                paint.color = paintColorCloser
                paintBracket.color = paintColorBracketCloser
                animateTarget = false
            }
            AcuantBaseCameraFragment.CameraState.NotInFrame -> {
                setDrawBox(false)
                paint.color = paintColorCloser
                paintBracket.color = paintColorBracketCloser
                animateTarget = false
            }
            AcuantBaseCameraFragment.CameraState.Hold -> {
                setDrawBox(true)
                paint.color = paintColorHold
                paintBracket.color = paintColorBracketHold
                animateTarget = true
            }
            AcuantBaseCameraFragment.CameraState.Steady -> {
                setDrawBox(true)
                paint.color = paintColorHold
                paintBracket.color = paintColorBracketHold
                animateTarget = true
            }
            AcuantBaseCameraFragment.CameraState.Capturing -> {
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

    companion object {

        internal fun fixPoints(points: Array<Point>) : Array<Point> {
            val fixedPoints = points.copyOf()
            if(fixedPoints.size == 4) {
                if(fixedPoints[0].y > fixedPoints[2].y && fixedPoints[0].x < fixedPoints[2].x) {
                    //rotate 2
                    var tmp = fixedPoints[0]
                    fixedPoints[0] = fixedPoints[2]
                    fixedPoints[2] = tmp

                    tmp = fixedPoints[1]
                    fixedPoints[1] = fixedPoints[3]
                    fixedPoints[3] = tmp

                } else if(fixedPoints[0].y > fixedPoints[2].y && fixedPoints[0].x > fixedPoints[2].x) {
                    //rotate 3
                    val tmp = fixedPoints[0]
                    fixedPoints[0] = fixedPoints[1]
                    fixedPoints[1] = fixedPoints[2]
                    fixedPoints[2] = fixedPoints[3]
                    fixedPoints[3] = tmp

                } else if(fixedPoints[0].y < fixedPoints[2].y && fixedPoints[0].x < fixedPoints[2].x) {
                    //rotate 1
                    val tmp = fixedPoints[0]
                    fixedPoints[0] = fixedPoints[3]
                    fixedPoints[3] = fixedPoints[2]
                    fixedPoints[2] = fixedPoints[1]
                    fixedPoints[1] = tmp

                }
            }
            return fixedPoints
        }
    }

}