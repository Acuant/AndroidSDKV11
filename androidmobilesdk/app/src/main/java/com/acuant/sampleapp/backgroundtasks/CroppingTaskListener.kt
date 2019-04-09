package com.acuant.sampleapp.backgroundtasks

import com.acuant.acuantcommon.model.Image

/**
 * Created by tapasbehera on 4/30/18.
 */
interface CroppingTaskListener {
    public fun croppingFinished(acuantImage: Image?, isFrontImage:Boolean)
}