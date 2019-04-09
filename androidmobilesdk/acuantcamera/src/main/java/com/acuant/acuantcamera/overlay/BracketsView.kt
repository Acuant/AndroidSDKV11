package com.acuant.acuantcamera.overlay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View

/**
 * Created by tapasbehera on 5/2/18.
 */

class BracketsView
/**
 * @param context
 * @param attr
 */
(context: Context, attr: AttributeSet?) : View(context, attr) {

    private val paint: Paint
    private val resultBitmap: Bitmap? = null
    private val maskColor: Int
    private val resultColor: Int
    private val frameColor: Int
    private var frame: Rect? = null
    private var cardRatio = 0.65f

    init {
        // Initialize these once for performance rather than calling them every
        // time in onDraw().
        paint = Paint()
        maskColor = -0x67000000
        resultColor = -0x50000000
        frameColor = -0x333334
    }

    /**
     *
     */
    override fun onDraw(canvas: Canvas) {
        val width = canvas.width
        val height = canvas.height

        if (frame == null) {
            frame = calculatePreviewFrame(width, height)
        }

        // Draw the exterior (i.e. outside the framing rect) darkened
        // it will be drawn four rectangles
        paint.color = if (resultBitmap != null) resultColor else maskColor

        // top
        canvas.drawRect(0f, 0f, width.toFloat(), frame!!.top.toFloat(), paint)
        // bottom
        canvas.drawRect(0f, frame!!.bottom.toFloat(), width.toFloat(), height.toFloat(), paint)
        // Right
        canvas.drawRect(frame!!.right.toFloat(), frame!!.top.toFloat(), width.toFloat(), frame!!.bottom.toFloat(), paint)
        // Left
        canvas.drawRect(0f, frame!!.top.toFloat(), frame!!.left.toFloat(), frame!!.bottom.toFloat(), paint)

        // draw the brackets
        paint.color = frameColor
        paint.strokeWidth = LINES_WIDTH.toFloat()

        // top-left corner drawn
        canvas.drawLine((frame!!.left - LINES_WIDTH / 2).toFloat(), frame!!.top.toFloat(), (frame!!.left + LINES_LENGTH).toFloat(), frame!!.top.toFloat(), paint)
        canvas.drawLine(frame!!.left.toFloat(), frame!!.top.toFloat(), frame!!.left.toFloat(), (frame!!.top + LINES_LENGTH).toFloat(), paint)

        // top-right corner drawn
        canvas.drawLine((frame!!.right - LINES_LENGTH).toFloat(), frame!!.top.toFloat(), (frame!!.right + LINES_WIDTH / 2).toFloat(), frame!!.top.toFloat(), paint)
        canvas.drawLine(frame!!.right.toFloat(), frame!!.top.toFloat(), frame!!.right.toFloat(), (frame!!.top + LINES_LENGTH).toFloat(), paint)

        // bottom-right corner drawn
        canvas.drawLine(frame!!.right.toFloat(), (frame!!.bottom - LINES_LENGTH).toFloat(), frame!!.right.toFloat(), (frame!!.bottom + LINES_WIDTH / 2).toFloat(), paint)
        canvas.drawLine((frame!!.right - LINES_LENGTH).toFloat(), frame!!.bottom.toFloat(), frame!!.right.toFloat(), frame!!.bottom.toFloat(), paint)

        // bottom-left corner drawn
        canvas.drawLine((frame!!.left - LINES_WIDTH / 2).toFloat(), frame!!.bottom.toFloat(), (frame!!.left + LINES_LENGTH).toFloat(), frame!!.bottom.toFloat(), paint)
        canvas.drawLine(frame!!.left.toFloat(), frame!!.bottom.toFloat(), frame!!.left.toFloat(), (frame!!.bottom - LINES_LENGTH).toFloat(), paint)

    }

    /**
     * @param width
     * @param height
     * @return
     */
    private fun calculatePreviewFrame(width: Int, height: Int): Rect {
        // it will be calculated a frame where the croppedCard's image has to fit.

        // width and height rectangle where the frame has to fit, including margins
        val widthLessMargin = width - 2 * MARGIN_WIDTH
        val heightLessMargin = height - 2 * MARGIN_HEIGHT

        // calculate widthBracket, heightBracket, the width and height of
        // the brackets, the max rectangle with the given aspect ratio in
        // the width and height window.
        val left: Int
        val top: Int
        val right: Int
        val bottom: Int
        var widthBracket: Int
        var heightBracket: Int
        heightBracket = (widthLessMargin / this.cardRatio).toInt()
        widthBracket = widthLessMargin
        if (heightBracket > heightLessMargin) {
            heightBracket = heightLessMargin
            widthBracket = (heightBracket * this.cardRatio).toInt()
        }

        // center the frame
        right = (widthLessMargin + widthBracket) / 2 + MARGIN_WIDTH
        top = (heightLessMargin - heightBracket) / 2 + MARGIN_HEIGHT
        left = (widthLessMargin - widthBracket) / 2 + MARGIN_WIDTH
        bottom = (heightLessMargin + heightBracket) / 2 + MARGIN_HEIGHT

        return Rect(left, top, right, bottom)
    }

    companion object {
        private val TAG = BracketsView::class.java.name
        private val LINES_WIDTH = 20
        private val LINES_LENGTH = 55

        /**
         * margin at left and right of the frame
         */
        private val MARGIN_WIDTH = 60
        /**
         * margin at top and bottom of the frame
         */
        private val MARGIN_HEIGHT = 60

    }

}

