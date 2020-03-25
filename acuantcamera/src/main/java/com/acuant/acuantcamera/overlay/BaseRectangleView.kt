package com.acuant.acuantcamera.overlay

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Point
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator

abstract class BaseRectangleView(context: Context, attr: AttributeSet?) : View(context, attr), ISetFromState {

    internal val paint: Paint = Paint()
    internal val paintBracket: Paint = Paint()
    internal var animateTarget: Boolean = false
    private var points: Array<Point>? = null
    private var oldPoints: Array<Point>? = null
    private var path: Path = Path()
    private var textureViewWidth: Float = 0.0f
    private var frame: Rect? = null
    internal var cardRatio = 0.15f
    private var drawBox: Boolean = false
    private var bracketAnimator : ValueAnimator? = null
    private var distanceMoved : Float = 0f
    /**
     * Width of a line used to draw the targeting brackets
     */
    private var bracketWidth = 7
    /**
     * Amount the bracket width is divided by when trying to find the correct start point for the line
     *
     * Without it brackets wont line up well at corners.
     *
     * Different line widths and paint caps might require vastly different values.
     */
    private var bracketWidthDivider : Float = 8.4f
    /**
     * Paint cap to use for drawing brackets
     */
    private var bracketPaintCap : Paint.Cap = Paint.Cap.SQUARE
    /**
     * Color that the paint is set to when the card is not aligned
     */
    internal var paintColorAlign = Color.BLACK
    /**
     * Color that the paint is set to when the card is aligned but too far
     */
    internal var paintColorCloser = Color.RED

    //Configurable Options
    /**
     * Determines if a box should be drawn around the detected card in the hold and capturing state
     */
    var allowBox : Boolean = true
    /**
     * Amount that brackets should go in from horizontal sides of the screen
     */
    var bracketLengthInHorizontal = 155
    /**
     * Amount that brackets should go in from vertical sides of the screen
     */
    var bracketLengthInVertical = 255
    /**
     * Relative horizontal distance from sides that brackets should be drawn at when no item is detected
     */
    var defaultBracketMarginWidth = 160
    /**
     * Relative vertical distance from sides that brackets should be drawn at when no item is detected
     */
    var defaultBracketMarginHeight = 160
    /**
     * Color that the paint is set to when the capture timer is going
     */
    var paintColorHold = Color.YELLOW
    /**
     * Color that the paint is set to when the camera is capturing
     */
    var paintColorCapturing = Color.GREEN
    /**
     * Color that the bracket paint is set to when the card is not aligned
     */
    var paintColorBracketAlign = Color.BLACK
    /**
     * Color that the bracket paint is set to when the card is aligned but too far
     */
    var paintColorBracketCloser = Color.RED
    /**
     * Color that the bracket paint is set to when the capture timer is going
     */
    var paintColorBracketHold = Color.YELLOW
    /**
     * Color that the bracket paint is set to when the camera is capturing
     */
    var paintColorBracketCapturing = Color.GREEN

    init {
        paint.color = paintColorAlign
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 5.0f

    }

    fun setWidth(width: Float){
        textureViewWidth = width
    }

    override fun setAndDrawPoints(points:Array<Point>?){
        if(this.points != null) {
            oldPoints = this.points
        }
        this.points = points

        bracketAnimator?.cancel()
        bracketAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            addUpdateListener {
                distanceMoved = it.animatedValue as Float
                invalidate()
            }
            duration = 250L
            interpolator = LinearInterpolator()
            start()
        }

        calcPath()

