package com.acuant.sampleapp

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Base64
import android.util.DisplayMetrics
import android.util.Log
import android.view.Surface
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.Switch
import com.acuant.acuantcamera.CapturedImage
import com.acuant.acuantcamera.camera.AcuantCameraActivity
import com.acuant.acuantcamera.constant.ACUANT_EXTRA_IMAGE_URL
import com.acuant.acuantcamera.constant.ACUANT_EXTRA_IS_AUTO_CAPTURE
import com.acuant.acuantcamera.constant.ACUANT_EXTRA_PDF417_BARCODE
import com.acuant.acuantcommon.model.*
import com.acuant.acuantcommon.type.CardSide
import com.acuant.acuantcommon.type.ProcessingMode
import com.acuant.acuantdocumentprocessing.AcuantDocumentProcessor
import com.acuant.acuantdocumentprocessing.model.*
import com.acuant.acuantdocumentprocessing.service.CreateInstanceListener
import com.acuant.acuantdocumentprocessing.service.DeleteListener
import com.acuant.acuantdocumentprocessing.service.GetDataListener
import com.acuant.acuantdocumentprocessing.service.UploadImageListener
import com.acuant.acuantfacematchsdk.AcuantFaceMatch
import com.acuant.acuantfacematchsdk.model.FacialMatchData
import com.acuant.acuantfacematchsdk.model.FacialMatchResult
import com.acuant.acuantfacematchsdk.service.FacialMatchListener
import com.acuant.acuanthgliveliness.FacialLivelinessActivity
import com.acuant.acuanthgliveliness.model.FaceCapturedImage
import com.acuant.sampleapp.backgroundtasks.CroppingTask
import com.acuant.sampleapp.backgroundtasks.CroppingTaskListener
import com.acuant.sampleapp.utils.CommonUtils
import com.acuant.sampleapp.utils.DialogUtils
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {
    private var progressDialog: ProgressDialog? = null
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
        autoCaptureSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            autoCaptureEnabled = isChecked
        }
    }

    private fun readFromFile(fileUri: String?): ByteArray{
        val file = File(fileUri)
        val bytes = ByteArray(file.length().toInt())
        try {
            val buf = BufferedInputStream(FileInputStream(file))
            buf.read(bytes, 0, bytes.size)
            buf.close()
        } catch (e: FileNotFoundException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } catch (e: IOException) {
            // TODO Auto-generated catch block
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
            progressDialog = DialogUtils.showProgessDialog(this, "Cropping...")
            val croppingTask = CroppingTask(BitmapFactory.decodeByteArray(bytes, 0, bytes.size), isHealthCard, true, !frontCaptured, object : CroppingTaskListener {
                override fun croppingFinished(acuantImage: Image?, isFrontImage: Boolean) {
                    this@MainActivity.runOnUiThread {
                        DialogUtils.dismissDialog(progressDialog)
                    }
                    CapturedImage.acuantImage = correctBitmapOrientation(acuantImage)
                    showConfirmation(isFrontImage, false)
                }
            })
            croppingTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null, null, null)

        }
        else if (resultCode == Constants.REQUEST_CONFIRMATION) {
            val isFront = data!!.getBooleanExtra("IsFrontImage", true)
            val isConfirmed = data!!.getBooleanExtra("Confirmed", true)
            val isBarcode = data!!.getBooleanExtra("IsBarcode", false)
            if (isConfirmed) {
                if (isFront) {
                    capturedFrontImage = CapturedImage.acuantImage
                    if (isHealthCard) {
                        frontCaptured = true
                        val alert = AlertDialog.Builder(this@MainActivity)
                        alert.setTitle("Message")
                        alert.setMessage("Capture back side of Health insurance card")
                        alert.setPositiveButton("OK") { dialog, whichButton ->
                            dialog.dismiss()
                            showDocumentCaptureCamera()
                        }
                        alert.setNegativeButton("SKIP") { dialog, whichButton ->
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
                        if (capturedBarcodeString != null && capturedBarcodeString!!.trim()!!.length > 0) {
                            alert.setMessage("Following barcode is captured.\n\n"
                                    + "Barcode String :\n\n"
                                    + capturedBarcodeString!!.subSequence(0, (capturedBarcodeString!!.length * 0.25).toInt())
                                    + "...\n\n"
                                    + "Capture Selfie Image now.")
                        }
                        else{
                            alert.setMessage("Capture Selfie Image now.")
                        }
                        alert.setPositiveButton("OK") { dialog, whichButton ->
                            dialog.dismiss()
                            uploadBackImageOfDocument()
                            showFrontCamera()
                        }
                        alert.setNegativeButton("CANCEL") { dialog, whichButton ->
                            progressDialog = DialogUtils.showProgessDialog(this@MainActivity, "Getting Data...")
                            facialLivelinessResultString = "Facial Liveliness Failed"
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

        } else if (resultCode == Constants.REQUEST_CAMERA_SELFIE) {
            //capturingSelfieImage = false
            capturedSelfieImage = FaceCapturedImage.bitmapImage
            processFacialMatch()
        }
    }

    // ID/Passport Clicked
    fun idPassPortClicked(view: View) {
        frontCaptured = false
        cleanUpTransaction()
        captureWaitTime = 0
        showDocumentCaptureCamera()
    }

    // Health Insurance Clicked
    fun healthInsuranceClicked(view: View) {
        frontCaptured = false
        cleanUpTransaction()
        isHealthCard = true
        captureWaitTime = 0
        showDocumentCaptureCamera()
    }

    //Show Rear Camera to Capture Image of ID,Passport or Health Insruance Card
    fun showDocumentCaptureCamera() {
        CapturedImage.barcodeString = null
        val cameraIntent = Intent(
                this@MainActivity,
                AcuantCameraActivity::class.java
        )
        cameraIntent.putExtra(ACUANT_EXTRA_IS_AUTO_CAPTURE, autoCaptureEnabled)
        startActivityForResult(cameraIntent, Constants.REQUEST_CAMERA_PHOTO)
    }

    fun showFrontCamera() {
        capturingSelfieImage = true;
        val cameraIntent = Intent(
                this@MainActivity,
                FacialLivelinessActivity::class.java
        )
        startActivityForResult(cameraIntent, Constants.REQUEST_CAMERA_SELFIE)
    }

    //process health card images
    fun uploadHealthCard() {
        this@MainActivity.runOnUiThread {
            progressDialog = DialogUtils.showProgessDialog(this, "Processing ...")
        }
        val idOptions = IdOptions()
        idOptions.cardSide = CardSide.Front
        idOptions.isHealthCard = true
        idOptions.isRetrying = false
        idOptions.processingMode = ProcessingMode.Authentication
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
                                    AcuantDocumentProcessor.uploadImage(documentInstanceID, idData, idOptions, object: UploadImageListener{
                                        override fun imageUploaded(error: Error?,classification: Classification?) {
                                            if (error == null) {
                                                getHealthCardData()
                                            } else {
                                                DialogUtils.dismissDialog(progressDialog)
                                                val alert = AlertDialog.Builder(this@MainActivity)
                                                alert.setTitle("Error")
                                                alert.setMessage(error.errorDescription)
                                                alert.setPositiveButton("OK") { dialog, whichButton ->
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
                                DialogUtils.dismissDialog(progressDialog)
                                val alert = AlertDialog.Builder(this@MainActivity)
                                alert.setTitle("Error")
                                alert.setMessage(error!!.errorDescription)
                                alert.setPositiveButton("OK") { dialog, whichButton ->
                                    dialog.dismiss()
                                }
                                alert.show()

                            }
                        }

                    })

                } else {
                    // Failure
                    this@MainActivity.runOnUiThread {
                        DialogUtils.dismissDialog(progressDialog)
                        val alert = AlertDialog.Builder(this@MainActivity)
                        alert.setTitle("Error")
                        alert.setMessage(error.errorDescription)
                        alert.setPositiveButton("OK") { dialog, whichButton ->
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
                    DialogUtils.dismissDialog(progressDialog)
                }
                if (result == null || result.error != null) {
                    val alert = AlertDialog.Builder(this@MainActivity)
                    alert.setTitle("Error")
                    if (result != null) {
                        alert.setMessage(result.error.errorDescription)
                    } else {
                        alert.setMessage(ErrorDescriptions.ERROR_DESC_CouldNotGetHealthCardData)
                    }
                    alert.setPositiveButton("OK") { dialog, whichButton ->
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
                            if (!success) {
                                // Handle error
                            } else {
                                Log.d("DELETE", "Medical Card Instance Deleted successfully")
                            }
                        }

                    })

                }
            }

        })
    }


    // Process Front image
    fun processFrontOfDocument() {
        this@MainActivity.runOnUiThread {
            progressDialog = DialogUtils.showProgessDialog(this, "Uploading  & Classifying...")
        }

        val idOptions = IdOptions()
        idOptions.cardSide = CardSide.Front
        idOptions.isHealthCard = false
        idOptions.isRetrying = isRetrying
        idOptions.processingMode = ProcessingMode.Authentication

        val idData = IdData()
        idData.image = capturedFrontImage!!.image

        if (isRetrying) {
            //CommonUtils.saveImage(idData.image,"second")
            uploadFrontImageOfDocument(documentInstanceID!!, idData, idOptions)

        } else {
            AcuantDocumentProcessor.createInstance(idOptions) { instanceId, error ->
                if (error == null) {
                    // Success : Instance Created
                    documentInstanceID = instanceId
                    uploadFrontImageOfDocument(instanceId!!, idData, idOptions)

                } else {
                    // Failure
                    this@MainActivity.runOnUiThread {
                        DialogUtils.dismissDialog(progressDialog)
                        val alert = AlertDialog.Builder(this@MainActivity)
                        alert.setTitle("Error")
                        alert.setMessage(error.errorDescription)
                        alert.setPositiveButton("OK") { dialog, whichButton ->
                            dialog.dismiss()
                        }
                        alert.show()
                    }

                }
            }
        }
    }

    // Upload front Image of Driving License
    fun uploadFrontImageOfDocument(instanceId: String, idData: IdData, idOptions: IdOptions) {
        numerOfClassificationAttempts = numerOfClassificationAttempts + 1
        // Upload front Image of DL
        Log.d("InstanceId:",instanceId)
        AcuantDocumentProcessor.uploadImage(instanceId, idData, idOptions) { error, classification ->
            if (error == null || (error.errorCode==ErrorCodes.ERROR_CouldNotClassifyDocument && numerOfClassificationAttempts>1)) {
                // Successfully uploaded
                DialogUtils.dismissDialog(progressDialog)
                frontCaptured = true
                if(isBackSideRequired(classification)) {
                    this@MainActivity.runOnUiThread {
                        val alert = AlertDialog.Builder(this@MainActivity)
                        alert.setTitle("Message")
                        alert.setMessage("Scan back side of document.")
                        alert.setPositiveButton("OK") { dialog, whichButton ->
                            dialog.dismiss()
                            captureWaitTime = 2
                            showDocumentCaptureCamera()
                        }
                        alert.setNegativeButton("CANCEL") { dialog, whichButton ->
                            dialog.dismiss()
                        }
                        alert.show()
                    }
                }else{
                    val alert = AlertDialog.Builder(this@MainActivity)
                    alert.setTitle("Message")
                    alert.setMessage("Capture Selfie Image")
                    alert.setPositiveButton("OK") { dialog, whichButton ->
                        dialog.dismiss()
                        showFrontCamera()
                        getData()
                    }
                    alert.setNegativeButton("CANCEL") { dialog, whichButton ->
                        facialLivelinessResultString = "Facial Liveliness Failed"
                        progressDialog = DialogUtils.showProgessDialog(this@MainActivity, "Getting Data...")
                        getData()
                        dialog.dismiss()
                    }
                    alert.show()
                }

            } else {
                // Failure
                this@MainActivity.runOnUiThread {
                    DialogUtils.dismissDialog(progressDialog)
                    if (error.errorCode == ErrorCodes.ERROR_CouldNotClassifyDocument) {
                        showClassificationError()
                    } else {
                        DialogUtils.dismissDialog(progressDialog)
                        val alert = AlertDialog.Builder(this@MainActivity)
                        alert.setTitle("Error")
                        alert.setMessage(error.errorDescription)
                        alert.setPositiveButton("OK") { dialog, whichButton ->
                            dialog.dismiss()
                        }
                        alert.show()
                    }
                }
            }
        }
    }


    //Upload Back side of Driving License
    fun uploadBackImageOfDocument() {

        val idOptions = IdOptions()
        idOptions.cardSide = CardSide.Back
        idOptions.isHealthCard = false
        idOptions.isRetrying = false
        idOptions.processingMode = ProcessingMode.Authentication

        val idData = IdData()
        idData.image = capturedBackImage!!.image
        idData.barcodeString = capturedBarcodeString

        AcuantDocumentProcessor.uploadImage(documentInstanceID, idData, idOptions) { error, classification ->
            //                if (progressDialog != null && progressDialog!!.isShowing) {
//                    DialogUtils.dismissDialog(progressDialog)
//                }
            if (error == null) {
//                    progressDialog = DialogUtils.showProgessDialog(this@MainActivity, "Processing...")
                getData()
            }
        }
    }

    // Get data
    fun getData() {
        AcuantDocumentProcessor.getData(documentInstanceID,false, GetDataListener { result ->
            if (result == null || result!!.error != null) {
                this@MainActivity.runOnUiThread {
                    DialogUtils.dismissDialog(progressDialog)
                }
                val alert = AlertDialog.Builder(this@MainActivity)
                alert.setTitle("Error")
                if (result != null) {
                    alert.setMessage(result.error.errorDescription)
                } else {
                    alert.setMessage(ErrorDescriptions.ERROR_DESC_CouldNotGetConnectData)
                }
                alert.setPositiveButton("OK") { dialog, whichButton ->
                    dialog.dismiss()
                }
                alert.show()
                return@GetDataListener
            } else if ((result as IDResult).fields == null || (result as IDResult).fields.dataFieldReferences == null) {
                this@MainActivity.runOnUiThread {
                    DialogUtils.dismissDialog(progressDialog)
                }
                val alert = AlertDialog.Builder(this@MainActivity)
                alert.setTitle("Error")
                alert.setMessage("Unknown error happened.Could not extract data")
                alert.setPositiveButton("OK") { dialog, whichButton ->
                    dialog.dismiss()
                }
                alert.show()
                return@GetDataListener

            }
            var instanceID = result.instanceID
            var docNumber = ""
            var cardType: String = "ID1"
            var frontImageUri: String? = null
            var backImageUri: String? = null
            var signImageUri: String? = null
            var faceImageUri: String? = null
            var resultString: String? = ""
            var fieldReferences = result.fields.dataFieldReferences
            for (reference in fieldReferences) {
                if (reference.key.equals("Document Class Name") && reference.type.equals("string")) {
                    if (reference.value.equals("Driver License")) {
                        cardType = "ID1"
                    } else if (reference.value.equals("Passport")) {
                        cardType = "ID3"
                    }
                } else if (reference.key.equals("Document Number") && reference.type.equals("string")) {
                    docNumber = reference.value;
                } else if (reference.key.equals("Photo") && reference.type.equals("uri")) {
                    faceImageUri = reference.value;
                } else if (reference.key.equals("Signature") && reference.type.equals("uri")) {
                    signImageUri = reference.value;
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
                if (reference.type.equals("string")) {
                    resultString = resultString + reference.key + ":" + reference.value + System.lineSeparator()
                }
            }

            resultString = "Authentication Result : " +
                    AuthenticationResult.getString(Integer.parseInt(result.result)) +
                    System.lineSeparator() +
                    System.lineSeparator() +
                    resultString;

            thread {
                val frontImage = loadAssureIDImage(frontImageUri, Credential.get())
                val backImage = loadAssureIDImage(backImageUri, Credential.get())
                val faceImage = loadAssureIDImage(faceImageUri, Credential.get())
                val signImage = loadAssureIDImage(signImageUri, Credential.get())
                capturedFaceImage = faceImage
                MainActivity@  capturingImageData = false
                while (capturingSelfieImage) {
                    Thread.sleep(100)
                }
                this@MainActivity.runOnUiThread {
                    DialogUtils.dismissDialog(progressDialog)
                    showResults(result.biographic.birthDate, result.biographic.expirationDate, docNumber, frontImage, backImage, faceImage, signImage, resultString, cardType)
                    /*Controller.deleteInstance(instanceID, DeleteType.ID, object : DeleteListener {
                            override fun instanceDeleted(success: Boolean) {
                                if (!success) {
                                    // Handle error
                                } else {
                                    Log.d("DELETE", "Instance Deleted successfully")
                                }
                            }

                        })*/
                }
            }
        })
    }

    //process Facial Match
    fun processFacialMatch() {
        //MainActivity@ capturingFacialMatch = true
        thread {
            while (MainActivity@ capturingImageData) {
                Thread.sleep(100)
            }
            this@MainActivity.runOnUiThread {
                val facialMatchData = FacialMatchData()
                facialMatchData.faceImageOne = capturedFaceImage
                facialMatchData.faceImageTwo = capturedSelfieImage

                if(facialMatchData.faceImageOne != null && facialMatchData.faceImageTwo != null){
                    if (progressDialog != null && progressDialog!!.isShowing) {
                        DialogUtils.dismissDialog(progressDialog)
                    }
                    progressDialog = DialogUtils.showProgessDialog(this, "Matching selfie ...")
                    AcuantFaceMatch.processFacialMatch(facialMatchData, object : FacialMatchListener {
                        override fun facialMatchFinished(result: FacialMatchResult?) {
                            this@MainActivity.runOnUiThread {
                                DialogUtils.dismissDialog(progressDialog)
                                if (result!!.error == null) {
                                    val resultStr = CommonUtils.stringFromFacialMatchResult(result)
                                    facialResultString = resultStr
                                } else {
                                    val alert = AlertDialog.Builder(this@MainActivity)
                                    alert.setTitle("Error")
                                    alert.setMessage(result!!.error.errorDescription)
                                    alert.setPositiveButton("OK") { dialog, whichButton ->
                                        dialog.dismiss()
                                    }
                                    alert.show()
                                }
                            }
                            capturingSelfieImage = false
                            //capturingFacialMatch = false
                        }
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
                    facialResultString = if(facialResultString == null) "Facial Match Failed" else facialResultString;
                    ProcessedData.formattedString = facialResultString + System.lineSeparator() + resultString
                    val resultIntent = Intent(
                            this@MainActivity,
                            ResultActivity::class.java
                    )
                    if (progressDialog != null && progressDialog!!.isShowing) {
                        DialogUtils.dismissDialog(progressDialog)
                    }
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
        if (progressDialog != null && progressDialog!!.isShowing) {
            DialogUtils.dismissDialog(progressDialog)
        }
        startActivity(resultIntent)
    }

    //Correct orientation
    fun correctBitmapOrientation(image: Image?): Image? {
        if (image != null && image.image != null && image.image.getHeight() > image.image.getWidth()) {
            val mWindowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            val display = mWindowManager.defaultDisplay
            var angle = 0
            when (display.rotation) {
                Surface.ROTATION_0 // This is display orientation
                -> angle = 270 // This is camera orientation
                Surface.ROTATION_90 -> angle = 180
                Surface.ROTATION_180 -> angle = 90
                Surface.ROTATION_270 -> angle = 0
                else -> angle = 180
            }

            val matrix = Matrix()
            matrix.postRotate(angle.toFloat())
            image.image = Bitmap.createBitmap(image.image, 0, 0, image.image.getWidth(), image.image.getHeight(), matrix, true)
            return image
        }
        return image
    }

    fun dpToPx(dp: Int): Int {
        val displayMetrics = applicationContext.resources.displayMetrics
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT))
    }

    fun loadAssureIDImage(url: String?, credential: Credential?): Bitmap? {
        if (url != null && credential != null) {
            val c = URL(url).openConnection() as HttpURLConnection
            val userpass = credential.username + ":" + credential.password
            val basicAuth = "Basic " + String(Base64.encode(userpass.toByteArray(), Base64.DEFAULT))
            c.setRequestProperty("Authorization", basicAuth)
            c.useCaches = false
            c.connect()
            val bmImg = BitmapFactory.decodeStream(c.inputStream)
            return bmImg
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
