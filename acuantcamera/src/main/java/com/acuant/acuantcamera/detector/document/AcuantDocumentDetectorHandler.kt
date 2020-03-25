package com.acuant.acuantcamera.detector.document

import com.acuant.acuantcommon.model.Image


interface AcuantDocumentDetectorHandler : AcuantDocumentDectectorHandler

@Deprecated("Class had a spelling mistake, use AcuantDocumentDetectorHandler instead.")
interface AcuantDocumentDectectorHandler{
    fun onDetected(croppedImage: Image?, cropDuration: Long)
}
