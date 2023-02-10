package com.acuant.acuantcamera.interfaces

import com.acuant.acuantcommon.background.AcuantListener

interface IAcuantSavedImage: AcuantListener {
    fun onSaved(bytes: ByteArray)
}