package com.acuant.sampleapp

import android.os.Bundle
import android.app.Activity
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ShapeDrawable
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.acuant.acuantcommon.model.Credential
import com.acuant.acuantechipreader.AcuantEchipReader
import com.acuant.acuantechipreader.model.NFCData
import com.acuant.acuantechipreader.model.OzoneAuthRequest
import com.acuant.acuantechipreader.model.OzoneAuthResult
import com.acuant.acuantechipreader.service.OzoneAuthenticateServiceListener

class NFCResultActivity : Activity() {

    private var progressDialog: LinearLayout? = null
    private var progressText: TextView? = null
    private var resultLayout: RelativeLayout? = null
    private var imageView: ImageView? = null
    private var signImageView: ImageView? = null
    private var currentId: Int = 0
    private var subscription : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_nfcresult)

        subscription = Credential.get().ozone

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
            val cardDetails = NFCStore.cardDetails
            if(subscription == null || subscription == "") {
                if (cardDetails != null) {
                    setData(cardDetails, null, null)
                    val image = NFCStore.image
                    if (image != null) {
                        imageView!!.setImageBitmap(image)
                    }
                    if (NFCStore.signature_image != null) {
                        signImageView!!.setImageBitmap(NFCStore.signature_image)
                    }

                    currentId = signImageView!!.id
                } else {
                    val message = intent.getStringExtra("DATA")
                    addField("Error", message)
                }
            } else {
                if (cardDetails != null) {
                    val mrz = getMrzCharacters(cardDetails)
                    val sod = getSODFile(cardDetails)

                    getOzoneResult(mrz, sod, cardDetails)
                }
                else {
                    val message = intent.getStringExtra("DATA")
                    addField("Error", message)
                }
            }

        }

    }

    private fun getOzoneResult(mrz: String, sod: String, cardDetails: NFCData){
        setProgress(true, "Authenticating...")
        AcuantEchipReader.authenticate(OzoneAuthRequest(apiKey = subscription ?: "", mrz = mrz, sod = sod), object: OzoneAuthenticateServiceListener {
            override fun onSuccess(result: OzoneAuthResult) {
                setProgress(false)
                setData(cardDetails, result.isCountrySigned, result.isDocumentSigned)
                val image = NFCStore.image
                if (image != null) {
                    imageView!!.setImageBitmap(image)
                }
                if (NFCStore.signature_image != null) {
                    signImageView!!.setImageBitmap(NFCStore.signature_image)
                }

                currentId = signImageView!!.id
            }

            override fun onFailed(errorDescription: String) {
                setProgress(false)
                setData(cardDetails, null, null)
                val image = NFCStore.image
                if (image != null) {
                    imageView!!.setImageBitmap(image)
                }
                if (NFCStore.signature_image != null) {
                    signImageView!!.setImageBitmap(NFCStore.signature_image)
                }

                currentId = signImageView!!.id
            }
        })
    }

    private fun getSODFile(nfcData: NFCData): String{
        return String(Base64.encode(nfcData.sod, Base64.DEFAULT)).replace("\n", "")
    }

    private fun getMrzCharacters(nfcData: NFCData): String{
        val docCode = nfcData.dg1File.mrzInfo.documentCode
        return if(docCode.length > 1){
            "$docCode${nfcData.dg1File.mrzInfo.issuingState}"
        }
        else{
            "$docCode<${nfcData.dg1File.mrzInfo.issuingState}"
        }
    }


    private fun setData(data: NFCData, isCountrySigned: Boolean?, isDocumentSigned: Boolean?) {

        var key: String
        var value: String?

        if (data.getLdsVersion() != null) {
            key = "LDS Version"
            value = data.getLdsVersion()
            addField(key, value)
        }

        if (data.primaryIdentifier != null) {
            key = "Primary Identifier"
            value = data.primaryIdentifier
            addField(key, value)
        }

        if (data.secondaryIdentifier != null) {
            key = "Secondary Identifier"
            value = data.secondaryIdentifier
            val i = value!!.indexOf("<")
            if (i > 0) {
                value = value.substring(0, i)
            }
            addField(key, value)
        }

        /*if(data.getGender()!=null){
            key = "Gender";
            value = data.getGender()+"";
            addField(key,value);
        }*/

        if (data.dateOfBirth != null) {
            key = "Date of Birth"
            value = data.dateOfBirth
            addField(key, value)
        }

        if (data.nationality != null) {
            key = "Nationality"
            value = data.nationality
            addField(key, value)
        }

        if (data.dateOfExpiry != null) {
            key = "Date of Expiry"
            value = data.dateOfExpiry
            addField(key, value)
        }

        if (data.documentCode != null) {
            key = "Document Code"
            value = data.documentCode
            addField(key, value)
        }

        key = "Document Type"
        value = data.documentType.toString() + ""
        addField(key, value)

        if (data.issuingState != null) {
            key = "Issuing State"
            value = data.issuingState
            addField(key, value)
        }

        if (data.documentNumber != null) {
            key = "Document Number"
            value = data.documentNumber
            addField(key, value)
        }

        if (data.personalNumber != null) {
            key = "Personal Number"
            value = data.personalNumber
            addField(key, value)
        }

        if (data.optionalData1 != null) {
            key = "OptionalData1"
            value = data.optionalData1
            addField(key, value)
        }

        if (data.optionalData2 != null) {
            key = "OptionalData2"
            value = data.optionalData2
            addField(key, value)
        }

        key = "Supported Authentications"
        value = data.supportedMethodsString()
        if (value != null && value != "") {
            addField(key, value)
        }

        key = "Unsupported Authentications"
        value = data.notSupportedMethodsString()
        if (value != null && value != "") {
            addField(key, value)
        }

        if (data.getDocSignerValidity() != null) {
            key = "Document Signer Validity"
            value = data.getDocSignerValidity()
            addField(key, value)
        }


        if (data.isBacSupported) {
            key = "BAC Authentication"
            addBooleanField(key, data.isBacAuthenticated)
        }

        key = "Group Hash Authentication"
        addBooleanField(key, data.isAuthenticDataGroupHashes)

        if (data.isAaSupported) {
            key = "Active Authentication"
            addBooleanField(key, data.isAaAuthenticated)
        }

        if (data.isCaSupported) {
            key = "Chip Authentication"
            addBooleanField(key, data.isCaAuthenticated)
        }

        if(isDocumentSigned == null) {
            key = "No Ozone, Following fields might be inaccurate"
            addField(key, "")
        }

        key = "Document Signer"
        if(isDocumentSigned != null) {
            addBooleanField(key, isDocumentSigned)
        } else {
            if(data.getDocSignerValidity() == null) {
                addBooleanField(key, data.isAuthenticDocSignature)
            } else {
                addBooleanField(key, false)
            }
        }

        addField("Terminal Authentication", "n/a")

        key = "Country Signer"
        if(isCountrySigned != null) {
            addBooleanField(key, isCountrySigned)
        } else {
            addField(key, "Needs Ozone")
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

