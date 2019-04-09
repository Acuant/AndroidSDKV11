package com.acuant.sampleapp

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView

class ResultActivity : AppCompatActivity() {

    var imgFaceViewer: ImageView? = null
    var imgSignatureViewer: ImageView? = null
    var frontSideCardImageView: ImageView? = null
    var backSideCardImageView: ImageView? = null

    var textViewCardInfo: TextView? = null
    var nfcScanningBtn: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)
        frontSideCardImageView = findViewById<ImageView>(R.id.frontSideCardImage) as ImageView
        backSideCardImageView = findViewById<ImageView>(R.id.backSideCardImage) as ImageView
        imgFaceViewer = findViewById<ImageView>(R.id.faceImage) as ImageView
        imgSignatureViewer = findViewById<ImageView>(R.id.signatureImage) as ImageView
        textViewCardInfo = findViewById<TextView>(R.id.textViewLicenseCardInfo) as TextView
        nfcScanningBtn = findViewById<Button>(R.id.buttonNFC) as Button

        if(ProcessedData.cardType.equals("ID3",true)){
            nfcScanningBtn!!.visibility = View.VISIBLE
        }else{
            nfcScanningBtn!!.visibility = View.GONE
        }

        if(ProcessedData.frontImage != null){
            frontSideCardImageView!!.setImageBitmap(ProcessedData.frontImage)
        }
        if(ProcessedData.backImage != null){
            backSideCardImageView!!.setImageBitmap(ProcessedData.backImage)
        }
        if(ProcessedData.faceImage != null){
            imgFaceViewer!!.setImageBitmap(ProcessedData.faceImage)
        }
        if(ProcessedData.signImage != null){
            imgSignatureViewer!!.setImageBitmap(ProcessedData.signImage)
        }
        if(ProcessedData.formattedString != null){
            textViewCardInfo!!.setText(ProcessedData.formattedString)
        }

    }

    fun nfcPressed(v: View) {
        val confirmNFCDataActivity = Intent(this, NFCConfirmationActivity::class.java)
        confirmNFCDataActivity.putExtra("DOB", ProcessedData.dateOfBirth)
        confirmNFCDataActivity.putExtra("DOE", ProcessedData.dateOfExpiry)
        confirmNFCDataActivity.putExtra("DOCNUMBER", ProcessedData.documentNumber)
        this.startActivity(confirmNFCDataActivity)
    }
}
