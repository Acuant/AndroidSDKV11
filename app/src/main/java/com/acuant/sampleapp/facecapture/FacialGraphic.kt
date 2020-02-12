package com.acuant.sampleapp.facecapture

import android.graphics.*
import com.acuant.acuanthgliveness.model.LiveFaceDetails
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
    private val mFaceRectPaint: Paint = Paint()

    init {
        mFaceRectPaint.color = Color.GREEN
        mFaceRectPaint.style = Paint.Style.STROKE
        mFaceRectPaint.strokeWidth = 10f
        mFaceRectPaint.strokeCap = Paint.Cap.ROUND
    }

    /**
     * Updates the eye positions and state from the detection of the most recent frame.  Invalidates
     * the relevant portions of the overlay to trigger a redraw.
     */
    @Suppress("unused")
    fun updateLiveFaceDetails(liveFaceDetails: LiveFaceDetails) {
        face = liveFaceDetails.face
        postInvalidate()
    }

    /**
     * Draws the current eye state to the supplied canvas.  This will draw the eyes at the last
     * reported position from the tracker, and the iris positions according to the physics
     * simulations for each iris given motion and other forces.
     */
    override fun draw(canvas: Canvas) {
        //Draw face Oval
        drawFaceOval(canvas)

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
}
