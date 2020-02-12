package com.acuant.sampleapp

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView

class ResultActivity : AppCompatActivity() {

    private lateinit var imgFaceViewer: ImageView
    private lateinit var imgSignatureViewer: ImageView
    private lateinit var frontSideCardImageView: ImageView
    private lateinit var backSideCardImageView: ImageView
    private lateinit var textViewCardInfo: TextView
    private lateinit var nfcScanningBtn: Button
    private lateinit var imgCapturedFaceViewer: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)
        frontSideCardImageView = findViewById(R.id.frontSideCardImage)
        backSideCardImageView = findViewById(R.id.backSideCardImage)
        imgFaceViewer = findViewById(R.id.faceImage)
        imgCapturedFaceViewer = findViewById(R.id.faceImageCaptured)
        imgSignatureViewer = findViewById(R.id.signatureImage)
        textViewCardInfo = findViewById(R.id.textViewLicenseCardInfo)
        nfcScanningBtn = findViewById(R.id.buttonNFC)

        if(ProcessedData.cardType.equals("ID3",true)){
            nfcScanningBtn.visibility = View.VISIBLE
        }else{
            nfcScanningBtn.visibility = View.GONE
        }

        if(ProcessedData.frontImage != null){
            frontSideCardImageView.setImageBitmap(ProcessedData.frontImage)
        }
        if(ProcessedData.backImage != null){
            backSideCardImageView.setImageBitmap(ProcessedData.backImage)
        }
        if(ProcessedData.faceImage != null){
            imgFaceViewer.setImageBitmap(ProcessedData.faceImage)
        }
        if(ProcessedData.capturedFaceImage != null){
            imgCapturedFaceViewer.setImageBitmap(ProcessedData.capturedFaceImage)
        }
        if(ProcessedData.signImage != null){
            imgSignatureViewer.setImageBitmap(ProcessedData.signImage)
        }
        if(ProcessedData.formattedString != null){
            textViewCardInfo.text = ProcessedData.formattedString
        }

    }

    fun nfcPressed(@Suppress("UNUSED_PARAMETER") v: View) {
        val confirmNFCDataActivity = Intent(this, NFCConfirmationActivity::class.java)
        confirmNFCDataActivity.putExtra("DOB", ProcessedData.dateOfBirth)
        confirmNFCDataActivity.putExtra("DOE", ProcessedData.dateOfExpiry)
        confirmNFCDataActivity.putExtra("DOCNUMBER", ProcessedData.documentNumber)
        this.startActivity(confirmNFCDataActivity)
    }
}
