package com.acuant.acuantfacecapture.interfaces

import com.acuant.acuantcommon.background.AcuantListener

interface IAcuantSavedImage: AcuantListener {
    fun onSaved(uri: String)
}