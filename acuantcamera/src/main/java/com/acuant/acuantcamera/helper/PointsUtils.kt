package com.acuant.acuantcamera.helper

import android.graphics.Point
import android.util.Log
import android.util.Size
import android.view.ViewGroup
import com.acuant.acuantcamera.overlay.BaseRectangleView
import kotlin.math.*

object PointsUtils {

    internal fun isAligned(points: Array<Point>?) : Boolean {
        if (points == null || points.size != 4)
            return false
        val val1 = distance(points[0], points[2])
        val val2 = distance(points[1], points[3])
        return abs(val1 - val2) < 15
    }

    internal fun isTooClose(points: Array<Point>?, screenSize: Size, maxDist: Float): Boolean {
        if (points != null) {
            val shortSide = min(
                distance(points[0], points[1]),
                distance(points[0], points[3])
            )
            val largeSide = max(
                distance(points[0], points[1]),
                distance(points[0], points[3])
            )
            val screenShortSide = min(screenSize.width, screenSize.height).toFloat()
            val screenLargeSide = max(screenSize.width, screenSize.height).toFloat()

            if (shortSide > maxDist * screenShortSide || largeSide > maxDist * screenLargeSide) {
                return true
            }
        }
        return false
    }

    internal fun isCloseEnough(points: Array<Point>?, screenSize: Size, minDist: Float): Boolean {
        if (points != null) {
            val shortSide = min(distance(points[0], points[1]), distance(points[0], points[3]))
            val largeSide = max(distance(points[0], points[1]), distance(points[0], points[3]))
            val screenShortSide = min(screenSize.width, screenSize.height).toFloat()
            val screenLargeSide = max(screenSize.width, screenSize.height).toFloat()

            if (shortSide > minDist * screenShortSide || largeSide > minDist * screenLargeSide) {
                return true
            }
        }
        return false
    }

    internal fun distance(pointA: Point, pointB: Point): Float {
        return sqrt( (pointA.x - pointB.x).toFloat().pow(2) + (pointA.y - pointB.y).toFloat().pow(2))
    }

    internal fun fixPoints(points: Array<Point>) : Array<Point> {
        if (points.size == 4) {
            if(points[0].y > points[2].y && points[0].x < points[2].x) {
                //rotate 2
                var tmp = points[0]
                points[0] = points[2]
                points[2] = tmp
                tmp = points[1]
                points[1] = points[3]
                points[3] = tmp
            } else if (points[0].y > points[2].y && points[0].x > points[2].x) {
                //rotate 3
                val tmp = points[0]
                points[0] = points[1]
                points[1] = points[2]
                points[2] = points[3]
                points[3] = tmp
            } else if (points[0].y < points[2].y && points[0].x < points[2].x) {
                //rotate 1
                val tmp = points[0]
                points[0] = points[3]
                points[3] = points[2]
                points[2] = points[1]
                points[1] = tmp
            }
        }
        return points
    }

    internal fun scalePoints(points: Array<Point>, camContainer: ViewGroup?, analyzerSize: Size?, previewSize: ViewGroup?, rectangleView: BaseRectangleView?) : Array<Point> {
        if (camContainer != null && previewSize != null && analyzerSize != null) {

            val offset = ((camContainer.height - previewSize.height) / 2 )
            val scaledPointY: Float = previewSize.height.toFloat() / analyzerSize.width.toFloat()
            val scaledPointX: Float = previewSize.width.toFloat() / analyzerSize.height.toFloat()
            rectangleView?.setWidth(camContainer.width.toFloat())

            points.onEach {
                it.x = (it.x * scaledPointX).toInt()
                it.y = (it.y * scaledPointY).toInt()
                it.x += offset
            }
        }

        return points
    }

    fun correctDirection(points: Array<Point>?, view: ViewGroup?): Boolean {
        if (view == null || points == null || points.size != 4)
            return false
        return if (view.width > view.height) {
            distance(points[0], points[1]) > distance(points[0], points[3])
        } else {
            distance(points[0], points[1]) < distance(points[0], points[3])
        }
    }
}