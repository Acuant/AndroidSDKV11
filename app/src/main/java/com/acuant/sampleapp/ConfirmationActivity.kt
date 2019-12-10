package com.acuant.sampleapp

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.DisplayMetrics
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.acuant.acuantcamera.CapturedImage
import com.acuant.acuantcommon.model.ErrorCodes


class ConfirmationActivity : AppCompatActivity() {

    private var isFrontImage: Boolean = true
    private var isBarcode: Boolean = false
    private var isHealthCard: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirmation)
        isFrontImage = intent.getBooleanExtra("isFrontImage", true)
        isHealthCard = intent.getBooleanExtra("isHealthCard", false)
        isBarcode = intent.getBooleanExtra("isBarcode", false)

        if(CapturedImage.barcodeString != null){
            val barcodeText = findViewById<TextView>(R.id.barcodeText)
            barcodeText.text = "Barcode :${CapturedImage.barcodeString!!.substring(0, (CapturedImage.barcodeString!!.length * 0.2).toInt())}..."
        }

        if (CapturedImage.acuantImage != null && (CapturedImage.acuantImage!!.error == null || (CapturedImage.acuantImage!!.error != null && CapturedImage.acuantImage!!.error.errorCode == ErrorCodes.ERROR_LowResolutionImage))) {

            val generalMessageText = findViewById<TextView>(R.id.generalMessageText)
            if (generalMessageText != null && CapturedImage.acuantImage!!.image != null) {
                generalMessageText.text = "Ensure all texts are visible."
            } else if (CapturedImage.acuantImage!!.image == null) {
                generalMessageText.text = "Could not crop image."
            }


            if (CapturedImage.acuantImage != null) {
                val sharpnessText = findViewById<TextView>(R.id.sharpnessText)
                val isBlurry = CapturedImage.sharpnessScore < SHARPNESS_THRESHOLD
                if (sharpnessText != null && CapturedImage.acuantImage!!.image != null) {
                    if (isBlurry) {
                        sharpnessText.text = "It is a blurry image. Sharpness Grade : ${CapturedImage.sharpnessScore}"
                    } else {
                        sharpnessText.text = "It is a sharp image. Sharpness Grade : ${CapturedImage.sharpnessScore}"
                    }
                }

                val glareText = findViewById<TextView>(R.id.glareText)
                val hasGlare = CapturedImage.glareScore < GLARE_THRESHOLD
                if (glareText != null && CapturedImage.acuantImage!!.image != null) {
                    if (hasGlare) {
                        glareText.text = "Image has glare. Glare Grade : ${CapturedImage.glareScore}"
                    } else {
                        glareText.text = "Image doesn't have glare. Glare Grade : ${CapturedImage.glareScore}"
                    }
                }
            }

            val dpiText = findViewById<TextView>(R.id.dpiText)
            if (dpiText != null && CapturedImage.acuantImage!!.image != null) {
                dpiText.text = "DPI : ${CapturedImage.acuantImage!!.dpi}"
            }

            val confirmationImage = findViewById<ImageView>(R.id.confrimationImage)
            if (confirmationImage != null && CapturedImage.acuantImage != null && CapturedImage.acuantImage!!.image != null) {
                confirmationImage.setImageBitmap(CapturedImage.acuantImage!!.image)
                confirmationImage.scaleType = ImageView.ScaleType.FIT_CENTER

                val displayMetrics = DisplayMetrics()
                windowManager.defaultDisplay.getMetrics(displayMetrics)
                val height = displayMetrics.heightPixels

                val lp = confirmationImage.layoutParams
                lp.height = (height * 0.4).toInt()
                confirmationImage.layoutParams = lp

            }
        } else {
            val confirmButton = findViewById<Button>(R.id.confirmButton)
            confirmButton.visibility = View.GONE
            val generalMessageText = findViewById<TextView>(R.id.generalMessageText)
            generalMessageText.text = "Could not crop image."

            val confirmationImage = findViewById<ImageView>(R.id.confrimationImage)
            if (confirmationImage != null) {
                confirmationImage.scaleType = ImageView.ScaleType.FIT_CENTER
                val displayMetrics = DisplayMetrics()
                windowManager.defaultDisplay.getMetrics(displayMetrics)
                val height = displayMetrics.heightPixels

                val lp = confirmationImage.layoutParams
                lp.height = (height * 0.4).toInt()
                confirmationImage.layoutParams = lp
                confirmationImage.setImageBitmap(textAsBitmap("Could not crop",200.0f,Color.RED))
                confirmationImage.visibility = View.VISIBLE
            }

        }

    }


    fun confirmClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        val result = Intent()
        result.putExtra("Confirmed", true)
        result.putExtra("isFrontImage", isFrontImage)
        this@ConfirmationActivity.setResult(Constants.REQUEST_CONFIRMATION, result)
        this@ConfirmationActivity.finish()

    }

    fun retryClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        val result = Intent()
        result.putExtra("Confirmed", false)
        result.putExtra("isFrontImage", isFrontImage)
        result.putExtra("isBarcode", isBarcode)
        this@ConfirmationActivity.setResult(Constants.REQUEST_CONFIRMATION, result)
        this@ConfirmationActivity.finish()
    }

    @Suppress("SameParameterValue")
    private fun textAsBitmap(text: String, textSize: Float, textColor: Int): Bitmap {
        val paint = Paint(ANTI_ALIAS_FLAG)
        paint.textSize = textSize
        paint.color = textColor
        paint.textAlign = Paint.Align.LEFT
        val baseline = -paint.ascent() // ascent() is negative
        val width = (paint.measureText(text) + 0.5f).toInt() // round
        val height = (baseline + paint.descent() + 0.5f).toInt()
        val image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(image)
        canvas.drawText(text, 0.0f, baseline, paint)
        return image
    }

    companion object {
        const val SHARPNESS_THRESHOLD = 50
        const val GLARE_THRESHOLD = 50
    }
}
