package com.acuant.sampleapp.facecapture

import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.acuant.acuanthgliveness.model.LiveFaceDetailState
import com.google.android.gms.vision.CameraSource
import java.util.*

class FacialGraphicOverlay(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val mLock = Any()
    private var mPreviewWidth: Int = 0
    private var mWidthScaleFactor = 1.0f
    private var mPreviewHeight: Int = 0
    private var mHeightScaleFactor = 1.0f
    private var mFacing = CameraSource.CAMERA_FACING_BACK
    private val mGraphics = HashSet<Graphic>()

    private var textPaint: Paint? = null
    private var instructionText = "Algin face and blink when green oval appears"
    private val inst1 = "Algin face and blink when "
    private val inst2 = " green oval "
    private val inst3 = "  appears"
    private val textRect = Rect()
    private val watermarkText = "Powered by Acuant"

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
        val currentTypeFace = textPaint!!.typeface
        val bold = Typeface.create(currentTypeFace, Typeface.BOLD)
        textPaint!!.color = Color.WHITE
        textPaint!!.textAlign = Paint.Align.LEFT

        // adjust UI textSize depending on screenwidth


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

            //drawOval
            drawOval(canvas)

            // draw UI
            drawUI(canvas)
        }
    }

    private fun drawOval(canvas: Canvas) {
        val width = canvas.width
        val height = canvas.height
        val minLength = 0.7f * Math.min(width, height)
        val maxLength = 0.7f * Math.max(width, height)
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

        val screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels
        if (textRect.width() > screenWidth) {
            textPaint!!.textSize = 40f
        }
    }

    private fun drawUI(canvas: Canvas) {
        val width = canvas.width
        val height = canvas.height
        var x = 0f
        var y = 0f
        when(state){
            LiveFaceDetailState.NONE -> {
                // Instruction Text
                instructionText = "Algin face and blink when green oval appears"
                setSize(50f)
                textPaint!!.getTextBounds(instructionText, 0, instructionText.length, textRect)
                x = (width - textRect.width()) / 2f
                y = height * 0.1f
                canvas.drawText(inst1, x, y, textPaint!!)

                textPaint!!.getTextBounds(inst1, 0, inst1.length, textRect)
                x = x + textRect.width()
                textPaint!!.color = Color.GREEN
                canvas.drawText(inst2, x, y, textPaint!!)

                textPaint!!.getTextBounds(inst2, 0, inst2.length, textRect)
                x = x + textRect.width()
                textPaint!!.color = Color.WHITE
                canvas.drawText(inst3, x, y, textPaint!!)

            }
            LiveFaceDetailState.FACE_TOO_FAR -> {
                instructionText = "Move Closer"
                setSize(60f)
                textPaint!!.getTextBounds(instructionText, 0, instructionText.length, textRect)
                x = (width - textRect.width()) / 2f
                y = height * 0.1f
                textPaint!!.color = Color.RED
                canvas.drawText(instructionText, x, y, textPaint!!)
            }
            LiveFaceDetailState.FACE_TOO_CLOSE -> {
                instructionText = "Too Close! Move away"
                setSize(60f)
                textPaint!!.getTextBounds(instructionText, 0, instructionText.length, textRect)
                x = (width - textRect.width()) / 2f
                y = height * 0.1f
                textPaint!!.color = Color.RED
                canvas.drawText(instructionText, x, y, textPaint!!)
            }
            LiveFaceDetailState.FACE_GOOD_DISTANCE -> {
                instructionText = "Blink!"
                setSize(70f)
                textPaint!!.getTextBounds(instructionText, 0, instructionText.length, textRect)
                x = (width - textRect.width()) / 2f
                y = height * 0.1f
                textPaint!!.color = Color.GREEN
                canvas.drawText(instructionText, x, y, textPaint!!)
            }
            LiveFaceDetailState.FACE_NOT_IN_FRAME -> {
                instructionText = "Move in Frame"
                setSize(60f)
                textPaint!!.getTextBounds(instructionText, 0, instructionText.length, textRect)
                x = (width - textRect.width()) / 2f
                y = height * 0.1f
                textPaint!!.color = Color.RED
                canvas.drawText(instructionText, x, y, textPaint!!)
            }
            LiveFaceDetailState.FACE_MOVED -> {
                instructionText = "Hold Steady"
                setSize(60f)
                textPaint!!.getTextBounds(instructionText, 0, instructionText.length, textRect)
                x = (width - textRect.width()) / 2f
                y = height * 0.1f
                textPaint!!.color = Color.RED
                canvas.drawText(instructionText, x, y, textPaint!!)
            }
        }

//        //Watermark text
        textPaint!!.textSize = 50f
        textPaint!!.color = Color.WHITE
        textPaint!!.getTextBounds(watermarkText, 0, watermarkText.length, textRect)
        x = width * 0.90f - textRect.width().toFloat() - 100f
        y = height * 0.95f
        canvas.drawText(watermarkText, x, y, textPaint!!)
    }
}
