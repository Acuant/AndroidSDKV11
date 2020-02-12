package com.acuant.acuantfacecapture.overlays

import android.graphics.*
import com.acuant.acuantfacecapture.model.FaceCaptureOptions
import com.acuant.acuantfacecapture.model.FaceDetailState
import com.acuant.acuantfacecapture.model.FaceDetails
import com.google.android.gms.vision.face.Face

/**
 * Graphics class for rendering eye position and face position.
 */
internal class FacialGraphic
//==============================================================================================
// Methods
//==============================================================================================

(overlay: FacialGraphicOverlay) : FacialGraphicOverlay.Graphic(overlay) {

    @Volatile private var face: Face? = null
    private var options: FaceCaptureOptions? = null
    private var bracketLengthInHorizontal = 155
    private var bracketLengthInVertical = 255
    private var bracketWidth = 7
    private var bracketWidthDivider : Float = 8.4f
    private var state = FaceDetailState.NONE
    private val mFaceRectPaint: Paint = Paint()

    init {
        mFaceRectPaint.color = options?.colorGood ?: Color.GREEN
        mFaceRectPaint.style = Paint.Style.STROKE
        mFaceRectPaint.strokeWidth = 10f
        mFaceRectPaint.strokeCap = Paint.Cap.ROUND
    }

    fun updateLiveFaceDetails(faceDetails: FaceDetails) {
        face = faceDetails.face
        state = faceDetails.state
        postInvalidate()
    }

    /**
     * Draws the current eye state to the supplied canvas.  This will draw the eyes at the last
     * reported position from the tracker, and the iris positions according to the physics
     * simulations for each iris given motion and other forces.
     */
    override fun draw(canvas: Canvas) {
        if (options != null && options!!.showOval) {
            drawFaceOval(canvas)
        } else {
            drawFaceBrackets(canvas)
        }

    }

    private fun drawFaceBrackets(canvas: Canvas) {

        var centerY = canvas.height * ((FacialGraphicOverlay.heightDivisor + 1) / (FacialGraphicOverlay.heightDivisor * 2f))
        var centerX = canvas.width / 2f
        var offsetX = canvas.width / 4f
        var offsetY = canvas.height / 4f

        if (face != null) {
            centerX = translateX(face!!.position.x + face!!.width / 2f)
            centerY = translateY(face!!.position.y + face!!.height / 2f)
            offsetX = scaleX(face!!.width / 2.6f)
            offsetY = scaleY(face!!.height / 2f)

        }

        val left = centerX - offsetX
        val right = centerX + offsetX
        val top = centerY - offsetY
        val bottom = centerY + offsetY

        when (state) {
            FaceDetailState.NONE -> mFaceRectPaint.color = options?.colorDefault ?: Color.BLACK
            FaceDetailState.FACE_ANGLE_TOO_SKEWED, FaceDetailState.FACE_MOVED,
                FaceDetailState.FACE_NOT_IN_FRAME, FaceDetailState.FACE_TOO_CLOSE,
                FaceDetailState.FACE_TOO_FAR -> mFaceRectPaint.color = options?.colorError ?: Color.RED
            FaceDetailState.FACE_GOOD_DISTANCE -> mFaceRectPaint.color = options?.colorGood ?: Color.GREEN
        }

        drawBracketsFromCords(canvas, bottom, left, bottom, right, top, right, top, left)
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
        if (face != null) {

            val centerX = translateX(face!!.position.x + face!!.width / 2f)
            val centerY = translateY(face!!.position.y + face!!.height / 2f)
            val offsetX = scaleX(face!!.width / 2f)
            val offsetY = scaleY(face!!.height / 2f)

            // Draw a box around the face.
            val left = centerX - offsetX
            val right = centerX + offsetX
            val top = centerY - offsetY
            val bottom = centerY + offsetY

            canvas.drawOval(left, top, right, bottom, mFaceRectPaint)
        }
    }

    fun setOptions(options: FaceCaptureOptions?) {
        this.options = options
    }
}
