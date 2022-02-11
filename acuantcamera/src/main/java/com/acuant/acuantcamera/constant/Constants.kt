@file:JvmName("Constants")

package com.acuant.acuantcamera.constant

const val ACUANT_EXTRA_IMAGE_URL = "imageUrl"
const val ACUANT_EXTRA_PDF417_BARCODE = "barCodeString"
const val ACUANT_EXTRA_CAMERA_OPTIONS = "cameraOptions"
const val ACUANT_EXTRA_MRZ_RESULT = "mrzResult"
const val ACUANT_EXTRA_ERROR = "error"
const val RESULT_ERROR = -99
const val MINIMUM_DPI = 100 // a random low number to filter out small detections due to noise but to not filter any documents even too small ones
const val TARGET_DPI = 600