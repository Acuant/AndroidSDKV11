package com.acuant.sampleapp

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Looper
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import com.acuant.acuantcamera.camera.AcuantCameraActivity
import com.acuant.acuantcamera.camera.AcuantCameraOptions
import com.acuant.acuantcamera.constant.*
import com.acuant.acuantcamera.helper.MrzResult
import com.acuant.acuantcamera.initializer.MrzCameraInitializer
import com.acuant.acuantcommon.background.AcuantAsync
import com.acuant.acuantcommon.exception.AcuantException
import com.acuant.acuantcommon.initializer.AcuantInitializer
import com.acuant.acuantcommon.initializer.IAcuantPackageCallback
import com.acuant.acuantcommon.model.*
import com.acuant.acuantcommon.helper.CredentialHelper
import com.acuant.acuantdocumentprocessing.AcuantDocumentProcessor
import com.acuant.acuantdocumentprocessing.model.*
import com.acuant.acuantdocumentprocessing.resultmodel.*
import com.acuant.acuantdocumentprocessing.service.listener.*
import com.acuant.acuantechipreader.initializer.EchipInitializer
import com.acuant.acuantfacecapture.camera.AcuantFaceCameraActivity
import com.acuant.acuantfacecapture.constant.Constants.ACUANT_EXTRA_FACE_CAPTURE_OPTIONS
import com.acuant.acuantfacecapture.constant.Constants.ACUANT_EXTRA_FACE_IMAGE_URL
import com.acuant.acuantfacecapture.model.CameraMode
import com.acuant.acuantfacecapture.model.FaceCaptureOptions
import com.acuant.acuantfacematchsdk.AcuantFaceMatch
import com.acuant.acuantfacematchsdk.model.FacialMatchData
import com.acuant.acuantfacematchsdk.model.FacialMatchResult
import com.acuant.acuantfacematchsdk.service.FacialMatchListener
import com.acuant.acuantimagepreparation.AcuantImagePreparation
import com.acuant.acuantimagepreparation.background.EvaluateImageListener
import com.acuant.acuantimagepreparation.initializer.ImageProcessorInitializer
import com.acuant.acuantimagepreparation.model.AcuantImage
import com.acuant.acuantimagepreparation.model.CroppingData
import com.acuant.acuantipliveness.AcuantIPLiveness
import com.acuant.acuantipliveness.IPLivenessListener
import com.acuant.acuantipliveness.facialcapture.model.FacialCaptureResult
import com.acuant.acuantipliveness.facialcapture.model.FacialSetupResult
import com.acuant.acuantipliveness.facialcapture.service.FacialCaptureListener
import com.acuant.acuantipliveness.facialcapture.service.FacialSetupListener
import com.acuant.acuantpassiveliveness.AcuantPassiveLiveness
import com.acuant.acuantpassiveliveness.model.PassiveLivenessData
import com.acuant.acuantpassiveliveness.model.PassiveLivenessResult
import com.acuant.acuantpassiveliveness.service.PassiveLivenessListener
import com.acuant.sampleapp.backgroundtasks.AcuantTokenService
import com.acuant.sampleapp.backgroundtasks.AcuantTokenServiceListener
import com.acuant.sampleapp.utils.CommonUtils
import com.google.android.material.switchmaterial.SwitchMaterial
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.collections.HashMap
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {
    private var progressDialog: LinearLayout? = null
    private var progressText: TextView? = null
    private var capturedBarcodeString: String? = null
    private var frontCaptured: Boolean = false
    private var isHealthCard: Boolean = false
    private var capturingImageData: Boolean = true
    private var capturingSelfieImage: Boolean = false
    private var capturingFacialMatch: Boolean = false
    private var facialResultString: String? = null
    private var facialLivelinessResultString: String? = null
    private var documentIdInstance: AcuantIdDocumentInstance? = null
    private var documentHealthInstance: AcuantHealthInsuranceDocumentInstance? = null
    private var autoCaptureEnabled: Boolean = true
    private var barcodeExpected: Boolean = false
    private var isInitialized = false
    private var livenessSelected = 0
    private var isKeyless = false
    private var processingFacialLiveness = false
    private var usingPassive = true
    private var recentImage: AcuantImage? = null
    private val useTokenInit = true
    private val backgroundTasks = mutableListOf<AcuantAsync>()
    private lateinit var livenessSpinner : Spinner
    private lateinit var livenessArrayAdapter: ArrayAdapter<String>

    private var capturedFrontImage: AcuantImage? = null
        set (value) {
            if (capturedFrontImage != null) {
                capturedFrontImage?.destroy()
            }
            field = value
        }
    private var capturedBackImage: AcuantImage? = null
        set (value) {
            if (capturedBackImage != null) {
                capturedBackImage?.destroy()
            }
            field = value
        }
    private var capturedSelfieImage: Bitmap? = null
        set (value) {
            if (capturedSelfieImage != null) {
                capturedSelfieImage?.recycle()
            }
            field = value
        }
    private var capturedDocumentFaceImage: Bitmap? = null
        set (value) {
            if (capturedDocumentFaceImage != null) {
                capturedDocumentFaceImage?.recycle()
            }
            field = value
        }

    fun cleanUpTransaction() {
        facialResultString = null
        capturedFrontImage = null
        capturedBackImage = null
        capturedSelfieImage = null
        capturedDocumentFaceImage = null
        facialLivelinessResultString = null
        capturedBarcodeString = null
        isHealthCard = false
        processingFacialLiveness = false
        frontCaptured = false
        capturingImageData = true
        documentIdInstance = null
        documentHealthInstance = null
    }

    override fun onDestroy() {
        stopBackgroundTasks()
        super.onDestroy()
    }

    //Ideally you should manually cancel all background tasks when your application gets
    // destroyed/stopped. This is neater/good practice, but likely won't have an impact if
    // you don't do this.
    private fun stopBackgroundTasks() {
        backgroundTasks.forEach {
            it.cancel()
        }
        backgroundTasks.clear()
        setProgress(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        livenessSpinner = findViewById(R.id.livenessSpinner)
        val list = mutableListOf("No liveness test/face match", "tmp")
        livenessArrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, list)
        livenessArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        livenessSpinner.adapter = livenessArrayAdapter
        loadLastLiveness()
        livenessSpinner.onItemSelectedListener = this

        val autoCaptureSwitch = findViewById<SwitchMaterial>(R.id.autoCaptureSwitch)
        autoCaptureSwitch.setOnCheckedChangeListener { _, isChecked ->
            autoCaptureEnabled = isChecked
        }

        progressDialog = findViewById(R.id.main_progress_layout)
        progressText = findViewById(R.id.pbText)

        setProgress(true, "Initializing...")

        initializeAcuantSdk(object: IAcuantPackageCallback{
            override fun onInitializeSuccess() {
                setProgress(false)
            }

            override fun onInitializeFailed(error: List<AcuantError>) {
                showAcuDialog(error[0])
            }
        })
    }

    private fun initializeAcuantSdk(callback: IAcuantPackageCallback) {
        try {
            // Or, if required to initialize without a config file , then can be done the following way
            /*Credential.init("**username**",
                    "**password**",
                    "**subscription**",
                    "https://frm.acuant.net",
                    "https://services.assureid.net",
                    "https://medicscan.acuant.net",
                    "https://us.passlive.acuant.net",
                    "https://acas.acuant.net",
                    "https://ozone.acuant.net")

            AcuantInitializer.initialize(null, ...proceed as normal from here...
            */


            /*
            * ========================================README========================================
            *
            * The following demonstrates how to initialize with both token and credentials,
            * there is no reason to implement both, pick which one fits your needs and use that one.
            *
            * The token initialization workflow also contains a service showing how to get the
            * token. This should NOT be done in the app, this defeats the purpose of tokens. This
            * should be done on a separate server then securely passed to the app.
            *
            * ======================================================================================
            * */
            val initCallback = object: IAcuantPackageCallback{

                override fun onInitializeSuccess() {

                    isInitialized = true
                    if(Credential.get().subscription == null || Credential.get().subscription.isEmpty() ){
                        isKeyless = true
                        livenessSpinner.visibility = View.INVISIBLE
                        callback.onInitializeSuccess()
                    } else {
                        if(Credential.get().secureAuthorizations.ozoneAuth || Credential.get().secureAuthorizations.chipExtract) {
                            runOnUiThread {
                                findViewById<Button>(R.id.main_mrz_camera).visibility = View.VISIBLE
                            }
                        }

                        runOnUiThread {
                            if (Credential.get().secureAuthorizations.ipLiveness) {
                                livenessArrayAdapter.insert("Liveness: Enhanced Liveness",1)
                                usingPassive = false
                            } else {
                                livenessArrayAdapter.insert("Liveness: Passive Liveness",1)
                            }
                            livenessArrayAdapter.remove("tmp")
                            loadLastLiveness()
                        }
                        callback.onInitializeSuccess()
                    }
                }

                override fun onInitializeFailed(error: List<AcuantError>) {
                    callback.onInitializeFailed(error)
                }
            }

            val finishTokenInit = { token: String ->
                if (!isInitialized) {
                    val task = AcuantInitializer.initializeWithToken("acuant.config.xml",
                        token,
                        this@MainActivity,
                        listOf(ImageProcessorInitializer(), EchipInitializer(), MrzCameraInitializer()),
                        initCallback)
                    if (task != null)
                        backgroundTasks.add(task)
                } else {
                    if (Credential.setToken(token)) {
                        initCallback.onInitializeSuccess()
                    } else {
                        initCallback.onInitializeFailed(listOf(AcuantError(-2, "Error in setToken\nBad/expired token")))
                    }
                }
            }

            /*
            * The initFromXml and AcuantTokenService is just for the sample app, you should be
            * generating these tokens on one of your secure servers, passing it to the app,
            * and then calling initializeWithToken passing the config and token.
            */
            @Suppress("ConstantConditionIf")
            if (useTokenInit) {
                Toast.makeText(this@MainActivity, "Using Token Init", Toast.LENGTH_SHORT).show()
                Credential.initFromXml("acuant.config.xml", this)
                if (Credential.get().token != null && Credential.get().token.isValid) {
                    finishTokenInit(Credential.get().token.value)
                } else {
                    Credential.removeToken()
                    val task = AcuantTokenService(object : AcuantTokenServiceListener {
                        override fun onSuccess(token: String) {
                            finishTokenInit(token)
                        }

                        override fun onError(error: AcuantError) {
                            initCallback.onInitializeFailed(listOf(error))
                        }
                    }).execute()
                    backgroundTasks.add(task)
                }
            } else {
                Toast.makeText(this@MainActivity, "Using Credential Init", Toast.LENGTH_SHORT).show()
                val task = AcuantInitializer.initialize("acuant.config.xml",
                        this@MainActivity,
                        listOf(ImageProcessorInitializer(), EchipInitializer(), MrzCameraInitializer()),
                        initCallback)
                if (task != null)
                    backgroundTasks.add(task)
            }

        } catch (e: AcuantException) {
            Log.e("Acuant Error", e.toString())
            showAcuDialog(e.toString())
        }
    }

    private fun showAcuDialog(message: Int, @Suppress("SameParameterValue") title: String = "Error",
                              yesOnClick: DialogInterface.OnClickListener? = null,
                              noOnClick: DialogInterface.OnClickListener? = null) {
        showAcuDialog(getString(message), title, yesOnClick, noOnClick)
    }

    private fun showAcuDialog(message: AcuantError, @Suppress("SameParameterValue") title: String = "Error",
                              yesOnClick: DialogInterface.OnClickListener? = null,
                              noOnClick: DialogInterface.OnClickListener? = null) {
        if (message.additionalDetails == null) {
            showAcuDialog(message.errorDescription, title, yesOnClick, noOnClick)
        } else {
            showAcuDialog("${message.errorDescription} - ${message.additionalDetails}", title, yesOnClick, noOnClick)
        }
    }

    private fun showAcuDialog(message: String, title: String = "Error",
                              yesOnClick: DialogInterface.OnClickListener? = null,
                              noOnClick: DialogInterface.OnClickListener? = null) {
        val code = {
            setProgress(false)
            val alert = AlertDialog.Builder(this@MainActivity)
            alert.setTitle(title)
            alert.setMessage(message)
            if (yesOnClick != null) {
                alert.setPositiveButton("YES", yesOnClick)
                if (noOnClick != null) {
                    alert.setNegativeButton("NO", noOnClick)
                }
            } else {
                alert.setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                }
            }
            alert.show()
        }
        if (Looper.getMainLooper().thread == Thread.currentThread()) {
            code()
        } else {
            runOnUiThread {
                code()
            }
        }
    }


    private fun loadLastLiveness() {
        val prefs = this.getSharedPreferences("com.acuant.sampleapp", Context.MODE_PRIVATE)
        val lastSel = prefs.getInt("lastLiveness", 0)
        livenessSpinner.setSelection(lastSel)
        livenessSelected = lastSel
        livenessArrayAdapter.notifyDataSetChanged()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        //do nothing
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        livenessSelected = position

        val prefs = this.getSharedPreferences("com.acuant.sampleapp", Context.MODE_PRIVATE)
        prefs.edit().putInt("lastLiveness", position).apply()
    }

    private fun setProgress(visible : Boolean, text : String = "") {
        val code = {
            if (visible) {
                progressDialog?.visibility = View.VISIBLE
                window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            } else {
                progressDialog?.visibility = View.GONE
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            }
            progressText?.text = text
        }
        if (Looper.getMainLooper().thread == Thread.currentThread()) {
            code()
        } else {
            runOnUiThread(code)
        }
    }

    private fun readFromFile(fileUri: String): ByteArray{
        val file = File(fileUri)
        val bytes = ByteArray(file.length().toInt())
        try {
            val buf = BufferedInputStream(FileInputStream(file))
            buf.read(bytes, 0, bytes.size)
            buf.close()
        } catch (e: Exception){
            e.printStackTrace()
        }
        file.delete()
        return bytes
    }

    private fun handleKeyless(acuantImage: AcuantImage?) {
        handleKeyless(acuantImage?.isPassport)
    }

    private fun handleKeyless(isPassport: Boolean?){
        if (isPassport == true || frontCaptured) {
            showHGLiveness()
        } else {
            this@MainActivity.runOnUiThread {
                showAcuDialog(R.string.scan_back_side_id, "Message",
                        { dialog, _ ->
                    frontCaptured = true
                    dialog?.dismiss()
                    showDocumentCaptureCamera()
                }, { dialog, _ -> dialog?.dismiss() })
            }
        }
    }

    private fun showFaceCaptureError() {
        showAcuDialog("Would you like to retry?", "Error Capturing Face", { dialog, _ ->
            dialog.dismiss()
            showFaceCamera()
        }, { dialog, _ ->
            dialog.dismiss()
            capturingSelfieImage = false
            facialLivelinessResultString = "Facial Liveliness Failed"
        })
    }

    private fun showFaceCaptureCanceled() {
        showAcuDialog("Would you like to retry?", "User Canceled Face Capture", { dialog, _ ->
            dialog.dismiss()
            showFaceCamera()
        }, { dialog, _ ->
            dialog.dismiss()
            capturingSelfieImage = false
            facialLivelinessResultString = "Facial Liveliness Failed"
        })
    }

    private fun startFacialLivelinessRequest(token: String, userId: String) {
        setProgress(true, "Getting Data...")
        AcuantIPLiveness.getFacialLiveness(
                token,
                userId,
                object: FacialCaptureListener {
                    override fun onDataReceived(result: FacialCaptureResult) {
                        facialLivelinessResultString = "Facial Liveliness: " + result.isPassed
                        val decodedString = Base64.decode(result.frame, Base64.NO_WRAP)
                        capturedSelfieImage = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                        setProgress(false)
                        processFacialMatch()
                    }

                    override fun onError(error: AcuantError) {
                        capturingSelfieImage = false
                        facialLivelinessResultString = "Facial Liveliness Failed"
                        showAcuDialog(error, "Error Retrieving Facial Data")
                    }
                }
        )
    }

    private fun hasInternetConnection(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val nw = connectivityManager.activeNetwork ?: return false
            val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
            return when {
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                //for other device how are able to connect with Ethernet
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                //for check internet over Bluetooth
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
                else -> false
            }
        } else {
            @Suppress("DEPRECATION")
            return connectivityManager.activeNetworkInfo?.isConnected ?: false
        }
    }

    // ID/Passport Clicked
    fun idPassPortClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        if (!hasInternetConnection()) {
            showAcuDialog("No internet connection available.")
        } else {
            if (isInitialized && (!useTokenInit || Credential.get()?.token?.isValid == true)) {
                cleanUpTransaction()
                showDocumentCaptureCamera()
            } else {
                setProgress(true, "Initializing...")
                initializeAcuantSdk(object: IAcuantPackageCallback{
                    override fun onInitializeSuccess() {
                        this@MainActivity.runOnUiThread {
                            setProgress(false)
                            cleanUpTransaction()
                            showDocumentCaptureCamera()
                        }
                    }

                    override fun onInitializeFailed(error: List<AcuantError>) {
                        showAcuDialog(error[0])
                    }
                })
            }
        }
    }

    // Health Insurance Clicked
    fun healthInsuranceClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        if (!hasInternetConnection()) {
            showAcuDialog("No internet connection available.")
        } else {
            if (isInitialized && (!useTokenInit || Credential.get()?.token?.isValid == true)) {
                cleanUpTransaction()
                isHealthCard = true
                showDocumentCaptureCamera()
            } else {
                setProgress(true, "Initializing...")
                initializeAcuantSdk(object: IAcuantPackageCallback{
                    override fun onInitializeSuccess() {
                        this@MainActivity.runOnUiThread {
                            cleanUpTransaction()
                            isHealthCard = true
                            showDocumentCaptureCamera()
                            setProgress(false)
                        }
                    }

                    override fun onInitializeFailed(error: List<AcuantError>) {
                        showAcuDialog(error[0])
                    }
                })
            }
        }
    }


    fun readMrzClicked(@Suppress("UNUSED_PARAMETER") view: View) {

        if (!hasInternetConnection()) {
            showAcuDialog("No internet connection available.")
        } else {
            if (isInitialized && (!useTokenInit || Credential.get()?.token?.isValid == true)) {
                cleanUpTransaction()
                showMrzHelpScreen()
            } else {
                setProgress(true, "Initializing...")
                initializeAcuantSdk(object: IAcuantPackageCallback{
                    override fun onInitializeSuccess() {
                        this@MainActivity.runOnUiThread {
                            setProgress(false)
                            cleanUpTransaction()
                            showMrzHelpScreen()
                        }
                    }

                    override fun onInitializeFailed(error: List<AcuantError>) {
                        showAcuDialog(error[0])
                    }
                })
            }
        }
    }

    private var mrzHelpScreenLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            showMrzCaptureCamera()
        }
    }

    private fun showMrzHelpScreen() {
        val intent = Intent(this, MrzHelpActivity::class.java)
        mrzHelpScreenLauncher.launch(intent)
    }

    private var docCameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data: Intent? = result.data
            val url = data?.getStringExtra(ACUANT_EXTRA_IMAGE_URL)
            capturedBarcodeString = data?.getStringExtra(ACUANT_EXTRA_PDF417_BARCODE)
            if (url != null) {
                setProgress(true, "Cropping...")
                AcuantImagePreparation.evaluateImage(this, CroppingData(url), object : EvaluateImageListener {

                    override fun onSuccess(image: AcuantImage) {

                        //Enable for debug saving
                        //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//                        try {
//                            val output = FileOutputStream(File(Environment.getExternalStorageDirectory().toString(), "test.png"))
//                            image.image.compress(Bitmap.CompressFormat.PNG, 100, output)
//                            output.flush()
//                            output.close()
//                        } catch (e: Exception) {
//                            e.printStackTrace()
//                        }
                        //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
                        setProgress(false)
                        if (isKeyless) {
                            handleKeyless(image)
                        } else {
                            recentImage = image

                            showConfirmation(!frontCaptured)
                        }
                    }

                    override fun onError(error: AcuantError) {
                        showAcuDialog(error)
                    }
                })
            } else {
                showAcuDialog("Camera failed to return valid image path")
            }
        } else if (result.resultCode == RESULT_CANCELED) {
            Log.d(TAG, "User canceled document capture")
        } else {
            val data: Intent? = result.data
            val error = data?.getSerializableExtra(ACUANT_EXTRA_ERROR)
            if (error is AcuantError) {
                showAcuDialog(error)
            }
        }
    }

    private fun showDocumentCaptureCamera() {

//        showHGLiveness()
//        showFaceCapture()
        capturedBarcodeString = null
        val cameraIntent = Intent(
                this@MainActivity,
                AcuantCameraActivity::class.java
        )
        cameraIntent.putExtra(ACUANT_EXTRA_CAMERA_OPTIONS,
                AcuantCameraOptions
                        .DocumentCameraOptionsBuilder()
                        .setAutoCapture(autoCaptureEnabled)
                        .build()
        )
        docCameraLauncher.launch(cameraIntent)
    }

    private var mrzCameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data: Intent? = result.data
            val mrzResult = data?.getSerializableExtra(ACUANT_EXTRA_MRZ_RESULT) as MrzResult?

            val confirmNFCDataActivity = Intent(this, NfcConfirmationActivity::class.java)
            confirmNFCDataActivity.putExtra("DOB", mrzResult?.dob)
            confirmNFCDataActivity.putExtra("DOE", mrzResult?.passportExpiration)
            confirmNFCDataActivity.putExtra("DOCNUMBER", mrzResult?.passportNumber)
            confirmNFCDataActivity.putExtra("COUNTRY", mrzResult?.country)
            confirmNFCDataActivity.putExtra("THREELINE", mrzResult?.threeLineMrz)

            this.startActivity(confirmNFCDataActivity)
        } else if (result.resultCode == RESULT_CANCELED) {
            Log.d(TAG, "User canceled mrz capture")
        }
    }

    private fun showMrzCaptureCamera() {
        val cameraIntent = Intent(
            this@MainActivity,
            AcuantCameraActivity::class.java
        )
        cameraIntent.putExtra(ACUANT_EXTRA_CAMERA_OPTIONS,
            AcuantCameraOptions
                .MrzCameraOptionsBuilder()
                .build()
        )
        mrzCameraLauncher.launch(cameraIntent)
    }

    private var barcodeCameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            capturedBarcodeString = data?.getStringExtra(ACUANT_EXTRA_PDF417_BARCODE)
            setProgress(true, "Uploading...")
            val barcodeString = capturedBarcodeString
            if (barcodeString != null) {
                documentIdInstance?.uploadBarcode(BarcodeData(barcodeString), object : UploadBarcodeListener {
                    override fun barcodeUploaded() {
                        finishBarcodeOnlyCapture()
                    }

                    override fun onError(error: AcuantError) {
                        Log.d(TAG, "barcode upload failed")
                        finishBarcodeOnlyCapture()
                    }
                })
            } else {
                Log.d(TAG, "barcode was null")
                finishBarcodeOnlyCapture()
            }
        } else {
            Log.d(TAG, "User canceled barcode capture")
            finishBarcodeOnlyCapture()
        }
    }

    private fun finishBarcodeOnlyCapture() {
        val alert = AlertDialog.Builder(this@MainActivity)
        alert.setTitle("Message")
        if (livenessSelected != 0) {
            alert.setMessage("Capture Selfie Image now.")
        } else {
            alert.setMessage("Continue.")
        }
        alert.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
            setProgress(true, "Getting Data...")
            getData()
            showFaceCamera()
        }
        if (livenessSelected != 0) {
            alert.setNegativeButton("CANCEL") { dialog, _ ->
                setProgress(true, "Getting Data...")
                facialLivelinessResultString = "Facial Liveliness Failed"
                capturingSelfieImage = false
                getData()
                dialog.dismiss()
            }
        }
        alert.show()
    }

    private fun showBarcodeCaptureCamera() {
        capturedBarcodeString = null
        val cameraIntent = Intent(
                this@MainActivity,
                AcuantCameraActivity::class.java
        )
        cameraIntent.putExtra(ACUANT_EXTRA_CAMERA_OPTIONS,
                AcuantCameraOptions
                        .BarcodeCameraOptionsBuilder()
                        .build()
        )
        barcodeCameraLauncher.launch(cameraIntent)
    }

    private var confirmationLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data: Intent? = result.data

            val isFront = data!!.getBooleanExtra("isFrontImage", true)
            val isConfirmed = data.getBooleanExtra("Confirmed", true)
            if (isConfirmed) {
                if (isFront) {
                    capturedFrontImage = recentImage
                    if (isHealthCard) {
                        frontCaptured = true
                        showAcuDialog(R.string.scan_back_side_health_insurance_card, "Message",
                                { dialog, _ ->
                                    dialog.dismiss()
                                    showDocumentCaptureCamera()
                                }, { dialog, _ ->
                            dialog.dismiss()
                            uploadHealthCard()
                        })

                    } else {
                        uploadIdFront()
                    }
                } else {
                    capturedBackImage = recentImage
                    if (!isHealthCard) {
                        val alert = AlertDialog.Builder(this@MainActivity)
                        alert.setTitle("Message")
                        if (capturedBarcodeString != null && capturedBarcodeString!!.trim().isNotEmpty()) {
                            if (livenessSelected != 0) {
                                alert.setMessage("Following barcode is captured.\n\n"
                                        + "Barcode String :\n\n"
                                        + capturedBarcodeString!!.subSequence(0, (capturedBarcodeString!!.length * 0.25).toInt())
                                        + "...\n\n"
                                        + "Capture Selfie Image now.")
                            } else {
                                alert.setMessage("Following barcode is captured.\n\n"
                                        + "Barcode String :\n\n"
                                        + capturedBarcodeString!!.subSequence(0, (capturedBarcodeString!!.length * 0.25).toInt()))
                            }
                            alert.setPositiveButton("OK") { dialog, _ ->
                                dialog.dismiss()
                                setProgress(true, "Getting Data...")
                                uploadIdBack()
                                showFaceCamera()
                            }
                            if (livenessSelected != 0) {
                                alert.setNegativeButton("CANCEL") { dialog, _ ->
                                    setProgress(true, "Getting Data...")
                                    facialLivelinessResultString = "Facial Liveliness Failed"
                                    capturingSelfieImage = false
                                    uploadIdBack()
                                    dialog.dismiss()
                                }
                            }
                        } else if (barcodeExpected) {
                            alert.setMessage("A barcode was expected but was not captured. Please try capturing the barcode.")

                            alert.setPositiveButton("OK") { dialog, _ ->
                                dialog.dismiss()
                                setProgress(true, "Uploading...")
                                uploadIdBack()
                                showBarcodeCaptureCamera()
                            }
                        } else {
                            if (livenessSelected != 0) {
                                alert.setMessage("Capture Selfie Image now.")
                            } else {
                                alert.setMessage("Continue.")
                            }
                            alert.setPositiveButton("OK") { dialog, _ ->
                                dialog.dismiss()
                                setProgress(true, "Getting Data...")
                                uploadIdBack()
                                showFaceCamera()
                            }
                            if (livenessSelected != 0) {
                                alert.setNegativeButton("CANCEL") { dialog, _ ->
                                    setProgress(true, "Getting Data...")
                                    facialLivelinessResultString = "Facial Liveliness Failed"
                                    capturingSelfieImage = false
                                    uploadIdFront()
                                    dialog.dismiss()
                                }
                            }
                        }
                        alert.show()
                    } else {
                        uploadHealthCard()
                    }

                }
            } else {
                showDocumentCaptureCamera()
            }
        }
    }

    private fun showConfirmation(isFrontImage: Boolean) {
        val confirmationIntent = Intent(
                this@MainActivity,
                ConfirmationActivity::class.java
        )
        confirmationIntent.putExtra("isFrontImage", isFrontImage)
        if (recentImage != null) {
            image = recentImage!!.image
            confirmationIntent.putExtra("sharpness", recentImage!!.sharpness)
            confirmationIntent.putExtra("glare", recentImage!!.glare)
            confirmationIntent.putExtra("dpi", recentImage!!.dpi)
            confirmationIntent.putExtra("barcode", capturedBarcodeString)
        }
        confirmationLauncher.launch(confirmationIntent)
    }

    private var retryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            showDocumentCaptureCamera()
        }
    }

    private fun showClassificationError() {
        val classificationErrorIntent = Intent(
                this@MainActivity,
                ClassificationFailureActivity::class.java
        )
        if (recentImage != null) {
            image = recentImage!!.image
        }
        retryLauncher.launch(classificationErrorIntent)
    }

    private var faceCameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        when (result.resultCode) {
            RESULT_OK -> {
                val data = result.data
                val url = data?.getStringExtra(ACUANT_EXTRA_FACE_IMAGE_URL)

                if (url == null) {
                    showFaceCaptureError()
                } else {
                    processingFacialLiveness = true

                    val bytes = readFromFile(url)
                    capturedSelfieImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    val plData = PassiveLivenessData(capturedSelfieImage as Bitmap)
                    AcuantPassiveLiveness.processFaceLiveness(plData, object : PassiveLivenessListener {
                        override fun passiveLivenessFinished(result: PassiveLivenessResult) {
                            facialLivelinessResultString = when (result.livenessAssessment) {
                                AcuantPassiveLiveness.LivenessAssessment.Live -> {
                                    "Facial Liveliness: live"
                                }
                                AcuantPassiveLiveness.LivenessAssessment.NotLive -> {
                                    "Facial Liveliness: not live"
                                }
                                AcuantPassiveLiveness.LivenessAssessment.PoorQuality -> {
                                    "Facial Liveliness: poor quality image (unable to verify liveness)"
                                }
                                else -> {
                                    "Facial Liveliness Failed: ${result.errorCode} - ${result.errorDesc}"
                                }
                            }
                            processingFacialLiveness = false
                        }

                        override fun onError(error: AcuantError) {
                            Log.d(TAG, error.toString())
                            facialLivelinessResultString = "Facial Liveliness Error: ${error.errorCode} - ${error.errorDescription}"
                            processingFacialLiveness = false
                        }
                    })
                    processFacialMatch()
                }
            }
            RESULT_CANCELED -> {
                showFaceCaptureCanceled()
            }
            else -> {
                showFaceCaptureError()
            }
        }
    }

    private fun showFaceCapture() {
        val cameraIntent = Intent(
                this@MainActivity,
                AcuantFaceCameraActivity::class.java
        )

        //Optional, should only be used if you are changing some of the options,
        // pointless to pass default options
        //
        cameraIntent.putExtra(ACUANT_EXTRA_FACE_CAPTURE_OPTIONS, FaceCaptureOptions())
        faceCameraLauncher.launch(cameraIntent)
    }

    private var hgCameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        when (result.resultCode) {
            RESULT_OK -> {
                val data = result.data
                val url = data?.getStringExtra(ACUANT_EXTRA_FACE_IMAGE_URL)

                if (url == null) {
                    showFaceCaptureError()
                } else {
                    val bytes = readFromFile(url)
                    capturedSelfieImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    facialLivelinessResultString = "Facial Liveliness: true"
                    processFacialMatch()
                }
            }
            RESULT_CANCELED -> {
                showFaceCaptureCanceled()
            }
            else -> {
                showFaceCaptureError()
            }
        }
    }

    private fun showHGLiveness() {
        val cameraIntent = Intent(
            this@MainActivity,
            AcuantFaceCameraActivity::class.java
        )

        cameraIntent.putExtra(ACUANT_EXTRA_FACE_CAPTURE_OPTIONS, FaceCaptureOptions(cameraMode = CameraMode.HgLiveness, showOval = true))
        hgCameraLauncher.launch(cameraIntent)
    }

    //Show Front Camera to Capture Live Selfie
    fun showFaceCamera() {
        when (livenessSelected) {
            1 -> {
                capturingSelfieImage = true
                if (usingPassive) {
                    if (!isKeyless) {
                        showFaceCapture()
                    } else {
                        showHGLiveness()
                    }
                } else {
                    showIPLiveness()
                }
            }
            else -> {
                capturingSelfieImage = false
                //just go to results
            }
        }
    }

    private fun showIPLiveness() {
        setProgress(true, "Loading...")
        val task = AcuantIPLiveness.getFacialSetup(object : FacialSetupListener {

            override fun onDataReceived(result: FacialSetupResult) {
                setProgress(false)
                result.allowScreenshots = true
                AcuantIPLiveness.runFacialCapture(this@MainActivity, result, object : IPLivenessListener {
                    override fun onConnecting() {
                        setProgress(true, "Connecting...")
                    }

                    override fun onConnected() {
                        setProgress(false)
                    }

                    override fun onProgress(status: String, progress: Int) {
                        setProgress(true, "$progress%\n$status")
                    }

                    override fun onSuccess(userId: String, token: String, frame: Bitmap?) {
                        startFacialLivelinessRequest(token, userId)
                    }

                    override fun onFail(error: AcuantError) {
                        showFaceCaptureError()
                    }

                    override fun onCancel() {
                        setProgress(true, "Getting Data...")
                        capturingSelfieImage = false
                        facialLivelinessResultString = "Facial Liveliness Failed"
                    }

                    override fun onError(error: AcuantError) {
                        showAcuDialog(error)
                    }
                })
            }

            override fun onError(error: AcuantError) {
                setProgress(false)
                handleInternalError()
            }
        })
        backgroundTasks.add(task)
    }

    fun handleInternalError() {
        showAcuDialog("Would you like to retry?", "Internal Error", { dialog, _ ->
            dialog.dismiss()
            showFaceCamera()
        }, { dialog, _ ->
            dialog.dismiss()
            capturingSelfieImage = false
            facialLivelinessResultString = "Facial Liveliness Failed"
        })
    }

    //process health card images
    private fun uploadHealthCard() {
        setProgress(true, "Uploading...")
        val task = AcuantDocumentProcessor.createInstance(HealthInsuranceInstanceOptions(), object : CreateHealthInsuranceInstanceListener {
            override fun instanceCreated(instance: AcuantHealthInsuranceDocumentInstance) {
                documentHealthInstance = instance
                val frontData = if (capturedFrontImage?.rawBytes != null) {
                    EvaluatedImageData(capturedFrontImage!!.rawBytes)
                } else {
                    showAcuDialog("Image bytes were null.")
                    return
                }
                val willUploadBack = capturedBackImage?.rawBytes != null
                val task = instance.uploadFrontImage(frontData, object : UploadImageListener {
                    override fun imageUploaded() {
                        if (!willUploadBack || (willUploadBack && instance.hasBackBeenUploaded)) {
                            getHealthCardData()
                        }
                    }

                    override fun onError(error: AcuantError) {
                        showAcuDialog(error)
                    }
                })
                backgroundTasks.add(task)
                if (willUploadBack) {
                    val backData = EvaluatedImageData(capturedBackImage!!.rawBytes)
                    val task2 = instance.uploadBackImage(backData, object : UploadImageListener {
                        override fun imageUploaded() {
                            if (instance.hasFrontBeenUploaded) {
                                getHealthCardData()
                            }
                        }

                        override fun onError(error: AcuantError) {
                            showAcuDialog(error)
                        }
                    })
                    backgroundTasks.add(task2)
                }
            }

            override fun onError(error: AcuantError) {
                showAcuDialog(error)
            }
        })
        backgroundTasks.add(task)
    }

    // health card data
    fun getHealthCardData() {
        setProgress(true, "Processing...")
        // Get Data
        documentHealthInstance?.getData(object: GetHealthInsuranceDataListener {
            override fun processingResultReceived(result: HealthInsuranceCardResult) {
                setProgress(false)
                val resultStr = CommonUtils.stringFromHealthInsuranceResult(result)
                showHealthCardResults(result.frontImage, result.backImage,  resultStr)
                documentHealthInstance?.deleteInstance(object : DeleteListener {
                    override fun instanceDeleted() {
                        documentHealthInstance = null
                    }

                    override fun onError(error: AcuantError) {
                        documentHealthInstance = null
                        Log.d(TAG, "failed to delete instance $error")
                    }

                })
            }

            override fun onError(error: AcuantError) {
                showAcuDialog(error)
            }
        }) ?: showAcuDialog("Health Insurance Instance was unexpectedly null")
    }

    enum class NextStep {Front, Back}

    private fun createIdInstance(next: NextStep) {
        setProgress(true, "Creating Instance...")

        val idOptions = IdInstanceOptions()
        val task = AcuantDocumentProcessor.createInstance(idOptions, object : CreateIdInstanceListener {
            override fun instanceCreated(instance: AcuantIdDocumentInstance) {
                documentIdInstance = instance
                when (next) {
                    NextStep.Front -> uploadIdFront()
                    NextStep.Back -> uploadIdBack()
                }
            }

            override fun onError(error: AcuantError) {
                showAcuDialog(error)
            }
        })
        backgroundTasks.add(task)
    }

    private fun uploadIdFront() {
        val instance = documentIdInstance
        if (instance != null) {
            setProgress(true, "Uploading...")
            val frontData = if (capturedFrontImage?.rawBytes != null) {
                EvaluatedImageData(capturedFrontImage!!.rawBytes)
            } else {
                showAcuDialog("Image bytes were null.")
                return
            }
            val task = instance.uploadFrontImage(frontData, object : UploadImageListener {
                override fun imageUploaded() {
                    setProgress(true, "Classifying...")
                    val task = instance.getClassification(object : ClassificationListener {
                        override fun documentClassified(classified: Boolean, classification: Classification) {
                            setProgress(false)
                            if (classified) {
                                frontCaptured = true
                                barcodeExpected = classification.type?.referenceDocumentDataTypes?.contains(0) ?: false
                                if (isBackSideRequired(classification)) {
                                    this@MainActivity.runOnUiThread {
                                        showAcuDialog(R.string.scan_back_side_id, "Message", { dialog, _ ->
                                            dialog.dismiss()
                                            showDocumentCaptureCamera()
                                        }, { dialog, _ ->
                                            dialog.dismiss()
                                        })
                                    }
                                } else {
                                    val alert = AlertDialog.Builder(this@MainActivity)
                                    alert.setTitle("Message")
                                    if (livenessSelected != 0) {
                                        alert.setMessage("Capture Selfie Image")
                                    } else {
                                        alert.setMessage("Continue")
                                    }
                                    alert.setPositiveButton("OK") { dialog, _ ->
                                        dialog.dismiss()
                                        setProgress(true, "Getting Data...")
                                        showFaceCamera()
                                        getData()
                                    }
                                    if (livenessSelected != 0) {
                                        alert.setNegativeButton("CANCEL") { dialog, _ ->
                                            facialLivelinessResultString = "Facial Liveliness Failed"
                                            setProgress(true, "Getting Data...")
                                            getData()
                                            dialog.dismiss()
                                        }
                                    }
                                    alert.show()
                                }
                            } else {
                                showClassificationError()
                            }
                        }

                        override fun onError(error: AcuantError) {
                            showAcuDialog(error)
                        }
                    })
                    backgroundTasks.add(task)
                }

                override fun onError(error: AcuantError) {
                    showAcuDialog(error)
                }
            })
            backgroundTasks.add(task)
        } else {
            createIdInstance(NextStep.Front)
        }
    }

    private fun uploadIdBack() {
        val instance = documentIdInstance
        if (instance != null) {
            val backData = if (capturedBackImage?.rawBytes != null) {
                EvaluatedImageData(capturedBackImage!!.rawBytes)
            } else {
                showAcuDialog("Image bytes were null.")
                return
            }

            val task = instance.uploadBackImage(backData, object : UploadImageListener {
                override fun imageUploaded() {
                    if (barcodeExpected && capturedBarcodeString != null) {
                        val task = instance.uploadBarcode(BarcodeData(capturedBarcodeString!!), object : UploadBarcodeListener {
                            override fun barcodeUploaded() {
                                getData()
                            }

                            override fun onError(error: AcuantError) {
                                showAcuDialog(error)
                            }
                        })
                        backgroundTasks.add(task)
                    }
                }

                override fun onError(error: AcuantError) {
                    showAcuDialog(error)
                }
            })
            backgroundTasks.add(task)
        } else {
            createIdInstance(NextStep.Back)
        }
    }

    // Get data
    fun getData() {
        val instance = documentIdInstance
        if (instance != null) {
            val task = instance.getData(object : GetIdDataListener {
                override fun processingResultReceived(result: IDResult) {
                    if (result.fields == null || result.fields.dataFieldReferences == null) {
                        showAcuDialog("Unknown error happened.\nCould not extract data")
                        return
                    }
                    var docNumber = ""
                    var cardType = "ID1"
                    var frontImageUri: String? = null
                    var backImageUri: String? = null
                    var signImageUri: String? = null
                    var faceImageUri: String? = null
                    var resultString: String? = ""
                    val fieldReferences = result.fields.dataFieldReferences
                    for (reference in fieldReferences) {
                        if (reference.key == "Document Class Name" && reference.type == "string") {
                            if (reference.value == "Driver License") {
                                cardType = "ID1"
                            } else if (reference.value == "Passport") {
                                cardType = "ID3"
                            }
                        } else if (reference.key == "Document Number" && reference.type == "string") {
                            docNumber = reference.value
                        } else if (reference.key == "Photo" && reference.type == "uri") {
                            faceImageUri = reference.value
                        } else if (reference.key == "Signature" && reference.type == "uri") {
                            signImageUri = reference.value
                        }
                    }

                    for (image in result.images.images) {
                        if (image.side == 0) {
                            frontImageUri = image.uri
                        } else if (image.side == 1) {
                            backImageUri = image.uri
                        }
                    }

                    for (reference in fieldReferences) {
                        if (reference.type == "string") {
                            resultString = resultString + reference.key + ":" + reference.value + System.lineSeparator()
                        }
                    }

                    resultString = "Authentication Result : " +
                            AuthenticationResult.getString(Integer.parseInt(result.result)) +
                            System.lineSeparator() +
                            System.lineSeparator() +
                            resultString

                    thread {
                        val frontImage = loadAssureIDImage(frontImageUri, Credential.get())
                        val backImage = loadAssureIDImage(backImageUri, Credential.get())
                        val faceImage = loadAssureIDImage(faceImageUri, Credential.get())
                        val signImage = loadAssureIDImage(signImageUri, Credential.get())
                        capturedDocumentFaceImage = faceImage
                        MainActivity@ capturingImageData = false
                        while (capturingSelfieImage) {
                            Thread.sleep(100)
                        }
                        this@MainActivity.runOnUiThread {
                            instance.deleteInstance(object : DeleteListener {
                                override fun instanceDeleted() {
                                    documentIdInstance = null
                                }

                                override fun onError(error: AcuantError) {
                                    documentIdInstance = null
                                    Log.d(TAG, "failed to delete instance $error")
                                }

                            })
                            showResults(result.biographic.birthDate, result.biographic.expirationDate, docNumber, frontImage, backImage, faceImage, signImage, resultString, cardType)
                        }
                    }
                }

                override fun onError(error: AcuantError) {
                    showAcuDialog(error)
                }
            })
            backgroundTasks.add(task)
        } else {
            showAcuDialog("Document Id Instance was null unexpectedly")
        }
    }

    //process Facial Match
    fun processFacialMatch() {
        //MainActivity@ capturingFacialMatch = true
        thread {
            while (capturingImageData) {
                Thread.sleep(100)
            }
            this@MainActivity.runOnUiThread {
                val facialMatchData = FacialMatchData()
                facialMatchData.faceImageOne = capturedDocumentFaceImage
                facialMatchData.faceImageTwo = capturedSelfieImage

                if (facialMatchData.faceImageOne != null && facialMatchData.faceImageTwo != null) {
                    val task = AcuantFaceMatch.processFacialMatch(facialMatchData, object : FacialMatchListener {
                        override fun facialMatchFinished(result: FacialMatchResult) {
                            capturingSelfieImage = false
                            capturingFacialMatch = false

                            this@MainActivity.runOnUiThread {
                                facialResultString = "isMatch: ${result.isMatch}\n"
                                facialResultString += "score: ${result.score}\n"
                                facialResultString += "transactionId: ${result.transactionId}\n"
                            }
                        }

                        override fun onError(error: AcuantError) {
                            capturingSelfieImage = false
                            capturingFacialMatch = false
                            showAcuDialog(error)
                        }
                    })
                    backgroundTasks.add(task)
                } else {
                    capturingSelfieImage = false
                    capturingFacialMatch = false
                }
            }
        }
    }

    //Show Health card Results
    fun showHealthCardResults(frontImage: Bitmap?, backImage: Bitmap?, resultString: String?) {
        ProcessedData.cleanup()
        ProcessedData.frontImage = frontImage
        ProcessedData.backImage = backImage
        ProcessedData.isHealthCard = true
        ProcessedData.formattedString = resultString
        val resultIntent = Intent(
                this@MainActivity,
                ResultActivity::class.java
        )
        startActivity(resultIntent)
    }

    // Show ID Result
    fun showResults(dateOfBirth: String?, dateOfExpiry: String?, documentNumber: String?, frontImage: Bitmap?, backImage: Bitmap?, faceImage: Bitmap?, signImage: Bitmap?, resultString: String?, cardType: String) {
        ProcessedData.cleanup()
        ProcessedData.frontImage = frontImage
        ProcessedData.backImage = backImage
        ProcessedData.faceImage = faceImage
        ProcessedData.capturedFaceImage = capturedSelfieImage
        ProcessedData.signImage = signImage
        ProcessedData.dateOfBirth = dateOfBirth
        ProcessedData.dateOfExpiry = dateOfExpiry
        ProcessedData.documentNumber = documentNumber
        ProcessedData.cardType = cardType
        thread {
            while (capturingFacialMatch || processingFacialLiveness) {
                Thread.sleep(100)
            }
            this@MainActivity.runOnUiThread {
                facialResultString = if (facialResultString == null) "Facial Match Failed" else facialResultString
                ProcessedData.formattedString = (facialLivelinessResultString ?: "No Liveness Test Performed") + System.lineSeparator() + facialResultString+ System.lineSeparator() + resultString
                val resultIntent = Intent(
                        this@MainActivity,
                        ResultActivity::class.java
                )
                setProgress(false)
                startActivity(resultIntent)
            }
        }
    }

    //only run not on main thread
    fun loadAssureIDImage(url: String?, credential: Credential?): Bitmap? {
        if (url != null && credential != null) {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.setRequestProperty("Authorization", CredentialHelper.getAcuantAuthHeader(credential))
            conn.useCaches = false
            conn.connect()
            val img = BitmapFactory.decodeStream(conn.inputStream)
            conn.disconnect()
            return img
        }
        return null
    }

    fun isBackSideRequired(classification : Classification?): Boolean {
        var isBackSideScanRequired = false
        if (classification?.type != null && classification.type.supportedImages != null) {
            val list = classification.type.supportedImages as ArrayList<HashMap<String, Int>>
            for (i in list.indices) {
                val map = list[i]
                if (map["Light"] == 0) {
                    if (map["Side"] == 1) {
                        isBackSideScanRequired = true
                    }
                }
            }
        }
        return isBackSideScanRequired
    }

    companion object {
        var image: Bitmap? = null
        const val TAG = "Acuant Sample App"
    }

}