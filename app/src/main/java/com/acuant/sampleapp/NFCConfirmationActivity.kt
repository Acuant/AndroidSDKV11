package com.acuant.sampleapp

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.nfc.NfcAdapter
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.acuant.acuantechipreader.AcuantEchipReader
import com.acuant.acuantechipreader.echipreader.NFCTagReadingListener
import com.acuant.acuantechipreader.model.NFCData
import com.acuant.sampleapp.utils.CommonUtils
import com.acuant.sampleapp.utils.DialogUtils
import java.text.SimpleDateFormat
import java.util.*

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class NFCConfirmationActivity : Activity(), NFCTagReadingListener {

    private val progressDialog: ProgressDialog? = null
    private var alertDialog: AlertDialog? = null
    private var nfcScanningBtn: Button? = null
    private var nfcAdapter: NfcAdapter? = null
    private var documentNumber: String? = null
    private var dob: String? = null
    private var doe: String? = null
    private var mrzDocNumber: EditText? = null
    private var mrzDOB: TextView? = null
    private var mrzDOE: TextView? = null


    fun DateDialog(tv: TextView?, year: Int, month: Int, day: Int) {
        val listener = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth -> tv!!.text = CommonUtils.getInMMddyyFormat(year, monthOfYear, dayOfMonth) }

        val dpDialog = DatePickerDialog(this, listener, year, month, day)
        dpDialog.show()

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_nfcconfirmation)

        nfcScanningBtn = findViewById<Button>(R.id.buttonNFC)

        documentNumber = intent.getStringExtra("DOCNUMBER")
        dob = intent.getStringExtra("DOB")
        doe = intent.getStringExtra("DOE")

        mrzDocNumber = findViewById<View>(R.id.mrzDocumentNumber) as EditText
        mrzDOB = findViewById<View>(R.id.mrzDOB) as TextView
        mrzDOE = findViewById<View>(R.id.mrzDOE) as TextView

        mrzDocNumber!!.setText(documentNumber)
        mrzDOB!!.text = dob
        mrzDOB!!.setOnClickListener {
            val cal = Calendar.getInstance()
            var day = cal.get(Calendar.DAY_OF_MONTH)
            var month = cal.get(Calendar.MONTH)
            var year = cal.get(Calendar.YEAR)
            if (mrzDOB!!.text != null) {
                val dateComps = mrzDOB!!.text.toString().split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (dateComps.size == 3) {
                    day = Integer.parseInt(dateComps[1])
                    month = Integer.parseInt(dateComps[0]) - 1
                    year = Integer.parseInt(dateComps[2])
                }
            }
            if (year < 20) {
                year = 2000 + year
            } else {
                year = CommonUtils.get4DigitYear(year, month, day)
            }
            DateDialog(mrzDOB, year, month, day)
        }

        mrzDOE!!.text = doe
        mrzDOE!!.setOnClickListener {
            val cal = Calendar.getInstance()
            var day = cal.get(Calendar.DAY_OF_MONTH)
            var month = cal.get(Calendar.MONTH)
            var year = cal.get(Calendar.YEAR)
            if (mrzDOE!!.text != null) {
                val dateComps = mrzDOE!!.text.toString().split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (dateComps.size == 3) {
                    day = Integer.parseInt(dateComps[1])
                    month = Integer.parseInt(dateComps[0]) - 1
                    year = Integer.parseInt(dateComps[2])
                }
            }
            if (year < 50) {
                year = 2000 + year
            } else {
                year = CommonUtils.get4DigitYear(year, month, day)
            }
            DateDialog(mrzDOE, year, month, day)
        }

        if (nfcAdapter == null) {
            nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        }

    }

    override fun onPause() {
        super.onPause()
        if (this.nfcAdapter != null) {
            this.nfcAdapter!!.disableForegroundDispatch(this)
        }
    }

    fun nfcPressed(v: View) {
        if (nfcAdapter != null) {
            ensureSensorIsOn()
            AcuantEchipReader.listenNFC(this, nfcAdapter, this)
            if (alertDialog != null && alertDialog!!.isShowing) {
                DialogUtils.dismissDialog(alertDialog)

            }
            alertDialog = DialogUtils.showDialog(this, "Searching for passport chip...\n\nTap and place the phone on top of passport chip.")
            alertDialog!!.setCancelable(false)

        } else {
            AlertDialog.Builder(this)
                    .setTitle("NFC error!")
                    .setMessage("NFC is not available for this device")
                    .setPositiveButton("OK") { dialogInterface, i -> dialogInterface.dismiss() }
                    .show()
        }
    }

    private fun ensureSensorIsOn() {
        if (this.nfcAdapter != null && !this.nfcAdapter!!.isEnabled) {
            // Alert the user that NFC is off
            AlertDialog.Builder(this)
                    .setTitle("NFC Sensor Turned Off")
                    .setMessage("In order to use this application, the NFC sensor must be turned on. Do you wish to turn it on?")
                    .setPositiveButton("Go to Settings") { dialogInterface, i ->
                        // Send the user to the settings page and hope they turn it on
                        if (android.os.Build.VERSION.SDK_INT >= 16) {
                            startActivity(Intent(android.provider.Settings.ACTION_NFC_SETTINGS))
                        } else {
                            startActivity(Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS))
                        }
                    }
                    .setNegativeButton("Do Nothing") { dialogInterface, i ->
                        // Do nothing
                    }
                    .show()
        } else if (this.nfcAdapter == null) {


        }

    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (alertDialog != null && alertDialog!!.isShowing) {
            DialogUtils.dismissDialog(alertDialog)
        }
        alertDialog = DialogUtils.showProgessDialog(this, "Reading passport chip...\n\nPlease don't move passport or phone.")
        val docNumber = mrzDocNumber!!.text.toString().trim { it <= ' ' }
        val dateOfBirth = getFromattedStringFromDateString(mrzDOB!!.text.toString().trim { it <= ' ' })
        val dateOfExpiry = getFromattedStringFromDateString(mrzDOE!!.text.toString().trim { it <= ' ' })
        if (docNumber != null && docNumber.trim { it <= ' ' } != "" && dateOfBirth != null && dateOfBirth.length == 6 && dateOfExpiry != null && dateOfExpiry.length == 6) {
            AcuantEchipReader.readNFCTag(this, intent, docNumber, dateOfBirth, dateOfExpiry)
        }


    }

    override fun tagReadSucceeded(cardDetails: NFCData?, image: Bitmap?, sign_image: Bitmap?) {
        Log.d(TAG, "NFC Tag successfully read")
        if (alertDialog != null && alertDialog!!.isShowing) {
            DialogUtils.dismissDialog(alertDialog)
        }
        val intent = Intent(baseContext, NFCResultActivity::class.java)
        NFCStore.image = image
        NFCStore.signature_image = sign_image
        NFCStore.cardDetails = cardDetails
        startActivity(intent)
        if (this.nfcAdapter != null) {
            this.nfcAdapter!!.disableForegroundDispatch(this)
        }
    }

    override fun tagReadFailed(message: String?) {
        Log.d(TAG, message)
        if (alertDialog != null && alertDialog!!.isShowing) {
            DialogUtils.dismissDialog(alertDialog)
        }
        if(message!=null) {
            alertDialog = DialogUtils.showDialog(this, message)
            if (this.nfcAdapter != null) {
                try {
                    this.nfcAdapter!!.disableForegroundDispatch(this)
                } catch (e: Exception) {

                }

            }
        }

    }

    companion object {

        private val TAG = NFCConfirmationActivity::class.java.name


        fun getFromattedStringFromDateString(dateString: String?): String? {
            var retString: String? = null
            if (dateString != null && dateString.trim { it <= ' ' } != "") {
                val dateComps = dateString.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (dateComps.size == 3) {
                    val year = Integer.parseInt(dateComps[2])
                    val day = Integer.parseInt(dateComps[1])
                    val month = Integer.parseInt(dateComps[0]) - 1
                    val date = Date(year, month, day)
                    val sdf = SimpleDateFormat("yyMMdd")
                    retString = sdf.format(date)
                }
            }
            return retString
        }
    }

}
