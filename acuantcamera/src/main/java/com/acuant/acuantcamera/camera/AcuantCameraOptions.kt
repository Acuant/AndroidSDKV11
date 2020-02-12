package com.acuant.acuantcamera.camera

import android.graphics.Color
import java.io.Serializable

/**
 * Serializable options object that can be passed in to change the camera ui.
 *
 * Important note, if options object is provided it will overwrite the following intent extras:
 *      ACUANT_EXTRA_IS_AUTO_CAPTURE with the autoCapture variable
 *      ACUANT_EXTRA_BORDER_ENABLED with the borderEnabled variable
 */
class AcuantCameraOptions @JvmOverloads constructor(val timeInMsPerDigit: Int = 900,
                                                    val digitsToShow: Int = 2,
                                                    var allowBox : Boolean = true,
                                                    var autoCapture : Boolean = true,
                                                    val bracketLengthInHorizontal : Int = 155,
                                                    val bracketLengthInVertical : Int = 255,
                                                    val defaultBracketMarginWidth : Int = 160,
                                                    val defaultBracketMarginHeight : Int = 160,
                                                    val colorHold : Int = Color.YELLOW,
                                                    val colorCapturing : Int = Color.GREEN,
                                                    val colorBracketAlign : Int = Color.BLACK,
                                                    val colorBracketCloser : Int = Color.RED,
                                                    val colorBracketHold : Int = Color.YELLOW,
                                                    val colorBracketCapturing : Int = Color.GREEN
) : Serializable