package com.acuant.sampleapp.backgroundtasks

import com.acuant.acuantcommon.background.AcuantListener

interface AcuantTokenServiceListener : AcuantListener {
    fun onSuccess(token: String)
}