        invalidate()
    }

    private fun calcPath() {
        path = Path()
        if(points != null && points!!.size == 4) {

            path.moveTo(points!![3].y.toFloat(), points!![3].x.toFloat())
            path.lineTo(points!![0].y.toFloat(), points!![0].x.toFloat())
            path.lineTo(points!![1].y.toFloat(), points!![1].x.toFloat())
            path.lineTo(points!![2].y.toFloat(), points!![2].x.toFloat())
            path.lineTo(points!![3].y.toFloat(), points!![3].x.toFloat())
        }
    }

    internal fun setDrawBox(drawBox : Boolean) {
        if(allowBox)
            this.drawBox = drawBox
        else
            this.drawBox = false
    }

    override fun onDraw(canvas: Canvas) {

        if (animateTarget && points != null && points!!.size == 4) {
            canvas.save()                            // need to restore after drawing
            canvas.translate(textureViewWidth, 0.0f)  // reset where 0,0 is located
            canvas.scale(-1.0f, 1.0f)                      // invert

            if(drawBox) {
                paint.style = Paint.Style.FILL
                paint.alpha = 80

                canvas.drawPath(path, paint)
            }

            drawBrackets(canvas)

            canvas.restore()
        } else {
            drawBrackets(canvas)
        }
    }

    private fun drawBrackets(canvas: Canvas) {

        paintBracket.style = Paint.Style.STROKE
        paintBracket.strokeCap = bracketPaintCap
        paintBracket.alpha = 0xFF
        paintBracket.strokeWidth = bracketWidth.toFloat()

        if(!animateTarget || points == null || points!!.size != 4) {

            if (frame == null) {
                frame = calculatePreviewFrame(width, height)
            }
            drawBracketsFromCords(canvas, frame!!.bottom.toFloat(), frame!!.left.toFloat(), frame!!.bottom.toFloat(), frame!!.right.toFloat(),
                    frame!!.top.toFloat(), frame!!.right.toFloat(), frame!!.top.toFloat(), frame!!.left.toFloat())

        } else {
            if(oldPoints == null || oldPoints!!.size != 4) {
                drawBracketsFromCords(canvas, points!![0].x.toFloat(), points!![0].y.toFloat(), points!![1].x.toFloat(), points!![1].y.toFloat(),
                        points!![2].x.toFloat(), points!![2].y.toFloat(), points!![3].x.toFloat(), points!![3].y.toFloat())
            } else {
                val x0 = oldPoints!![0].x + (points!![0].x - oldPoints!![0].x) * distanceMoved
                val y0 = oldPoints!![0].y + (points!![0].y - oldPoints!![0].y) * distanceMoved
                val x1 = oldPoints!![1].x + (points!![1].x - oldPoints!![1].x) * distanceMoved
                val y1 = oldPoints!![1].y + (points!![1].y - oldPoints!![1].y) * distanceMoved
                val x2 = oldPoints!![2].x + (points!![2].x - oldPoints!![2].x) * distanceMoved
                val y2 = oldPoints!![2].y + (points!![2].y - oldPoints!![2].y) * distanceMoved
                val x3 = oldPoints!![3].x + (points!![3].x - oldPoints!![3].x) * distanceMoved
                val y3 = oldPoints!![3].y + (points!![3].y - oldPoints!![3].y) * distanceMoved

                drawBracketsFromCords(canvas, x0, y0, x1, y1, x2, y2, x3, y3)
            }
        }
    }

    private fun drawBracketsFromCords(canvas: Canvas, x0 : Float, y0 : Float, x1 : Float, y1 : Float, x2 : Float, y2 : Float, x3 : Float, y3 : Float) {

        // top-left corner drawn
        canvas.drawLine((y3 - bracketWidth / bracketWidthDivider), x3, (y3 + bracketLengthInHorizontal), x3, paintBracket)
        canvas.drawLine(y3, (x3 - bracketWidth / bracketWidthDivider), y3, (x3+ bracketLengthInVertical), paintBracket)

        // top-right corner drawn
        canvas.drawLine((y2 - bracketLengthInHorizontal), x2, (y2 - bracketWidth / bracketWidthDivider), x2, paintBracket)
        canvas.drawLine(y2, (x2 - bracketWidth / bracketWidthDivider), y2, (x2 + bracketLengthInVertical), paintBracket)

        // bottom-right corner drawn
        canvas.drawLine(y1, (x1 - bracketLengthInVertical), y1, (x1 - bracketWidth / bracketWidthDivider), paintBracket)
        canvas.drawLine((y1 - bracketLengthInHorizontal), x1, (y1 - bracketWidth / bracketWidthDivider), x1, paintBracket)

        // bottom-left corner drawn
        canvas.drawLine((y0 - bracketWidth / bracketWidthDivider), x0, (y0 + bracketLengthInHorizontal), x0, paintBracket)
        canvas.drawLine(y0, (x0 - bracketWidth / bracketWidthDivider), y0, (x0 - bracketLengthInVertical), paintBracket)

    }

    open fun end() {
        bracketAnimator?.cancel()
    }


    override fun onDetachedFromWindow() {
        bracketAnimator?.cancel()
        super.onDetachedFromWindow()
    }

    /**
     * @param width
     * @param height
     * @return
     */
    private fun calculatePreviewFrame(width: Int, height: Int): Rect {
        // it will be calculated a frame where the croppedCard's image has to fit.

        // width and height rectangle where the frame has to fit, including margins
        val widthLessMargin = width - 2 * defaultBracketMarginWidth
        val heightLessMargin = height - 2 * defaultBracketMarginHeight

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
        right = (widthLessMargin + widthBracket) / 2 + defaultBracketMarginWidth
        top = (heightLessMargin - heightBracket) / 2 + defaultBracketMarginHeight
        left = (widthLessMargin - widthBracket) / 2 + defaultBracketMarginWidth
        bottom = (heightLessMargin + heightBracket) / 2 + defaultBracketMarginHeight

        return Rect(left, top, right, bottom)
    }
}