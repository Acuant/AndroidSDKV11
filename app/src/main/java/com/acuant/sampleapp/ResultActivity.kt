package com.acuant.sampleapp

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.acuant.acuantcommon.model.Credential

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

        if(ProcessedData.cardType.equals("ID3",true) && Credential.get().secureAuthorizations.ozoneAuth){
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
        val confirmNFCDataActivity = Intent(this, NfcConfirmationActivity::class.java)
        confirmNFCDataActivity.putExtra("DOB", formatDateForNfc(ProcessedData.dateOfBirth))
        confirmNFCDataActivity.putExtra("DOE", formatDateForNfc(ProcessedData.dateOfExpiry))
        confirmNFCDataActivity.putExtra("DOCNUMBER", ProcessedData.documentNumber)
        confirmNFCDataActivity.putExtra("COUNTRY", ProcessedData.country)
        this.startActivity(confirmNFCDataActivity)
    }

    private fun formatDateForNfc(date: String) : String {
        var pattern = Regex("[0-9]{2}-[0-9]{2}-[0-9]{4}")
        var out = ""
        if(pattern.matches(date)) {
            out = date.substring(8,10) + date.substring(0,2) + date.substring(3,5)
        }
        pattern = Regex("[0-9]{2}-[0-9]{2}-[0-9]{2}")
        if(pattern.matches(date)) {
            out = date.substring(6,8) + date.substring(0,2) + date.substring(3,5)
        }
        return out
    }
}
