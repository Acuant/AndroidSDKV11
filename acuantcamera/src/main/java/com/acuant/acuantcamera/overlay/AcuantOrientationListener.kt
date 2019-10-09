package com.acuant.acuantcamera.overlay

import android.content.Context
import android.hardware.SensorManager
import android.view.OrientationEventListener
import android.view.View
import android.widget.TextView
import java.lang.ref.WeakReference

class AcuantOrientationListener(context: Context, private val textView: WeakReference<TextView>) : OrientationEventListener(context, SensorManager.SENSOR_DELAY_UI){
    var previousAngle = 270
    override fun onOrientationChanged(orientation: Int) {
        val textView = textView.get()
        if(textView != null){
            if(orientation in 30..150 && previousAngle !in 30..150){
                previousAngle = orientation
                rotateView(textView, 90f, 270f)

            }
            else if(orientation in 210..330 && previousAngle !in 210..330){
                previousAngle = orientation
                rotateView(textView, 270f, 90f)
            }
        }
    }
    private fun rotateView(view: View, startDeg:Float, endDeg:Float) {
        view.rotation = startDeg
        view.animate().rotation(endDeg).start()
    }
}