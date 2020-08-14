package com.acuant.sampleapp.backgroundtasks

import android.os.AsyncTask
import com.acuant.acuantcommon.model.Credential
import android.util.Base64
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import javax.net.ssl.HttpsURLConnection

class AcuantTokenService(private val credential: Credential,
                         private val listener: AcuantTokenServiceListener) : AsyncTask<Any?, Any?, Any?>() {
    private var responseText: String? = null
    private var responseCode = -1
    private var token = ""

    override fun doInBackground(objects: Array<Any?>): Any? {
        try {
            val formatter = Formatter()
            val urlEnd = "%s/oauth/token"
            val urlString = formatter.format(
                    urlEnd, credential.endpoints.acasEndpointTrimmed).toString()
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "POST"
                val sub = if (credential.subscription != null && credential.subscription != "") "${credential.subscription};" else ""
                val auth = Base64.encodeToString("$sub${credential.username}:${credential.password}".toByteArray(), Base64.NO_WRAP)
                conn.setRequestProperty("Authorization", "Basic $auth")
                conn.setRequestProperty("Accept", "application/json")
                conn.addRequestProperty("Cache-Control", "no-cache")
                conn.addRequestProperty("Cache-Control", "max-age=0")
                conn.addRequestProperty("Content-Type", "application/json")
                conn.useCaches = false
                val webservicesTimeout = 60000
                conn.readTimeout = webservicesTimeout
                conn.connectTimeout = webservicesTimeout
                val outputStream = conn.outputStream
                val charset = "UTF-8"
                PrintWriter(OutputStreamWriter(outputStream, charset), true)
                val settingJsonObject = JSONObject()
                settingJsonObject.put("grant_type", "client_credentials")
                val dataBytes = settingJsonObject.toString().toByteArray()
                outputStream.write(dataBytes, 0, dataBytes.size)
                outputStream.flush()
                responseCode = conn.responseCode
                responseText = conn.responseMessage
                val response: StringBuffer
                if (responseCode == HttpsURLConnection.HTTP_OK) {
                    val `in` = BufferedReader(
                            InputStreamReader(conn.inputStream))
                    var output: String?
                    response = StringBuffer()
                    while (`in`.readLine().also { output = it } != null) {
                        response.append(output)
                    }
                    `in`.close()
                    responseText = response.toString()
                    val json = JSONObject(responseText)
                    if (json.has("access_token")) {
                        token = json.getString("access_token")
                    }
                }
            } catch (e: Exception) {
                throw e
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            responseText = e.message
            listener.onFail(responseCode)
        }
        return null
    }

    override fun onPostExecute(o: Any?) {
        super.onPostExecute(o)
        if (token != "")
            listener.onSuccess(token)
        else
            listener.onFail(-2)
    }

}
