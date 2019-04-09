package com.acuant.acuantcamera.detector.document

import com.acuant.acuantcommon.model.Image

interface AcuantDocumentDectectorHandler{
    fun onDetected(croppedImage: Image?, cropDuration: Long)
}
