package com.acuant.sampleapp.backgroundtasks

interface AcuantTokenServiceListener {
    fun onSuccess(token: String)
    fun onFail(responseCode: Int)
}