package com.acuant.acuantcamera.overlay

import android.content.Context
import android.hardware.SensorManager
import android.view.OrientationEventListener
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import java.lang.ref.WeakReference

class AcuantOrientationListener(context: Context, private val textView: WeakReference<TextView>, private val imageView: WeakReference<ImageView>) : OrientationEventListener(context, SensorManager.SENSOR_DELAY_UI){
    var previousAngle = 270
    override fun onOrientationChanged(orientation: Int) {
        val textViewLocal = textView.get()
        val imageViewLocal = imageView.get()
        if(orientation in 30..150 && previousAngle !in 30..150){
            previousAngle = orientation
            rotateView(textViewLocal, 90f, 270f)
            rotateView(imageViewLocal, 90f, 270f)
        }
        else if(orientation in 210..330 && previousAngle !in 210..330){
            previousAngle = orientation
            rotateView(textViewLocal, 270f, 90f)
            rotateView(imageViewLocal, 270f, 90f)
        }
    }
    private fun rotateView(view: View?, startDeg:Float, endDeg:Float) {
        if(view != null) {
            view.rotation = startDeg
            view.animate().rotation(endDeg).start()
        }
    }
}