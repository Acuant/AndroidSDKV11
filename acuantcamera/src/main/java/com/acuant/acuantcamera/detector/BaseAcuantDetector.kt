package com.acuant.acuantcamera.detector

abstract class BaseAcuantDetector : IAcuantDetector {

    private var lProcessing: Boolean = false

    override var isProcessing: Boolean
        get() = lProcessing
        set(value) {lProcessing = value}

    override fun clean() {
        lProcessing = false
    }
}