package com.acuant.acuantfacecapture.helper

import android.graphics.Point
import android.graphics.Rect
import android.util.Size
import android.view.ViewGroup
import kotlin.math.pow
import kotlin.math.sqrt

object RectHelper {
    private fun distance(pointA: Point, pointB: Point): Float {
        return sqrt( (pointA.x - pointB.x).toFloat().pow(2) + (pointA.y - pointB.y).toFloat().pow(2))
    }

    @JvmStatic fun distance(rect1: Rect, rect2: Rect): Float {
        return distance(Point(rect1.centerX(), rect1.centerY()), Point(rect2.centerX(), rect2.centerY()))
    }

    @JvmStatic fun scaleRect(rect: Rect, detectSize: Size, previewView: ViewGroup): Rect {
        val previewViewAspectRatio = previewView.height.toFloat() / previewView.width
        val detectAspectRatio = detectSize.width.toFloat() / detectSize.height
        val previewScaleToView = if (previewViewAspectRatio > detectAspectRatio) {
            previewView.height.toFloat() / detectSize.width
        } else {
            previewView.width.toFloat() / detectSize.height
        }
        val offset = (detectSize.height * previewScaleToView - previewView.width) / 2f
        return Rect((rect.left * previewScaleToView - offset).toInt(), (rect.top * previewScaleToView).toInt(),
            (rect.right * previewScaleToView - offset).toInt(), (rect.bottom * previewScaleToView).toInt()
        )
    }
}