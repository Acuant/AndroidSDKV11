package com.acuant.sampleapp

import android.os.Bundle
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ShapeDrawable
import androidx.appcompat.app.AppCompatActivity
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.acuant.acuantechipreader.model.NfcData

class NfcResultActivity : AppCompatActivity() {

    private var progressDialog: LinearLayout? = null
    private var progressText: TextView? = null
    private var resultLayout: RelativeLayout? = null
    private var imageView: ImageView? = null
    private var signImageView: ImageView? = null
    private var currentId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_nfcresult)

        progressDialog = findViewById(R.id.nfc_res_progress_layout)
        progressText = findViewById(R.id.nfc_res_pbText)

        this.resultLayout = findViewById<View>(R.id.dataLayout) as RelativeLayout
        this.imageView = findViewById<View>(R.id.photo) as ImageView
        this.signImageView = findViewById<View>(R.id.signaturePhoto) as ImageView

    }

    private fun setProgress(visible : Boolean, text : String = "") {
        if(visible) {
            progressDialog?.visibility = View.VISIBLE
            window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        } else {
            progressDialog?.visibility = View.GONE
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        }
        progressText?.text = text
    }

    override fun onResume() {
        super.onResume()
        val intent = intent

        if (intent != null) {
            val cardDetails = NfcStore.cardDetails
            if (cardDetails != null) {
                setProgress(false)
                setData(cardDetails)
                val image = cardDetails.image
                if (image != null) {
                    imageView!!.setImageBitmap(image)
                }

                currentId = signImageView!!.id
            }
            else {
                val message = intent.getStringExtra("DATA")
                addField("Error", message)
            }
        }
    }

    private fun setData(data: NfcData) {
        var key = "Given name"
        var value = data.firstName
        addField(key, value)

        key = "Surname"
        value = data.lastName
        addField(key, value)

        key = "Gender"
        value = data.gender
        addField(key, value)

        key = "Date of birth"
        value = data.dateOfBirth
        addField(key, value)

        if (data.age != null) {
            key = "Age"
            value = data.age.toString()
            addField(key, value)
        }

        key = "Nationality"
        value = data.nationality
        addField(key, value)

        key = "Expiration date"
        value = data.documentExpiryDate
        addField(key, value)

        if (data.isExpired != null) {
            key = "Is expired"
            addBooleanField(key, data.isExpired!!)
        }

        key = "Document code"
        value = data.documentCode
        addField(key, value)

        key = "Document type"
        value = data.documentType
        addField(key, value)

        key = "Issuing state"
        value = data.issuingAuthority
        addField(key, value)

        key = "Document number"
        value = data.documentNumber
        addField(key, value)

        key = "Personal number"
        value = data.personalNumber
        addField(key, value)

        key = "BAC authentication"
        addBooleanField(key, true)

        key = "Data group hash authentication"
        addBooleanField(key, data.passportDataValid)

        if (data.passportSigned != NfcData.OzoneResultStatus.NOT_PERFORMED) {
            key = "Document signer (Ozone)"
            addBooleanField(key, data.passportSigned == NfcData.OzoneResultStatus.SUCCESS)
        }
        if (data.passportCountrySigned != NfcData.OzoneResultStatus.NOT_PERFORMED) {
            key = "Country Signer (Ozone)"
            addBooleanField(key, data.passportCountrySigned == NfcData.OzoneResultStatus.SUCCESS)
        }
    }

    private fun addField(key: String, value: String?) {

        val field = RelativeLayout(this)
        val fieldparams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        fieldparams.addRule(RelativeLayout.BELOW, currentId)
        field.layoutParams = fieldparams

        val rectShapeDrawable = ShapeDrawable() // pre defined class

        // get paint
        val paint = rectShapeDrawable.paint

        // set border color, stroke and stroke width
        paint.color = Color.GRAY
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f // you can change the value of 5
        field.setBackgroundDrawable(rectShapeDrawable)

        val lparams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        lparams.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
        lparams.marginStart = 20

        val tvKey = TextView(this)
        tvKey.gravity = Gravity.CENTER_VERTICAL
        tvKey.layoutParams = lparams
        tvKey.text = key
        field.addView(tvKey)
        val display = windowManager.defaultDisplay
        val width = display.width

        val rparams = RelativeLayout.LayoutParams(width / 2, RelativeLayout.LayoutParams.WRAP_CONTENT)
        rparams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
        rparams.marginEnd = 10
        val valueKey = TextView(this)
        valueKey.layoutParams = rparams
        valueKey.text = value
        valueKey.textAlignment = View.TEXT_ALIGNMENT_TEXT_END
        valueKey.gravity = Gravity.CENTER_VERTICAL
        field.addView(valueKey)

        resultLayout!!.addView(field)
        currentId = View.generateViewId()
        field.id = currentId
    }

    private fun addBooleanField(key: String, value: Boolean) {

        val field = RelativeLayout(this)
        val fieldparams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        fieldparams.addRule(RelativeLayout.BELOW, currentId)
        field.layoutParams = fieldparams

        val rectShapeDrawable = ShapeDrawable() // pre defined class

        // get paint
        val paint = rectShapeDrawable.paint

        // set border color, stroke and stroke width
        paint.color = Color.GRAY
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f // you can change the value of 5
        field.setBackgroundDrawable(rectShapeDrawable)

        val lparams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        lparams.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
        lparams.marginStart = 20

        val tvKey = TextView(this)
        tvKey.layoutParams = lparams
        tvKey.gravity = Gravity.CENTER_VERTICAL
        tvKey.text = key
        field.addView(tvKey)

        val rparams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        rparams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
        rparams.marginEnd = 20
        val valueKey = ImageView(this)
        valueKey.layoutParams = rparams
        valueKey.layoutParams.height = 60
        valueKey.layoutParams.width = 60
        valueKey.scaleType = ImageView.ScaleType.CENTER_INSIDE
        if (value) {
            valueKey.setImageResource(R.drawable.greencheckmark)
        } else {
            valueKey.setImageResource(R.drawable.redcheckmark)
        }
        field.addView(valueKey)

        resultLayout!!.addView(field)
        currentId = View.generateViewId()
        field.id = currentId
    }


}

