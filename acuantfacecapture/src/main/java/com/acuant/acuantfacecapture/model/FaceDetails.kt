package com.acuant.acuantfacecapture.model

import android.graphics.Bitmap
import com.google.android.gms.vision.face.Face

class FaceDetails {
    var image: Bitmap? = null

    /**
     * Default = -1.
     * Countdown = [0, 2).
     * Trigger capture = 0.
     */
    var countdownToCapture = -1
    var state = FaceDetailState.NONE
    var face: Face? = null
    var error: Error? = null
}