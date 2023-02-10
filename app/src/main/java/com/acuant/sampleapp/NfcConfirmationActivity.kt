package com.acuant.sampleapp

import android.content.Intent
import android.graphics.Typeface
import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import com.acuant.acuantcommon.model.AcuantError
import com.acuant.acuantechipreader.AcuantEchipReader
import com.acuant.acuantechipreader.AcuantEchipReader.getPositionOfChip
import com.acuant.acuantechipreader.echipreader.NfcTagReadingListener
import com.acuant.acuantechipreader.model.NfcData
import com.acuant.sampleapp.utils.DialogUtils

class NfcConfirmationActivity : AppCompatActivity(), NfcTagReadingListener {

    private enum class HelpState {
        Locate, Found, Failed
    }

    private lateinit var nfcHelpLayout: ConstraintLayout
    private lateinit var nfcTitle: TextView
    private lateinit var nfcText: TextView
    private lateinit var nfcImage: ImageView
    private lateinit var nfcTextLower: TextView
    private lateinit var locationText: TextView
    private lateinit var mrzDocNumber: EditText
    private lateinit var mrzDOB: EditText
    private lateinit var mrzDOE: EditText
    private lateinit var nfcScanningBtn: Button
    private lateinit var country : String
    private lateinit var position : String
    private var alertDialog: AlertDialog? = null
    private var nfcAdapter: NfcAdapter? = null
    private var documentNumber: String? = null
    private var dob: String? = null
    private var doe: String? = null
    private var threeLine: Boolean = false
    private var error: Boolean = true

