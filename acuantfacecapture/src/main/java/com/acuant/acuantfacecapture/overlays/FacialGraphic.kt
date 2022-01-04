package com.acuant.acuantfacecapture.overlays

import android.graphics.*
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

        if (options != null && options!!.showOval) {
            drawFaceOval(canvas)
        } else {
            drawFaceBrackets(canvas)
        }
        canvas.restore()
    }

    private fun drawFaceBrackets(canvas: Canvas) {
        val position = faceBounds
        if (position != null)
        {
            drawBracketsFromCords(canvas, position.bottom.toFloat(), position.left.toFloat(), position.bottom.toFloat(), position.right.toFloat(), position.top.toFloat(), position.right.toFloat(), position.top.toFloat(), position.left.toFloat())
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

    private fun drawFaceOval(canvas: Canvas) {
        val position = faceBounds
        if (position != null) {
            canvas.drawOval(position.left.toFloat(),
                position.top.toFloat(), position.right.toFloat(),
                position.bottom.toFloat(), mFaceRectPaint)
        }
    }

    fun setOptions(options: FaceCaptureOptions) {
        this.options = options
    }
}
