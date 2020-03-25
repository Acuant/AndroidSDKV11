package com.acuant.sampleapp

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.nfc.NfcAdapter
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.acuant.acuantcommon.model.Credential
import com.acuant.acuantcommon.model.Error
import com.acuant.acuantechipreader.AcuantEchipReader
import com.acuant.acuantechipreader.AcuantEchipReader.getPositionOfChip
import com.acuant.acuantechipreader.echipreader.NfcTagReadingListener
import com.acuant.acuantechipreader.model.NfcData
import com.acuant.acuantechipreader.model.OzoneAuthResult
import com.acuant.sampleapp.utils.DialogUtils

class NfcConfirmationActivity : AppCompatActivity(), NfcTagReadingListener {

    private lateinit var progressDialog: LinearLayout
    private lateinit var progressText: TextView
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
    private var error: Boolean = true

    private fun setProgress(visible : Boolean, text : String = "") {
        if(visible) {
            progressDialog.visibility = View.VISIBLE
            window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        } else {
            progressDialog.visibility = View.GONE
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        }
        progressText.text = text
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_nfcconfirmation)

        nfcScanningBtn = findViewById(R.id.eChipButton)
        progressDialog = findViewById(R.id.nfc_progress_layout)
        progressText = findViewById(R.id.nfc_pbText)


        country = intent.getStringExtra("COUNTRY") ?: "UNKNOWN"
        position = try {
            getPositionOfChip(application, country)
        } catch (e: IllegalStateException) {
            DialogUtils.showDialog(this, "Ozone Not Enabled", DialogInterface.OnClickListener { _, _ ->
                finish()
            })
            ""
        }

        documentNumber = intent.getStringExtra("DOCNUMBER")
        dob = intent.getStringExtra("DOB")
        doe = intent.getStringExtra("DOE")

        mrzDocNumber = findViewById(R.id.mrzDocumentNumber)
        mrzDOB = findViewById(R.id.mrzDOB)
        mrzDOE = findViewById(R.id.mrzDOE)
        locationText = findViewById(R.id.mrzInstruction2)

        if(!position.equals("unknown", true) && position != "") {
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
            var instString = "Searching for passport chip..." +
                    "\n\nPlace the phone on top of passport chip." +
                    "\n\nIf not detected within several seconds lift up and replace phone."
            if(!position.equals("unknown", true) && position != "") {
                instString += "\n\nNote: For $country passports, the eChip is typically on the $position"
            }
            setProgress(true, instString)

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
        setProgress(true, "Reading passport chip...\n\n"
                + "Please don't move passport or phone.")
        val docNumber = mrzDocNumber.text.toString().trim { it <= ' ' }
        val dateOfBirth = mrzDOB.text.toString()
        val dateOfExpiry = mrzDOE.text.toString()
        if (docNumber != "" && dateOfBirth.length == 6 && dateOfExpiry.length == 6) {
            AcuantEchipReader.readNfcTag(this, intent, Credential.get(), docNumber, dateOfBirth,
                    dateOfExpiry, this)
        } else {
            setProgress(false)
            AlertDialog.Builder(this)
                    .setTitle("eChip read error!")
                    .setMessage("Error in formatting for Document number, Date of birth, or Expiration date. Fix and retry.")
                    .setPositiveButton("OK") { dialogInterface, _ ->
                        dialogInterface.dismiss()
                    }
                    .show()
        }
    }

    override fun tagReadSucceeded(nfcData: NfcData) {
        setProgress(false)
        error = false
        val intent = Intent(this, NfcResultActivity::class.java)
        NfcStore.cardDetails = nfcData
        startActivity(intent)
        if (this.nfcAdapter != null) {
            this.nfcAdapter!!.disableForegroundDispatch(this)
        }
    }

    override fun tagReadFailed(error: Error) {
        this.error = true
        if (alertDialog != null && alertDialog!!.isShowing) {
            DialogUtils.dismissDialog(alertDialog)
        }
        setProgress(false)
        if (error.errorDescription != null) {
            alertDialog = DialogUtils.showDialog(this, error.errorDescription)
            if (this.nfcAdapter != null) {
                try {
                    this.nfcAdapter!!.disableForegroundDispatch(this)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            alertDialog = DialogUtils.showDialog(this, "Error Occurred During eChip Read")
            if (this.nfcAdapter != null) {
                try {
                    this.nfcAdapter!!.disableForegroundDispatch(this)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun tagReadStatus(status: String) {
        if (alertDialog != null && alertDialog!!.isShowing) {
            DialogUtils.dismissDialog(alertDialog)
        }
        setProgress(true, status)
    }

}