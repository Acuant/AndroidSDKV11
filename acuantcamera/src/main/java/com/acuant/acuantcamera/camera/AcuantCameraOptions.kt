package com.acuant.acuantcamera.camera

import android.graphics.Color
import java.io.Serializable

/**
 * Serializable options object that can be passed in to change the camera ui.
 *
 * Important note, if options object is provided it will overwrite the following intent extras:
 *      ACUANT_EXTRA_IS_AUTO_CAPTURE with the autoCapture variable
 *      ACUANT_EXTRA_BORDER_ENABLED with the allowBox variable
 */

//todo this should be refactored into a base class and extensions when allowed to break backwards compatibility
open class AcuantCameraOptions
internal constructor(
    internal val timeInMsPerDigit: Int = 900,
    internal val digitsToShow: Int = 2,
    internal val allowBox: Boolean = true,
    internal val autoCapture: Boolean = true,
    internal val bracketLengthInHorizontal: Int = 155,
    internal val bracketLengthInVertical: Int = 255,
    internal val defaultBracketMarginWidth: Int = 160,
    internal val defaultBracketMarginHeight: Int = 160,
    internal val colorHold: Int = Color.YELLOW,
    internal val colorCapturing: Int = Color.GREEN,
    internal val colorBracketAlign: Int = Color.BLACK,
    internal val colorBracketCloser: Int = Color.RED,
    internal val colorBracketHold: Int = Color.YELLOW,
    internal val colorBracketCapturing: Int = Color.GREEN,
    internal val cardRatio: Float = 0.65f,
    internal val preventScreenshots: Boolean = true,
    internal val cameraMode: CameraMode = CameraMode.Document,
    internal val zoomType: ZoomType = ZoomType.Generic,
    internal val language: String = "en"
) : Serializable {

    enum class CameraMode { Document, Mrz, BarcodeOnly }

    enum class ZoomType { Generic, IdOnly }

    companion object {
        const val DEFAULT_TIMEOUT_BARCODE = 20000
        const val DEFAULT_DELAY_BARCODE = 800
    }

    @Suppress("unused")
    class DocumentCameraOptionsBuilder {
        private var timeInMsPerDigit: Int = 900
        private var digitsToShow: Int = 2
        private var allowBox: Boolean = true
        private var autoCapture: Boolean = true
        private var bracketLengthInHorizontal: Int = 155
        private var bracketLengthInVertical: Int = 255
        private var defaultBracketMarginWidth: Int = 160
        private var defaultBracketMarginHeight: Int = 160
        private var colorHold: Int = Color.YELLOW
        private var colorCapturing: Int = Color.GREEN
        private var colorBracketAlign: Int = Color.BLACK
        private var colorBracketCloser: Int = Color.RED
        private var colorBracketHold: Int = Color.YELLOW
        private var colorBracketCapturing: Int = Color.GREEN
        private var zoomType: ZoomType = ZoomType.Generic
        private var preventScreenshots: Boolean = true
        private val cardRatio: Float = 0.65f
        private var language: String = "en"

        fun setTimeInMsPerDigit(value: Int): DocumentCameraOptionsBuilder {
            timeInMsPerDigit = value
            return this
        }

        fun setLanguage(value: String): DocumentCameraOptionsBuilder {
            language = value
            return this
        }

        fun setDigitsToShow(value: Int): DocumentCameraOptionsBuilder {
            digitsToShow = value
            return this
        }

        fun setAllowBox(value: Boolean): DocumentCameraOptionsBuilder {
            allowBox = value
            return this
        }

        fun setAutoCapture(value: Boolean): DocumentCameraOptionsBuilder {
            autoCapture = value
            return this
        }

        fun setBracketLengthInHorizontal(value: Int): DocumentCameraOptionsBuilder {
            bracketLengthInHorizontal = value
            return this
        }

        fun setBracketLengthInVertical(value: Int): DocumentCameraOptionsBuilder {
            bracketLengthInVertical = value
            return this
        }

        fun setDefaultBracketMarginWidth(value: Int): DocumentCameraOptionsBuilder {
            defaultBracketMarginWidth = value
            return this
        }

        fun setDefaultBracketMarginHeight(value: Int): DocumentCameraOptionsBuilder {
            defaultBracketMarginHeight = value
            return this
        }

        fun setColorHold(value: Int): DocumentCameraOptionsBuilder {
            colorHold = value
            return this
        }

        fun setColorCapturing(value: Int): DocumentCameraOptionsBuilder {
            colorCapturing = value
            return this
        }

        fun setColorBracketAlign(value: Int): DocumentCameraOptionsBuilder {
            colorBracketAlign = value
            return this
        }

        fun setColorBracketHold(value: Int): DocumentCameraOptionsBuilder {
            colorBracketHold = value
            return this
        }

        fun setColorBracketCloser(value: Int): DocumentCameraOptionsBuilder {
            colorBracketCloser = value
            return this
        }

        fun setColorBracketCapturing(value: Int): DocumentCameraOptionsBuilder {
            colorBracketCapturing = value
            return this
        }

        fun setPreventScreenshots(value: Boolean): DocumentCameraOptionsBuilder {
            preventScreenshots = value
            return this
        }

        @Deprecated("No longer reliant on GMS, option is ignored", ReplaceWith(""))
        fun setUseGms(value: Boolean): DocumentCameraOptionsBuilder {
            return this
        }

        /**
         * [ZoomType.Generic] keeps the camera zoomed out to enable you to use nearly all available
         * capture space. This is the default setting. Use this setting to capture large
         * documents (ID3) and to use old devices with low-resolution cameras.
         *
         * [ZoomType.IdOnly] zooms the camera by approximately 25%, pushing part of the capture
         * space off the sides of the screen. Generally, IDs are smaller than passports and, on most
         * devices, the capture space is sufficient for a 600 dpi capture of an ID. The
         * [ZoomType.IdOnly] experience is more intuitive for users because [ZoomType.Generic] makes
         * the the ID appear too far away for capture. Using [ZoomType.IdOnly] to capture large
         * documents (ID3) usually results in a lower resolution capture that can cause
         * classification/authentication errors.
         */
        fun setZoomType(value: ZoomType): DocumentCameraOptionsBuilder {
            zoomType = value
            return this
        }

        fun build(): AcuantCameraOptions {
            @Suppress("DEPRECATION")
            return AcuantCameraOptions(
                timeInMsPerDigit, digitsToShow, allowBox, autoCapture,
                bracketLengthInHorizontal, bracketLengthInVertical, defaultBracketMarginWidth,
                defaultBracketMarginHeight, colorHold, colorCapturing, colorBracketAlign,
                colorBracketCloser, colorBracketHold, colorBracketCapturing, cardRatio,
                preventScreenshots = preventScreenshots, zoomType = zoomType,
                cameraMode = CameraMode.Document, language = language
            )
        }
    }

    @Suppress("unused")
    class BarcodeCameraOptionsBuilder {
        private var timeInMsPerDigit: Int = DEFAULT_DELAY_BARCODE
        private var digitsToShow: Int = DEFAULT_TIMEOUT_BARCODE
        private var colorCapturing: Int = Color.GREEN
        private var colorHold: Int = Color.WHITE
        private var preventScreenshots: Boolean = true

        /**
         * Only an aesthetic difference to prevent jarring transition from the camera (default: [DEFAULT_DELAY_BARCODE]).
         */
        fun setTimeToWaitAfterDetection(value: Int): BarcodeCameraOptionsBuilder {
            timeInMsPerDigit = value
            return this
        }

        /**
         * Set a time to wait in MS before canceling the camera (default [DEFAULT_TIMEOUT_BARCODE]).
         *
         * This can help account for if an id is miss-identified as having a barcode or the barcode
         * is damaged/unreadable.
         */
        fun setTimeToWaitUntilTimeout(value: Int): BarcodeCameraOptionsBuilder {
            digitsToShow = value
            return this
        }

        fun setColorCapturing(value: Int): BarcodeCameraOptionsBuilder {
            colorCapturing = value
            return this
        }

        fun setColorAlign(value: Int): BarcodeCameraOptionsBuilder {
            colorHold = value
            return this
        }

        fun setPreventScreenshots(value: Boolean): BarcodeCameraOptionsBuilder {
            preventScreenshots = value
            return this
        }

        fun build(): AcuantCameraOptions {
            @Suppress("DEPRECATION")
            return AcuantCameraOptions(
                timeInMsPerDigit = timeInMsPerDigit,
                digitsToShow = digitsToShow, colorCapturing = colorCapturing, colorHold = colorHold,
                preventScreenshots = preventScreenshots, cameraMode = CameraMode.BarcodeOnly
            )
        }
    }


    @Suppress("unused")
    class MrzCameraOptionsBuilder {
        private var timeInMsPerDigit: Int = 900
        private var digitsToShow: Int = 2
        private var allowBox: Boolean = true
        private var autoCapture: Boolean = true
        private var bracketLengthInHorizontal: Int = 55
        private var bracketLengthInVertical: Int = 155
        private var defaultBracketMarginWidth: Int = 100
        private var defaultBracketMarginHeight: Int = 100
        private var colorHold: Int = Color.YELLOW
        private var colorCapturing: Int = Color.GREEN
        private var colorBracketAlign: Int = Color.BLACK
        private var colorBracketCloser: Int = Color.RED
        private var colorBracketHold: Int = Color.YELLOW
        private var colorBracketCapturing: Int = Color.GREEN
        private var isMrzMode: Boolean = true
        private var preventScreenshots: Boolean = true
        private val cardRatio: Float = 0.65f

        fun setAllowBox(value: Boolean): MrzCameraOptionsBuilder {
            allowBox = value
            return this
        }

        fun setBracketLengthInHorizontal(value: Int): MrzCameraOptionsBuilder {
            bracketLengthInHorizontal = value
            return this
        }

        fun setBracketLengthInVertical(value: Int): MrzCameraOptionsBuilder {
            bracketLengthInVertical = value
            return this
        }

        fun setDefaultBracketMarginWidth(value: Int): MrzCameraOptionsBuilder {
            defaultBracketMarginWidth = value
            return this
        }

        fun setDefaultBracketMarginHeight(value: Int): MrzCameraOptionsBuilder {
            defaultBracketMarginHeight = value
            return this
        }

        fun setColorCapturing(value: Int): MrzCameraOptionsBuilder {
            colorCapturing = value
            return this
        }

        fun setColorBracketCapturing(value: Int): MrzCameraOptionsBuilder {
            colorBracketCapturing = value
            return this
        }

        fun setPreventScreenshots(value: Boolean): MrzCameraOptionsBuilder {
            preventScreenshots = value
            return this
        }

        fun build(): AcuantCameraOptions {
            @Suppress("DEPRECATION")
            return AcuantCameraOptions(
                timeInMsPerDigit, digitsToShow, allowBox, autoCapture,
                bracketLengthInHorizontal, bracketLengthInVertical, defaultBracketMarginWidth,
                defaultBracketMarginHeight, colorHold, colorCapturing, colorBracketAlign,
                colorBracketCloser, colorBracketHold, colorBracketCapturing, cardRatio,
                preventScreenshots = preventScreenshots, cameraMode = CameraMode.Mrz
            )
        }
    }
}