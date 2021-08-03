package com.acuant.sampleapp

/**
 * Created by tapasbehera on 4/30/18.
 */
class Constants {
    companion object {
        const val REQUEST_CAMERA_PHOTO = 1
        //this is the old IP liveness workflow. NOT supported any more, but left in for reference if needed.
        /*const val REQUEST_CAMERA_IP_SELFIE = 2*/
        const val REQUEST_CONFIRMATION = 3
        const val REQUEST_RETRY = 4
        const val REQUEST_CAMERA_HG_SELFIE = 5
        const val REQUEST_CAMERA_HG_SELFIE_KEYLESS = 6
        const val REQUEST_CAMERA_FACE_CAPTURE = 7
        const val REQUEST_CAMERA_MRZ = 8
        const val REQUEST_HELP_MRZ = 9
        const val REQUEST_CAMERA_BARCODE = 10
        const val HG_FRAME_RATE_TARGET = "targetFrameRate"

    }
}