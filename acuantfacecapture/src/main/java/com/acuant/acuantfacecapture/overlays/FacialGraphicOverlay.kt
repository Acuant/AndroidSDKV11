package com.acuant.acuantfacecapture.overlays

import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.acuant.acuantfacecapture.R
import com.acuant.acuantfacecapture.model.FaceCaptureOptions
import com.acuant.acuantfacecapture.model.FaceDetailState
import com.google.android.gms.vision.CameraSource
import java.util.*

internal class FacialGraphicOverlay(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private var options: FaceCaptureOptions? = null
    private val mLock = Any()
    private var mPreviewWidth: Int = 0
    private var mWidthScaleFactor = 1.0f
    private var mPreviewHeight: Int = 0
    private var mHeightScaleFactor = 1.0f
    private var mFacing = CameraSource.CAMERA_FACING_BACK
    private val mGraphics = HashSet<Graphic>()

    private var textPaint: Paint? = null
    private var instructionText = ""
    private val textRect = Rect()

    private var clearPaint: Paint? = null
    private var mTransparentPaint: Paint? = null
    private var mSemiBlackPaint: Paint? = null
    private var state: FaceDetailState = FaceDetailState.NONE
    private var countdown: Int = -1

    /**
     * Base class for a custom graphics object to be rendered within the graphic overlay.  Subclass
     * this and implement the [Graphic.draw] method to define the
     * graphics element.  Add instances to the overlay using [FacialGraphicOverlay.add].
     */
    abstract class Graphic(private val mOverlay: FacialGraphicOverlay) {

        /**
         * Draw the graphic on the supplied canvas.  Drawing should use the following methods to
         * convert to view coordinates for the graphics that are drawn:
         *
         *  1. [Graphic.scaleX] and [Graphic.scaleY] adjust the size of
         * the supplied value from the preview scale to the view scale.
         *  1. [Graphic.translateX] and [Graphic.translateY] adjust the
         * coordinate from the preview's coordinate system to the view coordinate system.
         *
         *
         * @param canvas drawing canvas
         */
        abstract fun draw(canvas: Canvas)

        /**
         * Adjusts a horizontal value of the supplied value from the preview scale to the view
         * scale.
         */
        fun scaleX(horizontal: Float): Float {
            return horizontal * mOverlay.mWidthScaleFactor
        }

        /**
         * Adjusts a vertical value of the supplied value from the preview scale to the view scale.
         */
        fun scaleY(vertical: Float): Float {
            return vertical * mOverlay.mHeightScaleFactor
        }

        /**
         * Adjusts the x coordinate from the preview's coordinate system to the view coordinate
         * system.
         */
        fun translateX(x: Float): Float {
            return if (mOverlay.mFacing == CameraSource.CAMERA_FACING_FRONT) {
                mOverlay.width - scaleX(x)
            } else {
                scaleX(x)
            }
        }

        /**
         * Adjusts the y coordinate from the preview's coordinate system to the view coordinate
         * system.
         */
        fun translateY(y: Float): Float {
            return scaleY(y)
        }

        fun postInvalidate() {
            mOverlay.postInvalidate()
        }
    }

    init {
        textPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
        textPaint!!.color = Color.WHITE
        textPaint!!.textAlign = Paint.Align.LEFT


        mTransparentPaint = Paint()
        mTransparentPaint!!.color = Color.TRANSPARENT
        mTransparentPaint!!.strokeWidth = 1000f

        mSemiBlackPaint = Paint()
        mSemiBlackPaint!!.color = Color.TRANSPARENT
        mSemiBlackPaint!!.strokeWidth = 1000f

        clearPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        clearPaint!!.color = Color.TRANSPARENT
        clearPaint!!.style = Paint.Style.STROKE
        clearPaint!!.strokeWidth = 30f
    }

    /**
     * Removes all graphics from the overlay.
     */
    fun clear() {
        synchronized(mLock) {
            mGraphics.clear()
        }
        postInvalidate()
    }

    /**
     * Adds a graphic to the overlay.
     */
    fun add(graphic: Graphic) {
        synchronized(mLock) {
            mGraphics.add(graphic)
        }
        postInvalidate()
    }

    fun setState(state: FaceDetailState, countdown: Int = -1){
        this.state = state
        this.countdown = countdown
    }

    /**
     * Removes a graphic from the overlay.
     */
    fun remove(graphic: Graphic) {
        synchronized(mLock) {
            mGraphics.remove(graphic)
        }
        postInvalidate()
    }

    /**
     * Sets the camera attributes for size and facing direction, which informs how to transform
     * image coordinates later.
     */
    fun setCameraInfo(previewWidth: Int, previewHeight: Int, facing: Int) {
        synchronized(mLock) {
            mPreviewWidth = previewWidth
            mPreviewHeight = previewHeight
            mFacing = facing
        }
        postInvalidate()
    }

    /**
     * Draws the overlay with its associated graphic objects.
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        synchronized(mLock) {
            if (mPreviewWidth != 0 && mPreviewHeight != 0) {
                mWidthScaleFactor = canvas.width.toFloat() / mPreviewWidth.toFloat()
                mHeightScaleFactor = canvas.height.toFloat() / mPreviewHeight.toFloat()
            }

            for (graphic in mGraphics) {
                graphic.draw(canvas)
            }

            //draw background
            drawRect(canvas)

            // draw UI
            drawUI(canvas)
        }
    }

    private fun drawRect(canvas: Canvas) {
        val blackPaint = Paint()
        blackPaint.color = Color.parseColor("#A6000000")
        blackPaint.strokeWidth = 1000f

        canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height / heightDivisor, blackPaint)
    }

    private fun setSize(size: Float){
        textPaint!!.textSize = size

        textPaint!!.getTextBounds(instructionText, 0, instructionText.length, textRect)

        val screenWidth = Resources.getSystem().displayMetrics.widthPixels
        if (textRect.width() > screenWidth) {
            textPaint!!.textSize = 40f
        }
    }

    private fun drawSimpleInst(canvas: Canvas, size: Float, color: Int) {
        val width = canvas.width
        val height: Float = canvas.height / heightDivisor
        setSize(size)
        if (instructionText.contains('\n')) {
            val instText1 = instructionText.substring(0,instructionText.indexOf('\n'))
            val instText2 = instructionText.substring(instructionText.indexOf('\n'))
            textPaint!!.getTextBounds(instText1, 0, instText1.length, textRect)
            var x = (width - textRect.width()) / 2f
            var y = height * 0.35f
            textPaint!!.color = color
            canvas.drawText(instText1, x, y, textPaint!!)
            textPaint!!.getTextBounds(instText2, 0, instText2.length, textRect)
            x = (width - textRect.width()) / 2f
            y = height * 0.65f
            textPaint!!.color = color
            canvas.drawText(instText2, x, y, textPaint!!)
        } else {
            textPaint!!.getTextBounds(instructionText, 0, instructionText.length, textRect)
            val x = (width - textRect.width()) / 2f
            val y = height * 0.5f
            textPaint!!.color = color
            canvas.drawText(instructionText, x, y, textPaint!!)
        }
    }

    private fun drawUI(canvas: Canvas) {
        when (state) {
            FaceDetailState.NONE -> {
                // Instruction Text
                instructionText = context.getString(R.string.acuant_face_camera_initial)
                drawSimpleInst(canvas, 60f, options?.colorTextDefault ?: Color.WHITE)

            }
            FaceDetailState.FACE_TOO_FAR -> {
                instructionText = context.getString(R.string.acuant_face_too_far)
                drawSimpleInst(canvas, 60f, options?.colorTextError ?: Color.RED)
            }
            FaceDetailState.FACE_TOO_CLOSE -> {
                instructionText = context.getString(R.string.acuant_face_camera_face_too_close)
                drawSimpleInst(canvas, 60f, options?.colorTextError ?: Color.RED)
            }
            FaceDetailState.FACE_GOOD_DISTANCE -> {
                instructionText = if (countdown == 0) {
                    context.getString(R.string.acuant_face_camera_capturing)
                } else {
                    resources.getQuantityString(R.plurals.acuant_face_camera_countdown, countdown, countdown)
                }
                drawSimpleInst(canvas, 70f, options?.colorTextGood ?: Color.GREEN)
            }
            FaceDetailState.FACE_NOT_IN_FRAME -> {
                instructionText = context.getString(R.string.acuant_face_camera_face_not_in_frame)
                drawSimpleInst(canvas, 60f, options?.colorTextError ?: Color.RED)
            }
            FaceDetailState.FACE_MOVED -> {
                instructionText = context.getString(R.string.acuant_face_camera_face_moved)
                drawSimpleInst(canvas, 60f, options?.colorTextError ?: Color.RED)
            }
            FaceDetailState.FACE_ANGLE_TOO_SKEWED -> {
                instructionText = context.getString(R.string.acuant_face_camera_face_has_angle)
                drawSimpleInst(canvas, 60f, options?.colorTextError ?: Color.RED)
            }
        }
    }

    fun setOptions(options: FaceCaptureOptions?) {
        this.options = options
    }

    companion object {
        const val heightDivisor = 7.5f
    }
}
