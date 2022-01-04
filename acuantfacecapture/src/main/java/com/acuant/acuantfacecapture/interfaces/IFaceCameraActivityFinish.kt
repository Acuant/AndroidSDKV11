package com.acuant.acuantfacecapture.interfaces

import com.acuant.acuantcommon.background.AcuantListener

interface IFaceCameraActivityFinish : AcuantListener{
    fun onCameraDone(imageUrl: String)
    fun onCancel()
}