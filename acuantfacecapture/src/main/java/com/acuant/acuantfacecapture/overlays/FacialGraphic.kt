package com.acuant.acuantfacecapture.overlays

import android.animation.ValueAnimator
import android.graphics.*
import android.view.animation.LinearInterpolator
import com.acuant.acuantfacecapture.camera.facecapture.FaceCameraState
import com.acuant.acuantfacecapture.model.FaceCaptureOptions

internal class FacialGraphic(overlay: FacialGraphicOverlay) : FacialGraphicOverlay.Graphic(overlay) {

    @Volatile private var faceBounds: Rect? = null
    private var options: FaceCaptureOptions? = null
    private var bracketLengthInHorizontal = 155
    private var bracketLengthInVertical = 255
    private var bracketWidth = 7
    private var bracketWidthDivider : Float = 8.4f
    private var state = FaceCameraState.Align
    private val mFaceRectPaint: Paint = Paint()
    private var width = 0
    private var bracketAnimator : ValueAnimator? = null
    private var distanceMoved : Float = 0f
    private var oldPoints: Rect? = null

    init {
        mFaceRectPaint.color = options?.colorGood ?: Color.GREEN
        mFaceRectPaint.style = Paint.Style.STROKE
        mFaceRectPaint.strokeWidth = 10f
        mFaceRectPaint.strokeCap = Paint.Cap.ROUND
    }

    fun setWidth(width: Int) {
        this.width = width
    }

    fun updateLiveFaceDetails(faceBounds: Rect?, state: FaceCameraState) {
        this.faceBounds = faceBounds
        this.state = state
        if (this.faceBounds != null) {
            oldPoints = this.faceBounds
        }
        bracketAnimator?.cancel()
        bracketAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            addUpdateListener {
                distanceMoved = it.animatedValue as Float
                postInvalidate()
            }
            duration = 250L
            interpolator = LinearInterpolator()
            start()
        }
        postInvalidate()
    }

    override fun draw(canvas: Canvas) {

        canvas.save()                            // need to restore after drawing
        canvas.translate(width.toFloat(), 0.0f)  // reset where 0,0 is located
        canvas.scale(-1.0f, 1.0f)


        when (state) {
            FaceCameraState.Align -> mFaceRectPaint.color = options?.colorDefault ?: Color.BLACK
            FaceCameraState.MoveCloser,
            FaceCameraState.MoveBack,
            FaceCameraState.FixRotation,
            FaceCameraState.KeepSteady -> mFaceRectPaint.color = options?.colorError ?: Color.RED
            else -> mFaceRectPaint.color = options?.colorGood ?: Color.GREEN
        }

        val options = options
        if (options != null) {
            drawFace(canvas, options.showOval)
        }
        canvas.restore()
    }

    private fun drawFace(canvas: Canvas, oval: Boolean) {
        val position = faceBounds
        val oldPoints = oldPoints
        if (position != null)
        {
            if (oldPoints == null) {
                if (oval) {
                    drawFaceOval(canvas, position.left.toFloat(),
                            position.top.toFloat(), position.right.toFloat(),
                            position.bottom.toFloat())
                } else {
                    drawBracketsFromCords(
                        canvas,
                        position.bottom.toFloat(),
                        position.left.toFloat(),
                        position.bottom.toFloat(),
                        position.right.toFloat(),
                        position.top.toFloat(),
                        position.right.toFloat(),
                        position.top.toFloat(),
                        position.left.toFloat()
                    )
                }
            } else {
                if (oval) {
                    val bottom = oldPoints.bottom + (position.bottom - oldPoints.bottom) * distanceMoved
                    val left = oldPoints.left + (position.left - oldPoints.left) * distanceMoved
                    val right = oldPoints.right + (position.right - oldPoints.right) * distanceMoved
                    val top = oldPoints.top + (position.top - oldPoints.top) * distanceMoved

                    drawFaceOval(canvas, left, top, right, bottom)
                } else {
                    val x0 = oldPoints.bottom + (position.bottom - oldPoints.bottom) * distanceMoved
                    val y0 = oldPoints.left + (position.left - oldPoints.left) * distanceMoved
                    val x1 = oldPoints.bottom + (position.bottom - oldPoints.bottom) * distanceMoved
                    val y1 = oldPoints.right + (position.right - oldPoints.right) * distanceMoved
                    val x2 = oldPoints.top + (position.top - oldPoints.top) * distanceMoved
                    val y2 = oldPoints.right + (position.right - oldPoints.right) * distanceMoved
                    val x3 = oldPoints.top + (position.top - oldPoints.top) * distanceMoved
                    val y3 = oldPoints.left + (position.left - oldPoints.left) * distanceMoved

                    drawBracketsFromCords(canvas, x0, y0, x1, y1, x2, y2, x3, y3)
                }
            }
        }
    }

    private fun drawBracketsFromCords(canvas: Canvas, x0 : Float, y0 : Float, x1 : Float, y1 : Float, x2 : Float, y2 : Float, x3 : Float, y3 : Float) {

        // top-left corner drawn
        canvas.drawLine((y3 - bracketWidth / bracketWidthDivider), x3, (y3 + bracketLengthInHorizontal), x3, mFaceRectPaint)
        canvas.drawLine(y3, (x3 - bracketWidth / bracketWidthDivider), y3, (x3+ bracketLengthInVertical), mFaceRectPaint)

        // top-right corner drawn
        canvas.drawLine((y2 - bracketLengthInHorizontal), x2, (y2 - bracketWidth / bracketWidthDivider), x2, mFaceRectPaint)
        canvas.drawLine(y2, (x2 - bracketWidth / bracketWidthDivider), y2, (x2 + bracketLengthInVertical), mFaceRectPaint)

        // bottom-right corner drawn
        canvas.drawLine(y1, (x1 - bracketLengthInVertical), y1, (x1 - bracketWidth / bracketWidthDivider), mFaceRectPaint)
        canvas.drawLine((y1 - bracketLengthInHorizontal), x1, (y1 - bracketWidth / bracketWidthDivider), x1, mFaceRectPaint)

        // bottom-left corner drawn
        canvas.drawLine((y0 - bracketWidth / bracketWidthDivider), x0, (y0 + bracketLengthInHorizontal), x0, mFaceRectPaint)
        canvas.drawLine(y0, (x0 - bracketWidth / bracketWidthDivider), y0, (x0 - bracketLengthInVertical), mFaceRectPaint)

    }

    private fun drawFaceOval(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float) {
        val position = faceBounds
        if (position != null) {
            canvas.drawOval(left, top, right, bottom, mFaceRectPaint)
        }
    }

    fun setOptions(options: FaceCaptureOptions) {
        this.options = options
    }
}
