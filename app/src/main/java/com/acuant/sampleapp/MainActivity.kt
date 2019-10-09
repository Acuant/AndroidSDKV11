package com.acuant.sampleapp

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.ConnectivityManager
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Base64
import android.util.Log
import android.view.Surface
import android.view.View
import android.view.WindowManager
import android.widget.*
import com.acuant.acuantcamera.CapturedImage
import com.acuant.acuantcamera.camera.AcuantCameraActivity
import com.acuant.acuantcamera.camera.AcuantCameraOptions
import com.acuant.acuantcamera.constant.*
import com.acuant.acuantcommon.exception.AcuantException
import com.acuant.acuantcommon.initializer.AcuantInitializer
import com.acuant.acuantcommon.initializer.IAcuantPackageCallback
import com.acuant.acuantcommon.model.*
import com.acuant.acuantcommon.type.CardSide
import com.acuant.acuantdocumentprocessing.AcuantDocumentProcessor
import com.acuant.acuantdocumentprocessing.model.*
import com.acuant.acuantdocumentprocessing.service.listener.CreateInstanceListener
import com.acuant.acuantdocumentprocessing.service.listener.DeleteListener
import com.acuant.acuantdocumentprocessing.service.listener.GetDataListener
import com.acuant.acuantdocumentprocessing.service.listener.UploadImageListener
import com.acuant.acuantfacematchsdk.AcuantFaceMatch
import com.acuant.acuantfacematchsdk.model.FacialMatchData
import com.acuant.acuantfacematchsdk.service.FacialMatchListener
import com.acuant.acuanthgliveness.model.FaceCapturedImage
import com.acuant.acuantimagepreparation.initializer.ImageProcessorInitializer
import com.acuant.acuantipliveness.AcuantIPLiveness
import com.acuant.acuantipliveness.constant.FacialCaptureConstant
import com.acuant.acuantipliveness.facialcapture.model.FacialCaptureResult
import com.acuant.acuantipliveness.facialcapture.model.FacialSetupResult
import com.acuant.acuantipliveness.facialcapture.service.FacialCaptureCredentialListener
import com.acuant.acuantipliveness.facialcapture.service.FacialCaptureLisenter
import com.acuant.acuantipliveness.facialcapture.service.FacialSetupLisenter
import com.acuant.sampleapp.backgroundtasks.CroppingTask
import com.acuant.sampleapp.backgroundtasks.CroppingTaskListener
import com.acuant.sampleapp.utils.CommonUtils
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {
    //TODO: Swap away from deprecated method of showing progress
    private var progressDialog: LinearLayout? = null
    private var progressText: TextView? = null
    private var capturedFrontImage: Image? = null
    private var capturedBackImage: Image? = null
    private var capturedSelfieImage: Bitmap? = null
    private var capturedFaceImage: Bitmap? = null
    private var capturedBarcodeString: String? = null
    private var frontCaptured: Boolean = false
    private var isHealthCard: Boolean = false
    private var isRetrying: Boolean = false
    private var insruanceButton: Button? = null
    private var idButton: Button? = null
    private var capturingImageData: Boolean = true
    private var capturingSelfieImage: Boolean = false
    private var capturingFacialMatch: Boolean = false
    private var facialResultString: String? = null
    private var facialLivelinessResultString: String? = null
    private var captureWaitTime: Int = 0
    private var documentInstanceID: String? = null
    private var autoCaptureEnabled: Boolean = true
    private var numerOfClassificationAttempts: Int = 0
    private var isInitialized = false
    private var isIPLivenessEnabled = false

    fun cleanUpTransaction() {
        facialResultString = null
        capturedFrontImage = null
        capturedBackImage = null
        capturedSelfieImage = null
        capturedFaceImage = null
        capturedBarcodeString = null
        isHealthCard = false
        isRetrying = false
        capturingImageData = true
        documentInstanceID = null
        numerOfClassificationAttempts = 0

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        insruanceButton = findViewById(R.id.main_health_card)
        idButton = findViewById(R.id.main_id_passport)

        val autoCaptureSwitch = findViewById<Switch>(R.id.autoCaptureSwitch)
        autoCaptureSwitch.setOnCheckedChangeListener { _, isChecked ->
            autoCaptureEnabled = isChecked
        }

        progressDialog = findViewById(R.id.main_progress_layout)
        progressText = findViewById(R.id.pbText)

        setProgress(true, "Initializing...")

        initializeAcuantSdk(object: IAcuantPackageCallback{
            override fun onInitializeSuccess() {
                this@MainActivity.runOnUiThread {
                    isInitialized = true
                    setProgress(false)
                }
            }

            override fun onInitializeFailed(error: List<Error>) {
                this@MainActivity.runOnUiThread {
                    setProgress(false)
                    val alert = AlertDialog.Builder(this@MainActivity)
                    alert.setTitle("Error")
                    alert.setMessage("Could not initialize")
                    alert.setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                    }
                    alert.show()
                }
            }

        })
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

    private fun initializeAcuantSdk(callback:IAcuantPackageCallback){
        try{
           AcuantInitializer.intialize("acuant.config.xml", this, listOf(ImageProcessorInitializer()),
                    object: IAcuantPackageCallback{
                        override fun onInitializeSuccess() {
                            getFacialLivenessCredentials(callback)
                        }

                        override fun onInitializeFailed(error: List<Error>) {
                            callback.onInitializeFailed(error)
                        }

                    })

            // Or, if required to initialize without a config file , then can be done the following way
            /*Credential.init("xxxx",
                    "xxxx",
                    "xxxx",
                    "https://frm.acuant.net",
                    "https://services.assureid.net",
                    "https://medicscan.acuant.net")

            AcuantInitializer.intialize(null, this.applicationContext, listOf(ImageProcessorInitializer()),
                    object: IAcuantPackageCallback {
                        override fun onInitializeSuccess() {
                            getFacialLivenessCredentials(callback)
                        }

                        override fun onInitializeFailed(error: List<Error>) {
                            callback.onInitializeFailed(error)
                        }
                    })*/

        }
        catch(e: AcuantException){
            Log.e("Acuant Error", e.toString())
            setProgress(false)
            val alert = AlertDialog.Builder(this@MainActivity)
            alert.setTitle("Error")
            alert.setMessage(e.toString())
            alert.setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            alert.show()

        }
    }

    private fun getFacialLivenessCredentials(callback: IAcuantPackageCallback){
        AcuantIPLiveness.getFacialCaptureCredential(object:FacialCaptureCredentialListener{
            override fun onDataReceived(result: Boolean) {
                if(result){
                    runOnUiThread{
                        val isIPEnabledSwitch = findViewById<Switch>(R.id.isIPLivenessEnabled)
                        isIPEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
                            isIPLivenessEnabled = isChecked
                        }
                        isIPEnabledSwitch.visibility = View.VISIBLE
                    }
                }
                isIPLivenessEnabled = result
                callback.onInitializeSuccess()
            }

            override fun onError(errorCode: Int, description: String) {
                callback.onInitializeFailed(listOf())
            }
        })
    }

    private fun readFromFile(fileUri: String?): ByteArray{
        val file = File(fileUri)
        val bytes = ByteArray(file.length().toInt())
        try {
            val buf = BufferedInputStream(FileInputStream(file))
            buf.read(bytes, 0, bytes.size)
            buf.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: Exception){
            e.printStackTrace()
        }
        return bytes
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.REQUEST_CAMERA_PHOTO && resultCode == AcuantCameraActivity.RESULT_SUCCESS_CODE) {
            val bytes = readFromFile(data?.getStringExtra(ACUANT_EXTRA_IMAGE_URL))
            capturedBarcodeString = data?.getStringExtra(ACUANT_EXTRA_PDF417_BARCODE)
            setProgress(true, "Cropping...")

            val croppingTask = CroppingTask(BitmapFactory.decodeByteArray(bytes, 0, bytes.size), !frontCaptured, object : CroppingTaskListener {
                override fun croppingFinished(acuantImage: Image?, isFrontImage: Boolean) {
                    this@MainActivity.runOnUiThread {
                        setProgress(false)
                    }
                    CapturedImage.acuantImage = correctBitmapOrientation(acuantImage)
                    showConfirmation(isFrontImage, false)
                }
            })
            croppingTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null, null, null)

        }
        else if (resultCode == Constants.REQUEST_CONFIRMATION) {
            val isFront = data!!.getBooleanExtra("IsFrontImage", true)
            val isConfirmed = data.getBooleanExtra("Confirmed", true)
            if (isConfirmed) {
                if (isFront) {
                    capturedFrontImage = CapturedImage.acuantImage
                    if (isHealthCard) {
                        frontCaptured = true
                        val alert = AlertDialog.Builder(this@MainActivity)
                        alert.setTitle("Message")
                        //alert.setMessage("Capture back side of Health insurance card")
                        alert.setMessage(R.string.scan_back_side_health_insurance_card)
                        alert.setPositiveButton("OK") { dialog, _ ->
                            dialog.dismiss()
                            showDocumentCaptureCamera()
                        }
                        alert.setNegativeButton("SKIP") { dialog, _ ->
                            dialog.dismiss()
                            uploadHealthCard()
                        }
                        alert.show()

                    } else {
                        processFrontOfDocument()
                    }
                } else {
                    capturedBackImage = CapturedImage.acuantImage
                    if (!isHealthCard) {
                        val alert = AlertDialog.Builder(this@MainActivity)
                        alert.setTitle("Message")
                        if (capturedBarcodeString != null && capturedBarcodeString!!.trim().isNotEmpty()) {
                            alert.setMessage("Following barcode is captured.\n\n"
                                    + "Barcode String :\n\n"
                                    + capturedBarcodeString!!.subSequence(0, (capturedBarcodeString!!.length * 0.25).toInt())
                                    + "...\n\n"
                                    + "Capture Selfie Image now.")
                        }
                        else{
                            alert.setMessage("Capture Selfie Image now.")
                        }
                        alert.setPositiveButton("OK") { dialog, _ ->
                            dialog.dismiss()
                            setProgress(true, "Getting Data...")
                            uploadBackImageOfDocument()
                            showFrontCamera()
                        }
                        alert.setNegativeButton("CANCEL") { dialog, _ ->
                            setProgress(true, "Getting Data...")
                            facialLivelinessResultString = "Facial Liveliness Failed"
                            capturingSelfieImage = false
                            uploadBackImageOfDocument()
                            dialog.dismiss()
                        }
                        alert.show()
                    } else {
                        uploadHealthCard()
                    }

                }
            } else {
                showDocumentCaptureCamera()
            }
        } else if (resultCode == Constants.REQUEST_RETRY) {
            isRetrying = true
            showDocumentCaptureCamera()

        } else if (requestCode == Constants.REQUEST_CAMERA_IP_SELFIE) {
            when (resultCode) {
                ErrorCodes.ERROR_CAPTURING_FACIAL -> showFaceCaptureError()
                ErrorCodes.USER_CANCELED_FACIAL -> {
                    setProgress(true, "Getting Data...")
                    capturingSelfieImage = false
                    facialLivelinessResultString = "Facial Liveliness Failed"
                }
                else -> {
                    val userId = data?.getStringExtra(FacialCaptureConstant.ACUANT_USERID_KEY)!!
                    val token = data.getStringExtra(FacialCaptureConstant.ACUANT_TOKEN_KEY)!!
                    startFacialLivelinessRequest(token, userId)
                }
            }
        } else if (requestCode == Constants.REQUEST_CAMERA_HG_SELFIE){
            if(resultCode == FacialLivenessActivity.RESPONSE_SUCCESS_CODE){
                capturedSelfieImage = FaceCapturedImage.bitmapImage
                facialLivelinessResultString = "Facial Liveliness: true"
                processFacialMatch()
            }
            else{
                showFaceCaptureError()
            }
        }
    }

    private fun showFaceCaptureError(){
        val alert = AlertDialog.Builder(this@MainActivity)
        alert.setTitle("Error Capturing Face")
        alert.setMessage("Would you like to retry?")
        alert.setPositiveButton("YES") { dialog, _ ->
            dialog.dismiss()
            showFrontCamera()
        }
        alert.setNegativeButton("NO") { dialog, _ ->
            dialog.dismiss()
            capturingSelfieImage = false
            facialLivelinessResultString = "Facial Liveliness Failed"
        }
        alert.show()
    }

    private fun startFacialLivelinessRequest(token: String, userId:String){
        setProgress(true, "Getting Data...")
        AcuantIPLiveness.getFacialLiveness(
                token,
                userId,
                object: FacialCaptureLisenter {
                    override fun onDataReceived(result: FacialCaptureResult) {
                        facialLivelinessResultString = "Facial Liveliness: " + result.isPassed
                        val decodedString = Base64.decode(result.frame, Base64.DEFAULT)
                        capturedSelfieImage = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                        setProgress(false)
                        processFacialMatch()
                    }

                    override fun onError(errorCode:Int, errorDescription: String) {
                        capturingSelfieImage = false
                        setProgress(false)
                        facialLivelinessResultString = "Facial Liveliness Failed"
                        val alert = AlertDialog.Builder(this@MainActivity)
                        alert.setTitle("Error Retreiving Facial Data")
                        alert.setMessage(errorDescription)
                        alert.setPositiveButton("OK") { dialog, _ ->
                            dialog.dismiss()
                        }
                        alert.show()
                    }
                }
        )
    }

    private fun hasInternetConnection():Boolean{
        val connectivityManager= this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo=connectivityManager.activeNetworkInfo
        return networkInfo!=null && networkInfo.isConnected
    }

    // ID/Passport Clicked
    fun idPassPortClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        if(!hasInternetConnection()){
            val alert = AlertDialog.Builder(this@MainActivity)
            alert.setTitle("Error")
            alert.setMessage("No internet connection available.")
            alert.setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            alert.show()
        }
        else{
            if(isInitialized){
                frontCaptured = false
                cleanUpTransaction()
                captureWaitTime = 0
                showDocumentCaptureCamera()
            }
            else{
                setProgress(true, "Initializing...")
                initializeAcuantSdk(object: IAcuantPackageCallback{
                    override fun onInitializeSuccess() {
                        this@MainActivity.runOnUiThread {
                            isInitialized = true
                            setProgress(false)
                            frontCaptured = false
                            cleanUpTransaction()
                            captureWaitTime = 0
                            showDocumentCaptureCamera()
                        }
                    }

                    override fun onInitializeFailed(error: List<Error>) {
                        this@MainActivity.runOnUiThread {
                            setProgress(false)
                            val alert = AlertDialog.Builder(this@MainActivity)
                            alert.setTitle("Error")
                            alert.setMessage("Could not initialize")
                            alert.setPositiveButton("OK") { dialog, _ ->
                                dialog.dismiss()
                            }
                            alert.show()
                        }
                    }

                })
            }
        }
    }

    // Health Insurance Clicked
    fun healthInsuranceClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        if(!hasInternetConnection()){
            val alert = AlertDialog.Builder(this@MainActivity)
            alert.setTitle("Error")
            alert.setMessage("No internet connection available.")
            alert.setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            alert.show()
        }
        else{
            if(isInitialized){
                frontCaptured = false
                cleanUpTransaction()
                isHealthCard = true
                captureWaitTime = 0
                showDocumentCaptureCamera()
            }
            else{
                setProgress(true, "Initializing...")
                initializeAcuantSdk(object: IAcuantPackageCallback{
                    override fun onInitializeSuccess() {
                        this@MainActivity.runOnUiThread {
                            isInitialized = true
                            frontCaptured = false
                            cleanUpTransaction()
                            isHealthCard = true
                            captureWaitTime = 0
                            showDocumentCaptureCamera()
                            setProgress(false)
                        }
                    }

                    override fun onInitializeFailed(error: List<Error>) {
                        this@MainActivity.runOnUiThread {
                            setProgress(false)
                            val alert = AlertDialog.Builder(this@MainActivity)
                            alert.setTitle("Error")
                            alert.setMessage("Could not initialize")
                            alert.setPositiveButton("OK") { dialog, _ ->
                                dialog.dismiss()
                            }
                            alert.show()
                        }
                    }

                })
            }

        }
    }

    //Show Rear Camera to Capture Image of ID,Passport or Health Insruance Card
    fun showDocumentCaptureCamera() {
        CapturedImage.barcodeString = null
        val cameraIntent = Intent(
                this@MainActivity,
                AcuantCameraActivity::class.java
        )
        cameraIntent.putExtra(ACUANT_EXTRA_CAMERA_OPTIONS,
                AcuantCameraOptions(allowBox = true, autoCapture = autoCaptureEnabled)
        )
        startActivityForResult(cameraIntent, Constants.REQUEST_CAMERA_PHOTO)
    }

    //Show Front Camera to Capture Live Selfie
    fun showFrontCamera() {
        try{
            capturingSelfieImage = true

            if(isIPLivenessEnabled){
                showIPLiveness()
            }
            else{
                showHGLiveness()
            }

        }
        catch(e: Exception){
            e.printStackTrace()
        }
    }

    private fun showIPLiveness(){
        setProgress(true, "Loading...")
        AcuantIPLiveness.getFacialSetup(object :FacialSetupLisenter{
            override fun onDataReceived(result: FacialSetupResult?) {
                setProgress(false)
                if(result != null){
                    val facialIntent = AcuantIPLiveness.getFacialCaptureIntent(this@MainActivity, result)
                    startActivityForResult(facialIntent, Constants.REQUEST_CAMERA_IP_SELFIE)
                }
                else{
                    handleInternalError()
                }
            }

            override fun onError(errorCode: Int, description: String?) {
                setProgress(false)
                handleInternalError()
            }
        })
    }
    private fun showHGLiveness(){
        val cameraIntent = Intent(
                this@MainActivity,
                FacialLivenessActivity::class.java
        )
        startActivityForResult(cameraIntent, Constants.REQUEST_CAMERA_HG_SELFIE)
    }

    fun handleInternalError(){
        runOnUiThread{
            val alert = AlertDialog.Builder(this@MainActivity)
            alert.setTitle("Internal Error")
            alert.setMessage("Would you like to retry?")
            alert.setNegativeButton("Proceed") { dialog, _ ->
                dialog.dismiss()
                capturingSelfieImage = false
                facialLivelinessResultString = "Facial Liveliness Failed"
            }
            alert.setPositiveButton("Retry") { dialog, _ ->
                dialog.dismiss()
                showFrontCamera()
            }
            alert.show()
        }
    }

    //process health card images
    private fun uploadHealthCard() {
        this@MainActivity.runOnUiThread {
            setProgress(true, "Processing...")
        }
        val idOptions = IdOptions()
        idOptions.cardSide = CardSide.Front
        idOptions.isHealthCard = true
        idOptions.isRetrying = false
        val idData = IdData()
        idData.image = capturedFrontImage!!.image

        AcuantDocumentProcessor.createInstance(idOptions, object : CreateInstanceListener {
            override fun instanceCreated(instanceId: String?, error: Error?) {
                if (error == null) {
                    // Success : Instance Created
                    documentInstanceID = instanceId
                    // Upload front image
                    AcuantDocumentProcessor.uploadImage(documentInstanceID,idData,idOptions, object : UploadImageListener {
                        override fun imageUploaded(error: Error?,classification: Classification?) {
                            if (error == null) {
                                if (capturedBackImage!=null && capturedBackImage!!.image != null) {
                                    // Upload back image
                                    idData.image = capturedBackImage!!.image
                                    idOptions.cardSide = CardSide.Back
                                    AcuantDocumentProcessor.uploadImage(documentInstanceID, idData, idOptions, object: UploadImageListener {
                                        override fun imageUploaded(error: Error?,classification: Classification?) {
                                            if (error == null) {
                                                getHealthCardData()
                                            } else {
                                                setProgress(false)
                                                val alert = AlertDialog.Builder(this@MainActivity)
                                                alert.setTitle("Error")
                                                alert.setMessage(error.errorDescription)
                                                alert.setPositiveButton("OK") { dialog, _ ->
                                                    dialog.dismiss()
                                                }
                                                alert.show()
                                            }
                                        }

                                    })
                                }else{
                                    getHealthCardData()
                                }
                            }else {
                                setProgress(false)
                                val alert = AlertDialog.Builder(this@MainActivity)
                                alert.setTitle("Error")
                                alert.setMessage(error.errorDescription)
                                alert.setPositiveButton("OK") { dialog, _ ->
                                    dialog.dismiss()
                                }
                                alert.show()

                            }
                        }

                    })

                } else {
                    // Failure
                    this@MainActivity.runOnUiThread {
                        setProgress(false)
                        val alert = AlertDialog.Builder(this@MainActivity)
                        alert.setTitle("Error")
                        alert.setMessage(error.errorDescription)
                        alert.setPositiveButton("OK") { dialog, _ ->
                            dialog.dismiss()
                        }
                        alert.show()
                    }

                }
            }

        })

    }

    // health card data
    fun getHealthCardData(){
        // Get Data
        AcuantDocumentProcessor.getData(documentInstanceID, true, object: GetDataListener {
            override fun processingResultReceived(result: ProcessingResult?) {
                this@MainActivity.runOnUiThread {
                    setProgress(false)
                }
                if (result == null || result.error != null) {
                    val alert = AlertDialog.Builder(this@MainActivity)
                    alert.setTitle("Error")
                    if (result != null) {
                        alert.setMessage(result.error.errorDescription)
                    } else {
                        alert.setMessage(ErrorDescriptions.ERROR_DESC_CouldNotGetHealthCardData)
                    }
                    alert.setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                    }
                    alert.show()

                } else {
                    val healthCardResult = result as HealthInsuranceCardResult
                    val resultStr = CommonUtils.stringFromResult(healthCardResult)
                    showHealthCardResults(null, null, null, healthCardResult.frontImage, healthCardResult.backImage, null, null, resultStr)
                    AcuantDocumentProcessor.deleteInstance(healthCardResult.instanceID, DeleteType.MedicalCard, object : DeleteListener
                    {
                        override fun instanceDeleted(success: Boolean) {
                            if (success) {
                                Log.d("DELETE", "Medical Card Instance Deleted successfully")
                            }
                        }

                    })

                }
            }

        })
    }


    // Process Front image
    private fun processFrontOfDocument() {
        this@MainActivity.runOnUiThread {
            setProgress(true, "Uploading  & Classifying...")
        }

        val idOptions = IdOptions()
        idOptions.cardSide = CardSide.Front
        idOptions.isHealthCard = false
        idOptions.isRetrying = isRetrying

        val idData = IdData()
        idData.image = capturedFrontImage!!.image

        if (isRetrying) {
            //CommonUtils.saveImage(idData.image,"second")
            uploadFrontImageOfDocument(documentInstanceID!!, idData, idOptions)

        } else {
            AcuantDocumentProcessor.createInstance(idOptions, object: CreateInstanceListener{
                override fun instanceCreated(instanceId: String?, error: Error?) {
                    if (error == null) {
                        // Success : Instance Created
                        documentInstanceID = instanceId
                        uploadFrontImageOfDocument(instanceId!!, idData, idOptions)

                    } else {
                        // Failure
                        this@MainActivity.runOnUiThread {
                            setProgress(false)
                            val alert = AlertDialog.Builder(this@MainActivity)
                            alert.setTitle("Error")
                            alert.setMessage(error.errorDescription)
                            alert.setPositiveButton("OK") { dialog, _ ->
                                dialog.dismiss()
                            }
                            alert.show()
                        }

                    }
                }
            })
        }
    }

    // Upload front Image of Driving License
    fun uploadFrontImageOfDocument(instanceId: String, idData: IdData, idOptions: IdOptions) {
        numerOfClassificationAttempts += 1
        // Upload front Image of DL
        Log.d("InstanceId:",instanceId)
        AcuantDocumentProcessor.uploadImage(instanceId, idData, idOptions, object:UploadImageListener{
                override fun imageUploaded(error: Error?, classification: Classification?) {
                    if (error == null) {
                        // Successfully uploaded
                        setProgress(false)
                        frontCaptured = true
                        if (isBackSideRequired(classification)) {
                            this@MainActivity.runOnUiThread {
                                val alert = AlertDialog.Builder(this@MainActivity)
                                alert.setTitle("Message")
                                //alert.setMessage("Scan back side of document.")
                                alert.setMessage(R.string.scan_back_side_id)
                                alert.setPositiveButton("OK") { dialog, _ ->
                                    dialog.dismiss()
                                    captureWaitTime = 2
                                    showDocumentCaptureCamera()
                                }
                                alert.setNegativeButton("CANCEL") { dialog, _ ->
                                    dialog.dismiss()
                                }
                                alert.show()
                            }
                        } else {
                            val alert = AlertDialog.Builder(this@MainActivity)
                            alert.setTitle("Message")
                            alert.setMessage("Capture Selfie Image")
                            alert.setPositiveButton("OK") { dialog, _ ->
                                dialog.dismiss()
                                setProgress(true, "Getting Data...")
                                showFrontCamera()
                                getData()
                            }
                            alert.setNegativeButton("CANCEL") { dialog, _ ->
                                facialLivelinessResultString = "Facial Liveliness Failed"
                                setProgress(true, "Getting Data...")
                                getData()
                                dialog.dismiss()
                            }
                            alert.show()
                        }

                    } else {
                        // Failure
                        this@MainActivity.runOnUiThread {
                            setProgress(false)
                            if (error.errorCode == ErrorCodes.ERROR_CouldNotClassifyDocument) {
                                showClassificationError()
                            } else {
                                setProgress(false)
                                val alert = AlertDialog.Builder(this@MainActivity)
                                alert.setTitle("Error")
                                alert.setMessage(error.errorDescription)
                                alert.setPositiveButton("OK") { dialog, _ ->
                                    dialog.dismiss()
                                }
                                alert.show()
                            }
                        }
                    }
                }

            })
    }


    //Upload Back side of Driving License
    private fun uploadBackImageOfDocument() {

        val idOptions = IdOptions()
        idOptions.cardSide = CardSide.Back
        idOptions.isHealthCard = false
        idOptions.isRetrying = false

        val idData = IdData()
        idData.image = capturedBackImage!!.image
        idData.barcodeString = capturedBarcodeString

        AcuantDocumentProcessor.uploadImage(documentInstanceID, idData, idOptions, object:UploadImageListener{
            override fun imageUploaded(error: Error?, classification: Classification?) {
                if (error == null) {
                    getData()
                }
            }
        })
    }

    // Get data
    fun getData() {
        AcuantDocumentProcessor.getData(documentInstanceID,false, object : GetDataListener {
            override fun processingResultReceived(result: ProcessingResult?) {
                if (result == null || result.error != null) {
                    this@MainActivity.runOnUiThread {
                        setProgress(false)
                    }
                    val alert = AlertDialog.Builder(this@MainActivity)
                    alert.setTitle("Error")
                    if (result != null) {
                        alert.setMessage(result.error.errorDescription)
                    } else {
                        alert.setMessage(ErrorDescriptions.ERROR_DESC_CouldNotGetConnectData)
                    }
                    alert.setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                    }
                    alert.show()
                    return
                } else if ((result as IDResult).fields == null || result.fields.dataFieldReferences == null) {
                    this@MainActivity.runOnUiThread {
                        setProgress(false)
                    }
                    val alert = AlertDialog.Builder(this@MainActivity)
                    alert.setTitle("Error")
                    alert.setMessage("Unknown error happened.Could not extract data")
                    alert.setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                    }
                    alert.show()
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
                    capturedFaceImage = faceImage
                    MainActivity@ capturingImageData = false
                    while (capturingSelfieImage) {
                        Thread.sleep(100)
                    }
                    this@MainActivity.runOnUiThread {
                        setProgress(false)
                        showResults(result.biographic.birthDate, result.biographic.expirationDate, docNumber, frontImage, backImage, faceImage, signImage, resultString, cardType)
                    }
                }
            }
        })
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
                facialMatchData.faceImageOne = capturedFaceImage
                facialMatchData.faceImageTwo = capturedSelfieImage

                if(facialMatchData.faceImageOne != null && facialMatchData.faceImageTwo != null){
                    setProgress(true, "Matching selfie...")
                    AcuantFaceMatch.processFacialMatch(facialMatchData, FacialMatchListener { result ->
                        this@MainActivity.runOnUiThread {
                            setProgress(false)
                            if (result!!.error == null) {
                                val resultStr = CommonUtils.stringFromFacialMatchResult(result)
                                facialResultString = resultStr
                            } else {
                                val alert = AlertDialog.Builder(this@MainActivity)
                                alert.setTitle("Error")
                                alert.setMessage(result.error.errorDescription)
                                alert.setPositiveButton("OK") { dialog, _ ->
                                    dialog.dismiss()
                                }
                                alert.show()
                            }
                        }
                        capturingSelfieImage = false
                        //capturingFacialMatch = false
                    })
                }
                else{
                    capturingSelfieImage = false
                    //capturingFacialMatch = false
                }
            }
        }
    }

    //Show Confirmation UI
    fun showConfirmation(isFrontImage: Boolean, isBarcode: Boolean) {
        val confirmationIntent = Intent(
                this@MainActivity,
                ConfirmationActivity::class.java
        )
        confirmationIntent.putExtra("IsFrontImage", isFrontImage)
        confirmationIntent.putExtra("IsBarcode", isBarcode)
        startActivityForResult(confirmationIntent, Constants.REQUEST_CONFIRMATION)
    }

    //Show Classification Error
    fun showClassificationError() {
        val classificationErrorIntent = Intent(
                this@MainActivity,
                ClassificationFailureActivity::class.java
        )
        startActivityForResult(classificationErrorIntent, Constants.REQUEST_RETRY)
    }

    //Show Health card Results
    fun showHealthCardResults(dateOfBirth: String?, dateOfExpiry: String?, documentNumber: String?, frontImage: Bitmap?, backImage: Bitmap?, faceImage: Bitmap?, signImage: Bitmap?, resultString: String?) {
        ProcessedData.cleanup()
        ProcessedData.frontImage = frontImage
        ProcessedData.backImage = backImage
        ProcessedData.faceImage = faceImage
        ProcessedData.signImage = signImage
        ProcessedData.dateOfBirth = dateOfBirth
        ProcessedData.dateOfExpiry = dateOfExpiry
        ProcessedData.documentNumber = documentNumber
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
        ProcessedData.signImage = signImage
        ProcessedData.dateOfBirth = dateOfBirth
        ProcessedData.dateOfExpiry = dateOfExpiry
        ProcessedData.documentNumber = documentNumber
        ProcessedData.cardType = cardType
        if (!isHealthCard) {
            thread {
                while (capturingFacialMatch) {
                    Thread.sleep(100)
                }
                this@MainActivity.runOnUiThread {
                    facialResultString = if(facialResultString == null) "Facial Match Failed" else facialResultString
                    ProcessedData.formattedString = facialLivelinessResultString + System.lineSeparator() + facialResultString + System.lineSeparator() + resultString
                    val resultIntent = Intent(
                            this@MainActivity,
                            ResultActivity::class.java
                    )
                    setProgress(false)
                    startActivity(resultIntent)
                }

            }
            return
        }
        ProcessedData.formattedString = resultString
        val resultIntent = Intent(
                this@MainActivity,
                ResultActivity::class.java
        )
        setProgress(false)
        startActivity(resultIntent)
    }

    //Correct orientation
    fun correctBitmapOrientation(image: Image?): Image? {
        if (image?.image != null && image.image.height > image.image.width) {
            val mWindowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            val display = mWindowManager.defaultDisplay
            val angle = when (display.rotation) {
                Surface.ROTATION_0 // This is display orientation
                -> 270 // This is camera orientation
                Surface.ROTATION_90 -> 180
                Surface.ROTATION_180 -> 90
                Surface.ROTATION_270 -> 0
                else -> 180
            }

            val matrix = Matrix()
            matrix.postRotate(angle.toFloat())
            image.image = Bitmap.createBitmap(image.image, 0, 0, image.image.width, image.image.height, matrix, true)
            return image
        }
        return image
    }

    fun loadAssureIDImage(url: String?, credential: Credential?): Bitmap? {
        if (url != null && credential != null) {
            val c = URL(url).openConnection() as HttpURLConnection
            val userpass = credential.username + ":" + credential.password
            val basicAuth = "Basic " + String(Base64.encode(userpass.toByteArray(), Base64.DEFAULT))
            c.setRequestProperty("Authorization", basicAuth)
            c.useCaches = false
            c.connect()
            return BitmapFactory.decodeStream(c.inputStream)
        }
        return null
    }

    fun isBackSideRequired(classification : Classification?):Boolean{
        var isBackSideScanRequired = false
        if (classification?.type != null && classification.type.supportedImages != null) {
            val list = classification.type.supportedImages as ArrayList<HashMap<*, *>>
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

}