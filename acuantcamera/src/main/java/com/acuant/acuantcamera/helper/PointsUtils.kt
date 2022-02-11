package com.acuant.acuantcamera.helper

import android.graphics.Point
import android.util.Size
import android.view.ViewGroup
import com.acuant.acuantcamera.constant.TARGET_DPI
import com.acuant.acuantcamera.overlay.BaseRectangleView
import kotlin.math.*

object PointsUtils {

    internal fun findMiddleForCamera(points: Array<Point>?, width: Int?, height: Int?): Point {
        //the coordinate system in the camera is inverted hence the subtraction from width and height
        if (points == null || points.size != 4 || width == null || height == null)
            return Point()
        return Point(width - intArrayOf(points[0].y, points[1].y, points[2].y, points[3].y).average().toInt(), height - intArrayOf(points[0].x, points[1].x, points[2].x, points[3].x).average().toInt())
    }

    internal fun isAligned(points: Array<Point>?) : Boolean {
        if (points == null || points.size != 4)
            return false
        val val1 = distance(points[0], points[2])
        val val2 = distance(points[1], points[3])
        return abs(val1 - val2) < 15
    }

    internal fun isNotTooClose(points: Array<Point>?, screenSize: Size, maxDist: Float): Boolean {
        if (points != null) {
            val ratio = getLargeRatio(points, screenSize)
            return ratio < maxDist
        }
        return false
    }

    internal fun isCloseEnough(points: Array<Point>?, screenSize: Size, minDist: Float): Boolean {
        if (points != null) {
            val ratio = getLargeRatio(points, screenSize)
            return ratio > minDist
        }
        return false
    }

    internal fun scaleDpi(dpi: Int, analyzerSize: Size?, captureSize: Size?): Int {
        if (analyzerSize == null || captureSize == null)
            return dpi
        return min((dpi * max(captureSize.width, captureSize.height).toFloat() / max(analyzerSize.width, analyzerSize.height)).toInt(), TARGET_DPI + 1)
    }

    internal fun getLargeRatio(points: Array<Point>, screenSize: Size): Float {
        val shortSide = min(distance(points[0], points[1]), distance(points[0], points[3]))
        val largeSide = max(distance(points[0], points[1]), distance(points[0], points[3]))
        val screenShortSide = min(screenSize.width, screenSize.height).toFloat()
        val screenLargeSide = max(screenSize.width, screenSize.height).toFloat()

        return max(shortSide / screenShortSide, largeSide / screenLargeSide)
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

            val scale: Float = previewSize.height.toFloat() / analyzerSize.width.toFloat()
            val yOffset: Int = ((previewSize.width.toFloat() - analyzerSize.height * scale) / 2).toInt()
            val xOffset: Int = ((camContainer.height - previewSize.height) / 2)

            rectangleView?.setWidth(camContainer.width.toFloat())

            points.onEach {
                it.x = (it.x * scale).toInt()
                it.y = (it.y * scale).toInt()
                it.x += xOffset
                it.y += yOffset
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