    private fun setProgress(visible : Boolean, state : HelpState = HelpState.Locate, text : String = "") {
        if(visible) {
            nfcHelpLayout.visibility = View.VISIBLE
            window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

            nfcTextLower.text = text

            when (state) {
                HelpState.Locate -> {
                    nfcText.text = getString(R.string.locate_echip_help)
                    nfcTitle.text = getString(R.string.locate_echip)
                    nfcImage.setImageResource(R.drawable.echip_searching_icon)
                    nfcHelpLayout.setOnClickListener(null)
                }
                HelpState.Found -> {
                    nfcText.text = getString(R.string.found_echip_help)
                    nfcTitle.text = getString(R.string.found_echip)
                    nfcImage.setImageResource(R.drawable.echip_checkmark_icon)
                    nfcHelpLayout.setOnClickListener(null)
                }
                HelpState.Failed -> {
                    nfcText.text = getString(R.string.error_echip_help)
                    nfcTitle.text = getString(R.string.error_echip)
                    nfcImage.setImageResource(R.drawable.echip_fail_icon)
                    window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                    nfcHelpLayout.setOnClickListener {
                        setProgress(false)
                    }
                }
            }
        } else {
            nfcHelpLayout.visibility = View.GONE
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            nfcHelpLayout.setOnClickListener(null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_nfcconfirmation)

        nfcScanningBtn = findViewById(R.id.eChipButton)
        nfcHelpLayout = findViewById(R.id.nfc_help_layout)
        nfcImage = findViewById(R.id.nfc_help_image)
        nfcTitle = findViewById(R.id.nfc_help_title)
        nfcText = findViewById(R.id.nfc_help_text)
        nfcTextLower = findViewById(R.id.nfc_help_text_2)

        val str = SpannableStringBuilder(getString(R.string.verify_captured_data))
        val indexOfBold = str.indexOf(getString(R.string.start_echip))
        val indexOfEndBold = indexOfBold + getString(R.string.start_echip).length
        str.setSpan(StyleSpan(Typeface.BOLD), indexOfBold, indexOfEndBold, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        findViewById<TextView>(R.id.mrzInstruction).text = str

        nfcImage.setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        country = intent.getStringExtra("COUNTRY") ?: "UNKNOWN"
        position = try {
            getPositionOfChip(application, country)
        } catch (e: IllegalStateException) {
            DialogUtils.showDialog(this, "Ozone Not Enabled") { _, _ ->
                finish()
            }
            ""
        }

        documentNumber = intent.getStringExtra("DOCNUMBER")
        dob = intent.getStringExtra("DOB")
        doe = intent.getStringExtra("DOE")
        threeLine = intent.getBooleanExtra("THREELINE", false)

        mrzDocNumber = findViewById(R.id.mrzDocumentNumber)
        mrzDOB = findViewById(R.id.mrzDOB)
        mrzDOE = findViewById(R.id.mrzDOE)
        locationText = findViewById(R.id.mrzInstruction2)

        if(!position.equals("unknown", true) && position != "" && !threeLine) {
            locationText.text = "Note: For $country passports, the eChip is typically on the $position"
        } else {
            locationText.text = ""
        }

        mrzDocNumber.setText(documentNumber)
        mrzDOB.setText(dob, TextView.BufferType.EDITABLE)
        mrzDOE.setText(doe, TextView.BufferType.EDITABLE)

        if (nfcAdapter == null) {
            nfcAdapter = NfcAdapter.getDefaultAdapter(this.applicationContext)
        }

    }

    override fun onPause() {
        super.onPause()
        if (this.nfcAdapter != null) {
            this.nfcAdapter!!.disableForegroundDispatch(this)
        }
    }

    fun nfcPressed(@Suppress("UNUSED_PARAMETER") v: View) {
        if (nfcAdapter != null) {
            ensureSensorIsOn()
            AcuantEchipReader.listenNfc(this, nfcAdapter!!)
            var instString = ""
            if(!position.equals("unknown", true) && position != "") {
                instString += "Note: For $country passports, the eChip is typically on the $position"
            }
            setProgress(true, HelpState.Locate, instString)

        } else {
            AlertDialog.Builder(this)
                    .setTitle("NFC error!")
                    .setMessage("NFC is not available for this device")
                    .setPositiveButton("OK") { dialogInterface, _ ->
                        dialogInterface.dismiss()
                    }
                    .show()
        }
    }

    private fun ensureSensorIsOn() {
        if (this.nfcAdapter != null && !this.nfcAdapter!!.isEnabled) {
            // Alert the user that NFC is off
            AlertDialog.Builder(this)
                    .setTitle("NFC Sensor Turned Off")
                    .setMessage("In order to use this application, the NFC sensor "
                            + "must be turned on. Do you wish to turn it on?")
                    .setPositiveButton("Go to Settings") { _, _ ->
                        // Send the user to the settings page and hope they turn it on
                        startActivity(Intent(android.provider.Settings.ACTION_NFC_SETTINGS))
                    }
                    .setNegativeButton("Do Nothing") { _, _ ->
                        // Do nothing
                    }
                    .show()
        } else if (this.nfcAdapter == null) {
            DialogUtils.showDialog(this,
                    "An NFC Reader is required for this step.")
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        error = false
        setProgress(true, HelpState.Found)
        val docNumber = mrzDocNumber.text.toString().trim { it <= ' ' }
        val dateOfBirth = mrzDOB.text.toString()
        val dateOfExpiry = mrzDOE.text.toString()
        if (docNumber != "" && dateOfBirth.length == 6 && dateOfExpiry.length == 6) {
            AcuantEchipReader.readNfcTag(this, intent, docNumber, dateOfBirth,
                    dateOfExpiry, performOzoneAuthentication = true, tagListener = this)
        } else {
            setProgress(true, HelpState.Failed, "Error in formatting for Document number, Date of birth, or Expiration date. Fix and retry." )
        }
    }

    override fun tagReadSucceeded(nfcData: NfcData) {
        if (this.nfcAdapter != null) {
            this.nfcAdapter!!.disableForegroundDispatch(this)
        }
        setProgress(false)
        error = false
        val intent = Intent()
        NfcStore.cardDetails = nfcData
        this.setResult(RESULT_OK, intent)
        this.finish()
    }

    override fun tagReadStatus(status: String) {
        if (alertDialog != null && alertDialog!!.isShowing) {
            DialogUtils.dismissDialog(alertDialog)
        }
        setProgress(true, HelpState.Found, status)
    }

    override fun onError(error: AcuantError) {
        this.error = true
        if (alertDialog != null && alertDialog!!.isShowing) {
            DialogUtils.dismissDialog(alertDialog)
        }
        setProgress(false)
        setProgress(true, HelpState.Failed, error.errorDescription)
        if (this.nfcAdapter != null) {
            try {
                this.nfcAdapter!!.disableForegroundDispatch(this)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}