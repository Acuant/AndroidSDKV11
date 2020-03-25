package com.acuant.acuantcamera.camera.mrz.cameraone

import android.graphics.Point
import android.util.Size

data class AcuantDocumentFeedback(val feedback: DocumentFeedback, val point: Array<Point>?, val frameSize: Size?, val barcode: String? = null) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AcuantDocumentFeedback

        if (feedback != other.feedback) return false
        if (point != null) {
            if (other.point == null) return false
            if (!point.contentEquals(other.point)) return false
        } else if (other.point != null) return false
        if (frameSize != other.frameSize) return false
        if (barcode != other.barcode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = feedback.hashCode()
        result = 31 * result + (point?.contentHashCode() ?: 0)
        result = 31 * result + (frameSize?.hashCode() ?: 0)
        result = 31 * result + (barcode?.hashCode() ?: 0)
        return result
    }
}