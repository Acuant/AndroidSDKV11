package com.acuant.acuantcamera.overlay

import android.graphics.Canvas
import android.graphics.Point
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View


class RectangleView(context: Context, attr: AttributeSet?) : View(context, attr) {

    private val paint: Paint = Paint()
    private var points: Array<Point>? = null
    private var textureViewWidth: Float = 0.0f

    init {
        // create the Paint and set its color
        paint.color = Color.RED
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 5.0f

    }

    fun setWidth(width: Float){
        textureViewWidth = width
    }

    fun setAndDrawPoints(points:Array<Point>?){
        this.points = points
        invalidate()
    }

    fun setColor(color: Int){
        paint.color = color
    }

    override fun onDraw(canvas: Canvas) {

        if(points == null || points!!.size != 4){
             return
        }
        else{
            canvas.save()                            // need to restore after drawing
            canvas.translate(textureViewWidth, 0.0f)  // reset where 0,0 is located
            canvas.scale(-1.0f, 1.0f);                      // invert

            canvas.drawLine(points!![0].y.toFloat(), points!![0].x.toFloat(), points!![1].y.toFloat(), points!![1].x.toFloat(), paint)
            canvas.drawLine(points!![1].y.toFloat(), points!![1].x.toFloat(), points!![2].y.toFloat(), points!![2].x.toFloat(), paint)
            canvas.drawLine(points!![2].y.toFloat(), points!![2].x.toFloat(), points!![3].y.toFloat(), points!![3].x.toFloat(), paint)
            canvas.drawLine(points!![3].y.toFloat(), points!![3].x.toFloat(), points!![0].y.toFloat(), points!![0].x.toFloat(), paint)

            canvas.restore()
        }
    }

}