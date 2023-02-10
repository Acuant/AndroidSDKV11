package com.acuant.sampleapp

import com.acuant.acuantechipreader.model.NfcData

object NfcStore {
    //This is a bit sloppy, but NfcData is not fully serializable due to Bitmaps, so this is a
    // simple solution. Either parcelable or implementing serialized bitmaps might be neater.
    var cardDetails: NfcData? = null
}
