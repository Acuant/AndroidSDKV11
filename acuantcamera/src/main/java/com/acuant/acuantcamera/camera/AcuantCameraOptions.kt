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
@Deprecated("Use DocumentCameraOptionsBuilder or MrzCameraOptionsBuilder, constructor will be private in the future.")
constructor(
        val timeInMsPerDigit: Int = 900,
        val digitsToShow: Int = 2,
        val allowBox : Boolean = true,
        val autoCapture : Boolean = true,
        val bracketLengthInHorizontal : Int = 155,
        val bracketLengthInVertical : Int = 255,
        val defaultBracketMarginWidth : Int = 160,
        val defaultBracketMarginHeight : Int = 160,
        val colorHold : Int = Color.YELLOW,
        val colorCapturing : Int = Color.GREEN,
        val colorBracketAlign : Int = Color.BLACK,
        val colorBracketCloser : Int = Color.RED,
        val colorBracketHold : Int = Color.YELLOW,
        val colorBracketCapturing : Int = Color.GREEN,
        var useGMS: Boolean = true,
        @Deprecated("This variable is not exposed in any of the OptionsBuilders, and is not intended to be modified externally. When the constructor goes private you will not be able to modify it.")
        val cardRatio : Float = 0.65f,
        internal val cameraMode: CameraMode = CameraMode.Document
) : Serializable {

    enum class CameraMode {Document, Mrz, BarcodeOnly}

    companion object {
        const val DEFAULT_TIMEOUT_BARCODE = 20000
        const val DEFAULT_DELAY_BARCODE = 800
    }

    @Suppress("unused")
    class DocumentCameraOptionsBuilder {
        private var timeInMsPerDigit: Int = 900
        private var digitsToShow: Int = 2
        private var allowBox : Boolean = true
        private var autoCapture : Boolean = true
        private var bracketLengthInHorizontal : Int = 155
        private var bracketLengthInVertical : Int = 255
        private var defaultBracketMarginWidth : Int = 160
        private var defaultBracketMarginHeight : Int = 160
        private var colorHold : Int = Color.YELLOW
        private var colorCapturing : Int = Color.GREEN
        private var colorBracketAlign : Int = Color.BLACK
        private var colorBracketCloser : Int = Color.RED
        private var colorBracketHold : Int = Color.YELLOW
        private var colorBracketCapturing : Int = Color.GREEN
        private var useGms: Boolean = true
        private val cardRatio : Float = 0.65f
        private var isMrzMode: Boolean = false

        fun setTimeInMsPerDigit(value: Int) : DocumentCameraOptionsBuilder {
            timeInMsPerDigit = value
            return this
        }

        fun setDigitsToShow(value: Int) : DocumentCameraOptionsBuilder {
            digitsToShow = value
            return this
        }

        fun setAllowBox(value: Boolean) : DocumentCameraOptionsBuilder {
            allowBox = value
            return this
        }

        fun setAutoCapture(value: Boolean) : DocumentCameraOptionsBuilder {
            autoCapture = value
            return this
        }

        fun setBracketLengthInHorizontal(value: Int) : DocumentCameraOptionsBuilder {
            bracketLengthInHorizontal = value
            return this
        }

        fun setBracketLengthInVertical(value: Int) : DocumentCameraOptionsBuilder {
            bracketLengthInVertical = value
            return this
        }

        fun setDefaultBracketMarginWidth(value: Int) : DocumentCameraOptionsBuilder {
            defaultBracketMarginWidth = value
            return this
        }

        fun setDefaultBracketMarginHeight(value: Int) : DocumentCameraOptionsBuilder {
            defaultBracketMarginHeight = value
            return this
        }

        fun setColorHold(value: Int) : DocumentCameraOptionsBuilder {
            colorHold = value
            return this
        }

        fun setColorCapturing(value: Int) : DocumentCameraOptionsBuilder {
            colorCapturing = value
            return this
        }

        fun setColorBracketAlign(value: Int) : DocumentCameraOptionsBuilder {
            colorBracketAlign = value
            return this
        }

        fun setColorBracketHold(value: Int) : DocumentCameraOptionsBuilder {
            colorBracketHold = value
            return this
        }

        fun setColorBracketCloser(value: Int) : DocumentCameraOptionsBuilder {
            colorBracketCloser = value
            return this
        }

        fun setColorBracketCapturing(value: Int) : DocumentCameraOptionsBuilder {
            colorBracketCapturing = value
            return this
        }

        fun setUseGms(value: Boolean) : DocumentCameraOptionsBuilder {
            useGms = value
            return this
        }

        fun build() : AcuantCameraOptions {
            @Suppress("DEPRECATION")
            return AcuantCameraOptions(timeInMsPerDigit, digitsToShow, allowBox, autoCapture, bracketLengthInHorizontal,
                    bracketLengthInVertical, defaultBracketMarginWidth, defaultBracketMarginHeight, colorHold,
                    colorCapturing, colorBracketAlign, colorBracketCloser, colorBracketHold, colorBracketCapturing, useGms, cardRatio, cameraMode = CameraMode.Document)
        }
    }

    @Suppress("unused")
    class BarcodeCameraOptionsBuilder {
        private var timeInMsPerDigit : Int = DEFAULT_DELAY_BARCODE
        private var digitsToShow : Int = DEFAULT_TIMEOUT_BARCODE
        private var colorCapturing : Int = Color.GREEN
        private var colorHold : Int = Color.WHITE

        /**
         * Only an aesthetic difference to prevent jarring transition from the camera (default: [DEFAULT_DELAY_BARCODE]).
         */
        fun setTimeToWaitAfterDetection(value: Int) : BarcodeCameraOptionsBuilder {
            timeInMsPerDigit = value
            return this
        }

        /**
         * Set a time to wait in MS before canceling the camera (default [DEFAULT_TIMEOUT_BARCODE]).
         *
         * This can help account for if an id is miss-identified as having a barcode or the barcode
         * is damaged/unreadable.
         */
        fun setTimeToWaitUntilTimeout(value: Int) : BarcodeCameraOptionsBuilder {
            digitsToShow = value
            return this
        }

        fun setColorCapturing(value: Int) : BarcodeCameraOptionsBuilder {
            colorCapturing = value
            return this
        }

        fun setColorAlign(value: Int) : BarcodeCameraOptionsBuilder {
            colorHold = value
            return this
        }

        fun build() : AcuantCameraOptions {
            @Suppress("DEPRECATION")
            return AcuantCameraOptions(timeInMsPerDigit = timeInMsPerDigit, digitsToShow = digitsToShow,
                    colorCapturing = colorCapturing, colorHold = colorHold,
                    cameraMode = CameraMode.BarcodeOnly)
        }
    }


    @Suppress("unused")
    class MrzCameraOptionsBuilder {
        private var timeInMsPerDigit: Int = 900
        private var digitsToShow: Int = 2
        private var allowBox : Boolean = true
        private var autoCapture : Boolean = true
        private var bracketLengthInHorizontal : Int = 55
        private var bracketLengthInVertical : Int = 155
        private var defaultBracketMarginWidth : Int = 100
        private var defaultBracketMarginHeight : Int = 100
        private var colorHold : Int = Color.YELLOW
        private var colorCapturing : Int = Color.GREEN
        private var colorBracketAlign : Int = Color.BLACK
        private var colorBracketCloser : Int = Color.RED
        private var colorBracketHold : Int = Color.YELLOW
        private var colorBracketCapturing : Int = Color.GREEN
        private var useGMS: Boolean = true
        //private val cardRatio : Float = 0.15f
        private val cardRatio : Float = 0.65f
        private var isMrzMode: Boolean = true

        fun setAllowBox(value: Boolean) : MrzCameraOptionsBuilder {
            allowBox = value
            return this
        }

        fun setBracketLengthInHorizontal(value: Int) : MrzCameraOptionsBuilder {
            bracketLengthInHorizontal = value
            return this
        }

        fun setBracketLengthInVertical(value: Int) : MrzCameraOptionsBuilder {
            bracketLengthInVertical = value
            return this
        }

        fun setDefaultBracketMarginWidth(value: Int) : MrzCameraOptionsBuilder {
            defaultBracketMarginWidth = value
            return this
        }

        fun setDefaultBracketMarginHeight(value: Int) : MrzCameraOptionsBuilder {
            defaultBracketMarginHeight = value
            return this
        }

        fun setColorCapturing(value: Int) : MrzCameraOptionsBuilder {
            colorCapturing = value
            return this
        }

        fun setColorBracketCapturing(value: Int) : MrzCameraOptionsBuilder {
            colorBracketCapturing = value
            return this
        }

        fun build() : AcuantCameraOptions {
            @Suppress("DEPRECATION")
            return AcuantCameraOptions(timeInMsPerDigit, digitsToShow, allowBox, autoCapture, bracketLengthInHorizontal,
                    bracketLengthInVertical, defaultBracketMarginWidth, defaultBracketMarginHeight, colorHold,
                    colorCapturing, colorBracketAlign, colorBracketCloser, colorBracketHold, colorBracketCapturing, useGMS, cardRatio, cameraMode = CameraMode.Mrz)
        }
    }
}