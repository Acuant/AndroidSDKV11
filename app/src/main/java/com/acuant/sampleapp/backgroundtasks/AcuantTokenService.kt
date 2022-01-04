package com.acuant.sampleapp.backgroundtasks

import com.acuant.acuantcommon.background.AcuantWebService
import com.acuant.acuantcommon.model.Credential
import com.acuant.acuantcommon.model.AcuantError
import com.acuant.acuantcommon.model.ErrorCodes
import com.acuant.acuantcommon.model.ErrorDescriptions
import org.json.JSONException
import org.json.JSONObject
import java.net.URL

class AcuantTokenService(private val listener: AcuantTokenServiceListener): AcuantWebService(
        URL(String.format("%s/oauth/token", Credential.get().endpoints.acasEndpointTrimmed)),
        listener
) {
    init {
        val settingJsonObject = JSONObject()
        settingJsonObject.put("grant_type", "client_credentials")
        // settingJsonObject.put("expires_in", "10") //for testing with token expiry

        setPayload(settingJsonObject)
    }

    override fun onSuccess(responseCode: Int, responseText: String) {
        try {
            val json = JSONObject(responseText)
            var token = ""
            if (json.has("access_token")) {
                token = json.getString("access_token")
            }
            if (token != "")
                listener.onSuccess(token)
            else
                listener.onError(AcuantError(ErrorCodes.ERROR_InvalidJson, ErrorDescriptions.ERROR_DESC_InvalidJson, responseText))
        } catch (e: JSONException) {
            listener.onError(AcuantError(ErrorCodes.ERROR_InvalidJson, ErrorDescriptions.ERROR_DESC_InvalidJson, responseText))
        }
    }
}
