package com.acuant.acuantfacecapture.model

import android.graphics.Color
import java.io.Serializable

/**
 * Serializable options object that can be passed in to change the face capture ui.
 */
class FaceCaptureOptions @JvmOverloads constructor(val totalCaptureTime : Int = 2,
                                                   val colorGood : Int = Color.GREEN,
                                                   val colorDefault : Int = Color.BLACK,
                                                   val colorError : Int = Color.RED,
                                                   val colorTextGood : Int = Color.GREEN,
                                                   val colorTextDefault : Int = Color.WHITE,
                                                   val colorTextError : Int = Color.RED,
                                                   val showOval : Boolean = false
) : Serializable