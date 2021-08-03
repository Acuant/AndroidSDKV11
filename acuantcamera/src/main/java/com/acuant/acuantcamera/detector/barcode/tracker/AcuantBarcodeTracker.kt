package com.acuant.acuantcamera.detector.barcode.tracker

/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import android.util.Log
import com.acuant.acuantcamera.detector.barcode.AcuantBarcodeDetectorHandler
import com.google.android.gms.vision.Detector.Detections

import com.google.android.gms.vision.Tracker
import com.google.android.gms.vision.barcode.Barcode

/**
 * Generic tracker which is used for tracking or reading a barcode (and can really be used for
 * any type of item).  This is used to receive newly detected items, add a graphical representation
 * to an overlay, update the graphics as the item changes, and remove the graphics when the item
 * goes away.
 */
class AcuantBarcodeTracker internal constructor(private val mBarcodeUpdateListener: AcuantBarcodeDetectorHandler) : Tracker<Barcode>() {

    /**
     * Start tracking the detected item instance within the item overlay.
     */
    override fun onNewItem(id: Int, item: Barcode?) {
        if (item != null && item.format == Barcode.PDF417) {
            mBarcodeUpdateListener.onBarcodeDetected(item.rawValue)
        }
    }
}