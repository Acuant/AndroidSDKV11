package com.acuant.acuantcamera.interfaces

import com.acuant.acuantcamera.helper.MrzResult
import com.acuant.acuantcommon.background.AcuantListener

interface ICameraActivityFinish : AcuantListener{
    fun onCameraDone(imageUrl: String, barCodeString: String?)
    fun onCameraDone(mrzResult: MrzResult)
    fun onCameraDone(barCodeString: String)
    fun onCancel()
}