package com.acuant.acuantcamera.detector

import android.graphics.Point

enum class DocumentType {Id, Passport, Other}

enum class DocumentState { NoDocument, TooFar, TooClose, GoodDocument }

class DocumentFrameResult (val points: Array<Point>?,
                           val currentDistRatio: Float?,
                           val documentType: DocumentType,
                           val analyzerDpi: Int,
                           val state: DocumentState,
                           val barcode: String?)