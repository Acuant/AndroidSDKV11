package com.acuant.sampleapp.facecapture

import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.acuant.acuanthgliveness.model.LiveFaceDetailState
import com.acuant.sampleapp.R
import com.google.android.gms.vision.CameraSource
import java.util.*
import kotlin.math.max
import kotlin.math.min

class FacialGraphicOverlay(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val mLock = Any()
    private var mPreviewWidth: Int = 0
    private var mWidthScaleFactor = 1.0f
    private var mPreviewHeight: Int = 0
    private var mHeightScaleFactor = 1.0f
    private var mFacing = CameraSource.CAMERA_FACING_BACK
    private val mGraphics = HashSet<Graphic>()

    private var textPaint: Paint? = null
    private var instructionText: String = context.getString(R.string.hg_align_face_and_blink)
    private val textRect = Rect()

    private var clearPaint: Paint? = null
    private var mTransparentPaint: Paint? = null
    private var mSemiBlackPaint: Paint? = null
    private val mPath = Path()
    private var state: LiveFaceDetailState = LiveFaceDetailState.NONE

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

    fun setState(state: LiveFaceDetailState){
        this.state = state
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

            drawOval(canvas)

            // draw UI
            drawUI(canvas)
        }
    }

    private fun drawOval(canvas: Canvas) {
        val width = canvas.width
        val height = canvas.height
        val minLength = 0.7f * min(width, height)
        val maxLength = 0.7f * max(width, height)
        val left = (width - minLength) / 2f
        val top = (height - maxLength) / 2f
        val rect = RectF(left, top, left + minLength, top + maxLength)
        mPath.reset()
        mPath.addOval(rect, Path.Direction.CW)
        mPath.fillType = Path.FillType.INVERSE_EVEN_ODD
        canvas.drawOval(rect, mTransparentPaint!!)
        canvas.clipPath(mPath)
        canvas.drawColor(Color.parseColor("#A6000000"))
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
        val height: Float = canvas.height.toFloat()
        setSize(size)
        textPaint!!.getTextBounds(instructionText, 0, instructionText.length, textRect)
        val x = (width - textRect.width()) / 2f
        val y = height * 0.1f
        textPaint!!.color = color
        canvas.drawText(instructionText, x, y, textPaint!!)
    }

    private fun drawUI(canvas: Canvas) {
        val width = canvas.width
        val height: Float = canvas.height.toFloat()
        var x: Float
        val y: Float
        when (state) {
            LiveFaceDetailState.NONE -> {
                instructionText = context.getString(R.string.hg_align_face_and_blink)
                drawSimpleInst(canvas, 60f, Color.WHITE)
            }
            LiveFaceDetailState.FACE_TOO_FAR -> {
                instructionText = context.getString(R.string.hg_too_far_away)
                drawSimpleInst(canvas, 60f, Color.RED)
            }
            LiveFaceDetailState.FACE_TOO_CLOSE -> {
                instructionText = context.getString(R.string.hg_too_close)
                drawSimpleInst(canvas, 60f, Color.RED)
            }
            LiveFaceDetailState.FACE_GOOD_DISTANCE -> {
                instructionText = context.getString(R.string.hg_blink)
                drawSimpleInst(canvas, 70f, Color.GREEN)
            }
            LiveFaceDetailState.FACE_NOT_IN_FRAME -> {
                instructionText = context.getString(R.string.hg_move_in_frame)
                drawSimpleInst(canvas, 60f, Color.RED)
            }
            LiveFaceDetailState.FACE_MOVED -> {
                instructionText = context.getString(R.string.hg_hold_steady)
                drawSimpleInst(canvas, 60f, Color.RED)
            }
        }
    }
}
