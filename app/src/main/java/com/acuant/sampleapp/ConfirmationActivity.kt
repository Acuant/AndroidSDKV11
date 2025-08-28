package com.acuant.sampleapp

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams


class ConfirmationActivity : AppCompatActivity() {

    private var isFrontImage: Boolean = true
    private var isHealthCard: Boolean = false
    private var barcodeString: String? = null
    private var image: Bitmap? = null
    private var sharpness = -1
    private var glare = -1
    private var dpi = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.GRAY),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.GRAY)
        )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirmation)

        val topLayout = findViewById<RelativeLayout>(R.id.messageLayout)

        ViewCompat.setOnApplyWindowInsetsListener(topLayout) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
                        or WindowInsetsCompat.Type.displayCutout()
            )
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = bars.top
            }
            WindowInsetsCompat.CONSUMED
        }

        isFrontImage = intent.getBooleanExtra("isFrontImage", true)
        isHealthCard = intent.getBooleanExtra("isHealthCard", false)
        barcodeString = intent.getStringExtra("barcode")
        sharpness = intent.getIntExtra("sharpness", -1)
        glare = intent.getIntExtra("glare", -1)
        dpi = intent.getIntExtra("dpi", -1)
        image = MainActivity.image

        if(barcodeString != null){
            val barcodeText = findViewById<TextView>(R.id.barcodeText)
            barcodeText.text = "Barcode :${barcodeString!!.substring(0, (barcodeString!!.length * 0.2).toInt())}..."
        }

        if (image != null) {

            val generalMessageText = findViewById<TextView>(R.id.generalMessageText)
            if (generalMessageText != null) {
                generalMessageText.text = "Ensure all texts are visible."
            }


            if (image != null) {
                val sharpnessText = findViewById<TextView>(R.id.sharpnessText)
                val isBlurry = sharpness < SHARPNESS_THRESHOLD
                if (sharpnessText != null) {
                    if (isBlurry) {
                        sharpnessText.text = "It is a blurry image. Sharpness Grade : $sharpness"
                    } else {
                        sharpnessText.text = "It is a sharp image. Sharpness Grade : $sharpness"
                    }
                }

                val glareText = findViewById<TextView>(R.id.glareText)
                val hasGlare = glare < GLARE_THRESHOLD
                if (glareText != null && image != null) {
                    if (hasGlare) {
                        glareText.text = "Image has glare. Glare Grade : $glare"
                    } else {
                        glareText.text = "Image doesn't have glare. Glare Grade : $glare"
                    }
                }
            }

            val dpiText = findViewById<TextView>(R.id.dpiText)
            if (dpiText != null) {
                when {
                    dpi < 550 -> {
                        dpiText.text = "DPI is low: $dpi"
                    }
                    dpi < 600 -> {
                        dpiText.text = "DPI is slightly low: $dpi"
                    }
                    else -> {
                        dpiText.text = "DPI: $dpi"
                    }
                }
            }

            val confirmationImage = findViewById<ImageView>(R.id.confrimationImage)
            if (confirmationImage != null && image != null) {
                confirmationImage.setImageBitmap(image)
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
        this@ConfirmationActivity.setResult(RESULT_OK, result)
        this@ConfirmationActivity.finish()

    }

    fun retryClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        val result = Intent()
        result.putExtra("Confirmed", false)
        result.putExtra("isFrontImage", isFrontImage)
        this@ConfirmationActivity.setResult(RESULT_OK, result)
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
