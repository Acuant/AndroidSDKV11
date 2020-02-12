package com.acuant.acuantfacecapture.detector

import com.acuant.acuantfacecapture.model.FaceDetails

interface FaceListener {
    fun faceCaptured(faceDetails: FaceDetails)
}