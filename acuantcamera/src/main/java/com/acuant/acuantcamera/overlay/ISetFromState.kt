package com.acuant.acuantcamera.overlay

import android.graphics.Point
import com.acuant.acuantcamera.camera.AcuantBaseCameraFragment

interface ISetFromState {
    fun setViewFromState(state: AcuantBaseCameraFragment.CameraState)
    fun setAndDrawPoints(points:Array<Point>?)
}