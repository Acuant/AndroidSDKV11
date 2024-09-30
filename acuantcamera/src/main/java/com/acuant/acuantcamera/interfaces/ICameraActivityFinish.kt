package com.acuant.acuantcamera.interfaces

import com.acuant.acuantcamera.helper.MrzResult
import com.acuant.acuantcommon.background.AcuantListener

interface ICameraActivityFinish : AcuantListener {
    fun onCameraDone(imageBytes: ByteArray, barCodeString: String?)
    fun onCameraDone(imageBytes: ByteArray, mrzResult: MrzResult)
    fun onCameraDone(mrzResult: MrzResult)
    fun onCameraDone(barCodeString: String)
    fun onCancel()
}