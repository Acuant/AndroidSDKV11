package com.acuant.acuantcamera.initializer

import android.content.Context
import android.util.Log
import com.acuant.acuantcommon.initializer.IAcuantPackage
import com.acuant.acuantcommon.initializer.IAcuantPackageCallback
import com.acuant.acuantcommon.model.Credential
import com.acuant.acuantcommon.model.Error
import com.acuant.acuantcommon.model.ErrorCodes
import com.acuant.acuantcommon.model.ErrorDescriptions
import java.io.*
import java.lang.Exception


class MrzCameraInitializer : IAcuantPackage {

    override fun initialize(credential: Credential, context: Context, callback: IAcuantPackageCallback) {
        if(credential.secureAuthorizations != null) {
            try {
                initializeOcr(context)
                callback.onInitializeSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                callback.onInitializeFailed(
                        listOf(Error(ErrorCodes.ERROR_FailedToLoadOcrFiles,
                                ErrorDescriptions.ERROR_DESC_FailedToLoadOcrFiles)))
            }
        } else {
            callback.onInitializeFailed(listOf(Error(ErrorCodes.ERROR_InvalidCredentials, ErrorDescriptions.ERROR_DESC_InvalidCredentials)))
        }
    }

    @Throws(IOException::class)
    private fun initializeOcr(context: Context) {
        val tessDataDirectory = File(context.getExternalFilesDir(null)?.absolutePath + "/tessdata")
        if(!tessDataDirectory.exists()){
            tessDataDirectory.mkdirs()
        }
        copyAssets(context)
    }

    @Throws(IOException::class)
    private fun copyAssets(context: Context) {
        val assetManager = context.assets
        var files: Array<String>? = null
        try {
            files = assetManager.list("tesseract")
        } catch (e: IOException) {
            Log.e("tag", "Failed to get asset file list.", e)
        }

        if (files != null) {
            for (filename in files) {
                var inputSteam: InputStream? = null
                var out: OutputStream? = null
                try {
                    inputSteam = assetManager.open("tesseract/$filename")
                    val outFile = File(context.getExternalFilesDir(null)?.absolutePath + "/tessdata/", filename)
                    out = FileOutputStream(outFile)
                    copyFile(inputSteam, out)
                } catch (e: IOException) {
                    throw e
                } finally {
                    if (inputSteam != null) {
                        try {
                            inputSteam.close()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }

                    }
                    if (out != null) {
                        try {
                            out.close()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }

                    }
                }
            }
        } else {
            throw IOException()
        }
    }

    @Throws(IOException::class)
    private fun copyFile(inputSteam: InputStream, out: OutputStream) {
        val buffer = ByteArray(1024)

        inputSteam.copyTo(out, buffer.size)
    }
}