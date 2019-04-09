package com.acuant.acuantcamera.overlay

import android.content.Context
import android.hardware.SensorManager
import android.view.OrientationEventListener
import android.view.View
import android.widget.TextView

class AcuantOrientationListener(context: Context, private val textView: TextView) : OrientationEventListener(context, SensorManager.SENSOR_DELAY_UI){
    private var previousAngle = 0
    override fun onOrientationChanged(orientation: Int) {
        if(orientation in 0..180 && previousAngle !in 0..180){
            previousAngle = orientation
            rotateView(textView, 90f, 270f)

        }
        else if(orientation in 181..360 && previousAngle !in 181..360){
            previousAngle = orientation
            rotateView(textView, 270f, 90f)
        }
    }
    private fun rotateView(view: View, startDeg:Float, endDeg:Float) {
        view.rotation = startDeg
        view.animate().rotation(endDeg).start()
    }
}