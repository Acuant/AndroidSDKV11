# Acuant Android SDK v11.6.2
**August 2025**

See [https://github.com/Acuant/AndroidSDKV11/releases](https://github.com/Acuant/AndroidSDKV11/releases) for release notes.

----------

## License
This software is subject to Acuant's end user license agreement (EULA), which can be found [here](EULA.pdf).

----------

## Introduction ##

This document provides detailed information about the Acuant Android SDK. The Acuant recommended workflow is described below.

![](https://i.imgur.com/KR0J94S.png)

**Note:** The acceptable quality image is well-cropped, sharp and with no glare present, has a resolution of at least 300 dpi (for data capture) or 600 dpi (for authentication). The aspect ratio should be acceptable and match an ID document.

----------

## Updates

**v11.6.2:** Please review [Migration Details](docs/MigrationDetails.md) for migration details (last updated for v11.6.2).

----------

## AndroidX Support

As of 11.5.0 the SDK is compiled with AndroidX and CameraX.

Before 11.5.0, the SDK was not compiled with AndroidX. The SDK could still be used with AndroidX by using [Jetifier](https://developer.android.com/jetpack/androidx/migrate).

----------

## Prerequisites ##

- Supports Android SDK versions 21-35 (compiled with 35)


## Modules ##

The SDK includes the following modules:

**Acuant Common Library (AcuantCommon):**

- Contains shared internal models and supporting classes.

**Acuant Camera Library (AcuantCamera):**

- Implemented using CameraX API and uses Google ML Kit for barcode reading. ML Kit model is packaged in the SDK (no outbound call to download model from Google Play services).
- Encompasses three different versions of the camera for reading document and barcodes, reading MRZ zones, and a backup camera for reading only barcodes.
- Uses AcuantImagePreparation for document detection and cropping.

**Acuant Image Preparation Library (AcuantImagePreparation):**

- Contains all image processing including document detection, cropping, and metrics calculation.

**Acuant Document Processing Library (AcuantDocumentProcessing):**

- Contains all the methods to upload and process document images.

**Acuant Face Match Library (AcuantFaceMatch):**    

- Contains a method to match two face images.

**Acuant EChip Reader Library (AcuantEChipReader):**

- Contains methods for e-Passport chip reading and authentication using Ozone.

**Acuant IP Liveness Library (AcuantIPLiveness):**

- Uses library for capturing a facial image and calculating liveness.
- Enhanced Face Liveness.

**Acuant Face Capture Library (AcuantFaceCapture):**

- Contains two CameraX implementations for capturing a user's face image.
- Intended to be used with with Acuant Passive Liveness to determine a user's liveness or as a standalone, primitive liveness check.

**Acuant Passive Liveness Library (AcuantPassiveLiveness):**

- Processes a single photo using our web service to determine liveness.

----------

## Setup ##

1. Specify the permissions in the App manifest file:
	
		<uses-permission android:name="android.permission.INTERNET" />
		<uses-permission android:name="android.permission.CAMERA" />
		<uses-permission android:name="android.permission.NFC" />
	
		<uses-feature android:name="android.hardware.camera" />
		<uses-feature android:name="android.hardware.camera.autofocus" />

1. Add the Acuant SDK dependency in **build.gradle**:

	- Add the following under `android` (if not already present)
	
			compileOptions {
				sourceCompatibility JavaVersion.VERSION_1_8
				targetCompatibility JavaVersion.VERSION_1_8
			}
			kotlinOptions {
				jvmTarget = "1.8"
			}

	- Add the following Maven URLs

			maven { url 'https://maven.google.com' }
			maven { url 'https://jitpack.io' }
			maven { url 'https://raw.githubusercontent.com/Acuant/AndroidSdkMaven/main/maven/' }
			maven { url 'https://raw.githubusercontent.com/iProov/android/master/maven/' }
        	
   - Add the following dependencies

			implementation 'com.acuant:acuantcommon:11.6.2'
			implementation 'com.acuant:acuantcamera:11.6.2'
			implementation 'com.acuant:acuantimagepreparation:11.6.2'
			implementation 'com.acuant:acuantdocumentprocessing:11.6.2'
			implementation 'com.acuant:acuantechipreader:11.6.2'
			implementation 'com.acuant:acuantipliveness:11.6.2'
			implementation 'com.acuant:acuantfacematch:11.6.2'
			implementation 'com.acuant:acuantfacecapture:11.6.2'
			implementation 'com.acuant:acuantpassiveliveness:11.6.2'

1. 	Create an .xml file with the following tags. (If you plan to use bearer tokens to initialize, include only the endpoints.):

		<?xml version="1.0" encoding="UTF-8" ?>
		<setting>
		    <acuant_username></acuant_username>
		    <acuant_password></acuant_password>
		    <acuant_subscription></acuant_subscription>
		    <frm_endpoint></frm_endpoint>
		    <med_endpoint></med_endpoint>
		    <assureid_endpoint></assureid_endpoint>
		    <passive_liveness_endpoint><passive_liveness_endpoint>
		    <acas_endpoint></acas_endpoint>
		    <ozone_endpoint></ozone_endpoint>
		</setting>
		
	The following are the default values for testing purposes:
	
		PREVIEW	
		<frm_endpoint>https://preview.face.acuant.net</frm_endpoint>
		<med_endpoint>https://preview.medicscan.acuant.net</med_endpoint>
		<assureid_endpoint>https://preview.assureid.acuant.net</assureid_endpoint>
		<passive_liveness_endpoint>https://preview.passlive.acuant.net</passive_liveness_endpoint>
		<acas_endpoint>https://preview.acas.acuant.net</acas_endpoint>
		<ozone_endpoint>https://preview.ozone.acuant.net</ozone_endpoint>
		
	The following are the default values based on region:

		USA
		<frm_endpoint>https://frm.acuant.net</frm_endpoint>
		<med_endpoint>https://medicscan.acuant.net</med_endpoint>
		<assureid_endpoint>https://us.assureid.acuant.net</assureid_endpoint>
		<passive_liveness_endpoint>https://us.passlive.acuant.net</passive_liveness_endpoint>
		<acas_endpoint>https://us.acas.acuant.net</acas_endpoint>
		<ozone_endpoint>https://ozone.acuant.net</ozone_endpoint>

		EU
		<frm_endpoint>https://eu.frm.acuant.net</frm_endpoint>
		<assureid_endpoint>https://eu.assureid.acuant.net</assureid_endpoint>
		<passive_liveness_endpoint>https://eu.passlive.acuant.net</passive_liveness_endpoint>
		<acas_endpoint>https://eu.acas.acuant.net</acas_endpoint>
		<ozone_endpoint>https://eu.ozone.acuant.net</ozone_endpoint>

		AUS
		<frm_endpoint>https://aus.frm.acuant.net</frm_endpoint>
		<assureid_endpoint>https://aus.assureid.acuant.net</assureid_endpoint>
		<passive_liveness_endpoint>https://aus.passlive.acuant.net</passive_liveness_endpoint>
		<acas_endpoint>https://aus.acas.acuant.net</acas_endpoint>
		<ozone_endpoint>https://aus.ozone.acuant.net</ozone_endpoint>

1.	Save the file to the application assets directory:

		{PROJECT_ROOT_DIRECTORY} => app => src => main => assets => PATH/TO/CONFIG/FILENAME.XML

----------
		
## Initializing the SDK ##

Before you use the SDK, you must initialize it, either by using the credentials saved on the device or by using bearer tokens (provided by an external server).

* Using bearer tokens:	
		
		try {
			AcuantInitializer.initializeWithToken("PATH/TO/CONFIG/FILENAME.XML",
					token,
					context, 
					listOf(MrzCameraInitializer()), 
					listener)
		} catch(e: AcuantException) {
			Log.e("Acuant Error", e.toString())
		}

* Using credentials saved on device in a config file:
				
		try {
			AcuantInitializer.initialize("PATH/TO/CONFIG/FILENAME.XML",
					context, 
					listOf(MrzCameraInitializer()), 
					listener)
		} catch(e: AcuantException) {
			Log.e("Acuant Error", e.toString())
		}

* Using credentials hardcoded in the code (not recommended):

		try {
			Credential.init(
				username: String,
				password: String,
				subscription: String?,
				acasEndpoint: String,
				assureIdEndpoint: String? = null,
				frmEndpoint: String? = null,
				passiveLivenessEndpoint: String? = null,
				ipLivenessEndpoint: String? = null,
				ozoneEndpoint: String? = null,
				healthInsuranceEndpoint: String? = null
			)
			
			AcuantInitializer.initialize(null, 
					context, 
					listOf(MrzCameraInitializer()), 
					listener)
		} catch(e: AcuantException) {
			Log.e("Acuant Error", e.toString())
		}

* Here is the interface for the initialize listener:

		interface IAcuantPackageCallback{
			fun onInitializeSuccess()

			fun onInitializeFailed(error: List<Error>)
		}
		

### Initialization without a Subscription ID ###

The SDK can be initialized by providing only a username and a password. However, without a Subscription ID, some features of the SDK are unavailable. In general, the SDK can capture images, but cannot make most outbound calls, such as uploading documents.

----------

## AcuantCamera ##

### Capturing a document ###
**Note:** **AcuantCamera** is dependent on **AcuantImagePreparation** and  **AcuantCommon**.

1. Start camera activity:

		val cameraIntent = Intent(
			this@MainActivity,
			AcuantCameraActivity::class.java
		)
		cameraIntent.putExtra(ACUANT_EXTRA_CAMERA_OPTIONS,
			AcuantCameraOptions
				.DocumentCameraOptionsBuilder()
				/*Call any other methods detailed in the AcuantCameraOptions section near the bottom of the readme*/
				.build()
		)

		//start activity for result

**Note:** When the camera is launched, the image processing speed is automatically checked.

 * Live document detection and auto capture features are enabled if the device supports a speed of at least 200ms.
 * For devices that don't meet the processing threshold, tap to capture will be enabled. Live document detection and auto capture features are disabled and switched to tap to capture. The user will have to manually capture the document. 
 
1. Get activity result:
	
		if (result.resultCode == RESULT_OK) {
			val data: Intent? = result.data
			val bytes = AcuantCameraActivity.getLatestCapturedBytes(clearBytesAfterRead = true)
			val barcodeString = data?.getStringExtra(ACUANT_EXTRA_PDF417_BARCODE)
			//...
		} else if (result.resultCode == RESULT_CANCELED) {
			//...
		} else {
			val data: Intent? = result.data
			val error = data?.getSerializableExtra(ACUANT_EXTRA_ERROR)
			if (error is AcuantError) {
				//...
			}
		}

1. (Optional) Add localized strings in app's string resources as indicated [here](#language-localization)

### Capturing a document barcode ###

**Note:** During regular capture of a document the camera will try to read the barcode. You should only launch this camera mode if the barcode is expected according to document classification and failed to read during normal capture of the relevant side.

1. Start camera activity:

		val cameraIntent = Intent(
			this@MainActivity,
			AcuantCameraActivity::class.java
		)

		cameraIntent.putExtra(ACUANT_EXTRA_CAMERA_OPTIONS,
			AcuantCameraOptions
				.BarcodeCameraOptionsBuilder()
				/*Call any other methods detailed in the AcuantCameraOptions section near the bottom of the readme*/
				.build()
		)

		//start activity for result

1. Get activity result:
	
		if (result.resultCode == RESULT_OK) {
			val data: Intent? = result.data
			val capturedBarcodeString = data?.getStringExtra(ACUANT_EXTRA_PDF417_BARCODE)
			//...
		} else if (result.resultCode == RESULT_CANCELED) {
			//...
		} else {
			val data: Intent? = result.data
			val error = data?.getSerializableExtra(ACUANT_EXTRA_ERROR)
			if (error is AcuantError) {
				//...
			}
		}	
1. (Optional) Add localized strings in app's string resources as indicated [here](#language-localization)

### Capturing MRZ data in a passport document ###

**Note:** To use the MRZ features, your credentials must be enabled to use Ozone.

- **Initialization**

	**MrzCameraInitializer()** must be included in initialization (see **Initializing the SDK**).

- **Capturing the MRZ data**

	Capturing the MRZ data using AcuantCamera is similar to document capture.

	1. Start camera activity:

			val cameraIntent = Intent(
				this@MainActivity,
				AcuantCameraActivity::class.java
			)
			
			cameraIntent.putExtra(ACUANT_EXTRA_CAMERA_OPTIONS,
				AcuantCameraOptions
					.MrzCameraOptionsBuilder()
					/*Call any other methods detailed in the AcuantCameraOptions section near the bottom of the readme*/
					.build()
			)
			
			//start activity for result

  1. Get activity result:

			if (result.resultCode == RESULT_OK) {
				val data: Intent? = result.data
				val result = data?.getSerializableExtra(ACUANT_EXTRA_MRZ_RESULT) as MrzResult?
				//...
			} else if (result.resultCode == RESULT_CANCELED) {
				//...
			} else {
				val data: Intent? = result.data
				val error = data?.getSerializableExtra(ACUANT_EXTRA_ERROR)
				if (error is AcuantError) {
					//...
				}
			}

	1. (Optional) Get the captured frame using the following method:
			
			val bytes = AcuantCameraActivity.getLatestCapturedBytes(clearBytesAfterRead = true)
			
		Note: No document detection or quality checks were performed on this uncropped frame.
		
	1. (Optional) Add localized strings to the app's string resources as indicated [here](#language-localization)

----------
		 
## AcuantImagePreparation ##

**Note:** **AcuantImagePreparation** uses @Keep annotations. These are supported by the default Android configuration. If you override or modify the Android ProGuard file, then support for these annotations must be included.

This section describes how to use **AcuantImagePreparation**.

- **Initialization**

	Must have included ImageProcessorInitializer() in initialization (See **Initializing the SDK**).

- **Cropping, Sharpness, and Glare** 

	After an image is captured, it is cropped and checked for sharpness and glare. This is done using the evaluateImage of **AcuantImagePreparation**.
	
		evaluateImage(context: Context, croppingData: CroppingData, listener: EvaluateImageListener)
	
	passing in the cropping data:
	
		class CroppingData(imageBytes: ByteArray)
	
	and a callback listener:
	
		interface EvaluateImageListener: AcuantListener {
			fun onSuccess(image: AcuantImage)
		}

	* **Important note:** Most listeners/callbacks in the SDK are extended off of AcuantListener, which contains the onError function shown below.
	
		interface AcuantListener {
			fun onError(error: AcuantError)
		}

	The **AcuantImage** can be used to verify the crop, sharpness, and glare of the image, and then upload the document in the next step (see [AcuantDocumentProcessing](#acuantdocumentprocessing)).

	
		class AcuantImage {
			val image: Bitmap
			val dpi: Int
			val sharpness: Int
			val glare: Int
			val isCorrectAspectRatio: Boolean
			val isPassport: Boolean
			val aspectRatio: Float
			val rawBytes: ByteArray
		}
	
	If the sharpness value is greater than 50, then the image is considered sharp (not blurry). If the glare value is 100, then the image does not contain glare. If the glare value is 0, then image contains glare.
	
	Preferably, the image must be sharp and not contain glare to get best results in authentication and data extraction. When the image has glare, low sharpness, or both, retake the image. 

**Note:** If you are using an independent orchestration layer, make sure you supply AcuantImage.rawBytes, and not AcuantImage.image. AcuantImage.image is provided only for visual use within the application (for example, for presenting the crop result to the user for visual verification). Do not modify AcuantImage.rawBytes in any way before upload.
		
-------------------------------------

## AcuantDocumentProcessing ##

After you capture a document image and completed crop, it can be processed using the following steps.

**Note:**  If an upload fails with an error, retry the image upload using a better image.

1. Create an instance:
		
		fun createInstance(options: IdInstanceOptions, listener: CreateIdInstanceListener)
		
			class IdInstanceOptions (
				val authenticationSensitivity: AuthenticationSensitivity,
				val tamperSensitivity: AuthenticationSensitivity,
				val countryCode: String?
			)
			
			interface CreateIdInstanceListener : AcuantListener {
			    fun instanceCreated(instance: AcuantIdDocumentInstance)
			}

1. All further methods will be called on the instanced returned thorough the instanceCreated callback. You can run multiple instances simultaneously. Each instances tracks its own state independently. These are the available methods and relevant objects/interfaces:

		fun uploadFrontImage(imageData: EvaluatedImageData, listener: UploadImageListener)
		
		fun uploadBackImage(imageData: EvaluatedImageData, listener: UploadImageListener)
		
			class EvaluatedImageData(imageBytes: ByteArray)
			
			interface UploadImageListener : AcuantListener {
				fun imageUploaded()
			}
		
		
		fun uploadBarcode(barcodeData: BarcodeData, listener: UploadBarcodeListener)
		
			interface UploadBarcodeListener : AcuantListener {
				fun barcodeUploaded()
			}
		
		fun getClassification(listener: ClassificationListener)
		
			interface ClassificationListener: AcuantListener {
				fun documentClassified(classified: Boolean, classification: Classification)
			}
		
		fun getData(listener: GetIdDataListener)
		
			interface GetIdDataListener : AcuantListener {
				fun processingResultReceived(result: IDResult)
			}
		
		fun deleteInstance(listener: DeleteListener)
		
			interface DeleteListener : AcuantListener {
				fun instanceDeleted()
			}

For most workflows, the steps resemble the following, with reuploads on error or failed classification:

		createInstance
		uploadFrontImage
		getClassification
		uploadBackImage && uploadBarcode
		getData
		deleteInstance

-------------------------------------

## AcuantIPLiveness ##

1. Get the setup from the controller and begin Activity:

		AcuantIPLiveness.getFacialSetup(object : FacialSetupListener {
			override fun onDataReceived(result: FacialSetupResult) {
				AcuantIPLiveness.runFacialCapture(this@MainActivity, result, object : IPLivenessListener {
					override fun onConnecting() {
						/...
					}
					
					override fun onConnected() {
						/...
					}
					
					override fun onProgress(status: String, progress: Int) {
						/...
					}
					
					override fun onSuccess(userId: String, token: String, frame: Bitmap?) {
						/...
					}
					
					override fun onFail(error: AcuantError) {
						/...
					}
					
					override fun onCancel() {
						/...
					}
					
					override fun onError(error: AcuantError) {
						/...
					}
				})
			}
			
			override fun onError(error: AcuantError) {
				/...
			}
		})

1. Get the facial capture result (call after onSuccess in IPLivenessListener):
		
		AcuantIPLiveness.getFacialLiveness(
			token,
			userId,
			object: FacialCaptureListener {
				override fun onDataReceived(result: FacialCaptureResult {
					//...
				}
		
				override fun onError(error: AcuantError) {
					//...
				}
			}
		)

1. (Optional) Add localized strings in app's string resources as indicated [here](#language-localization)

----------

## AcuantFaceCapture ##

This module is used to automate capturing an image of a face appropriate for use with passive liveness.

1. Start the face capture activity:

		val cameraIntent = Intent(
			this@MainActivity,
			AcuantFaceCameraActivity::class.java
		)
		
		cameraIntent.putExtra(ACUANT_EXTRA_FACE_CAPTURE_OPTIONS, FaceCaptureOptions())

		//start activity for result

2. Receive the result from the face capture activity:

		when (result.resultCode) {
		RESULT_OK -> {
			val data = result.data
			val url = data?.getStringExtra(ACUANT_EXTRA_FACE_IMAGE_URL)
			//...
		}
		RESULT_CANCELED -> {
			//...
		}
		else -> {
			//error...
		}

3. (Optional) Add localized strings in app's string resources as indicated [here](#language-localization)

**Note:** HGLiveness/Blink Test Liveness can be accessed by modifying the options as follows:

		FaceCaptureOptions(cameraMode = CameraMode.HgLiveness)

----------

## AcuantPassiveLiveness ##

This module is used to determine liveness from a single selfie image.

1. Call and handle response:

		AcuantPassiveLiveness.processFaceLiveness(passiveLivenessData: PassiveLivenessData, listener: PassiveLivenessListener)
		
		class PassiveLivenessData(faceImage: Bitmap)
		
		interface PassiveLivenessListener: AcuantListener {
			fun passiveLivenessFinished(result: PassiveLivenessResult)
		}
		
		class PassiveLivenessResult {
			var livenessResult: LivenessResult?
			var transactionId: String?
			var errorDesc: String?
			var unparsedErrorCode: String?
			val errorCode: PassiveLivenessErrorCode?
		}
			
		class LivenessResult {
			var unparsedLivenessAssessment: String?
			val livenessAssessment: LivenessAssessment?
			var score: Int
		}

		enum class LivenessAssessment {
			Error,
			PoorQuality,
			Live,
			NotLive
		}

		enum class PassiveLivenessErrorCode {
			Unknown,
			FaceTooClose,
			FaceNotFound,
			FaceTooSmall,
			FaceAngleTooLarge,
			FailedToReadImage,
			InvalidRequest,
			InvalidRequestSettings,
			Unauthorized,
			NotFound
		}

-------------------------------------
			
## AcuantFaceMatch ##

This module is used to match two facial images:

		fun processFacialMatch(facialData: FacialMatchData, listener: FacialMatchListener)

		interface FacialMatchListener: AcuantListener {
			fun facialMatchFinished(result: FacialMatchResult)
		}

-------------------------------------

## AcuantEChipReader ##

**Initialization**

Must include EchipInitializer() in initialization (See **Initializing the SDK**).
	
1. If you are using ProGuard, then you must add the the following to the configuration file (otherwise the eChip read will fail at runtime):

		-keep class org.bouncycastle.jcajce.provider.** {
			<fields>;
			<methods>;
		}
		-keep class net.sf.scuba.** {
			<fields>;
			<methods>;
		}

2. Check that the permission is included in the manifest:

		<uses-permission android:name="android.permission.NFC" />
	
3. Make sure that the NFC sensor on the device is turned on.

4. Initialize the Android NFC Adapter:
		
		NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this)

5. Use the SDK to listen to NFC tags available in an ePassport:

		AcuantEchipReader.listenNfc(this, nfcAdapter)
		
	If an NFC tag is discovered, then the control will return to the method of the Activity that was previously overridden:

		override fun onNewIntent(intent: Intent) {
			super.onNewIntent(intent)
		   
			AcuantEchipReader.readNfcTag(this, intent, docNumber, dateOfBirth, dateOfExpiry, listener)
		}
		

	This is the interface for the listener:
	
		interface NfcTagReadingListener: AcuantListener {
			fun tagReadSucceeded(nfcData: NfcData)
			
			fun tagReadStatus(status: String)
		}

**Important Note:** Most, but not all, of the data in NfcData is directly read from the passport chip. *age* and *isExpired* are extrapolated from the data read from the chip and the current date (obtained through Calendar.getInstance().time). Potentially, this can lead to inaccuracy due to either the device time being wrong or the DOB or DOE being calculated incorrectly from the data on the chip. This is an unfortunate restraint of passport chips that occurs because the DOE and DOB are stored in YYMMDD format, which is susceptible to the Y2K issue. Given a year of 22, we cannot determine with 100 percent certainty whether the 22 represents 1922 or 2022 or, theoretically, 2122. The workaround is as follows: For age, the current year is the breakpoint (for example, in 2020, 25 would be interpreted as 1925. However, in 2030, 25 would be interpreted as 2025. For isExpired, we use the same logic but going forward 20 years from the current year. Additionally, *translatedDocumentType* is an extrapolated form of the 2 character document type/subtype within the eChip. Some countries do not include a second character and some use unstandard subtype correlations, meaning that this field can also be inaccurate.

-------------------------------------

## Language localization

In order to display texts in the corresponding language you need to add the following strings to your app's strings resources:

#### AcuantCamera

	<string name="description_info">Info</string>
	<string name="request_permission">This sample needs camera permission.</string>
	<string name="acuant_camera_error">This device doesn\'t support Camera2 API.</string>

	<string name="acuant_camera_align">ALIGN</string>
	<string name="acuant_camera_align_and_tap">ALIGN AND TAP</string>
	<string name="acuant_camera_move_closer">MOVE CLOSER</string>
	<string name="acuant_camera_too_close">TOO CLOSE</string>
	<string name="acuant_camera_out_of_bounds">NOT IN FRAME</string>
	<string name="acuant_camera_hold_steady">HOLD STEADY</string>
	<string name="acuant_camera_capturing">CAPTURING</string>

	<string name="acuant_reading_mrz">Reading MRZ</string>
	<string name="acuant_align_mrz">Align</string>
	<string name="acuant_closer_mrz">Move Closer</string>
	<string name="acuant_glare_mrz">Try Repositioning</string>
	<string name="acuant_read_mrz">Read Successful</string>

	<string name="acuant_camera_capturing_barcode">Capturing...</string>
	<string name="acuant_camera_align_barcode">Capture Barcode</string>

#### AcuantFaceCapture

	<string name="acuant_face_camera_initial">Align face to start capture</string>
	<string name="acuant_face_camera_face_too_close">Too close! Move away</string>
	<string name="acuant_face_too_far">Move closer</string>
	<string name="acuant_face_camera_face_has_angle">Face has angle. Do not tilt</string>
	<string name="acuant_face_camera_face_not_in_frame">Move in frame</string>
	<string name="acuant_face_camera_face_moved">Hold steady</string>
	<string name="acuant_face_camera_blink">Please blink</string>
	<string name="acuant_face_camera_capturing">Capturing…</string>
	<plurals name="acuant_face_camera_countdown">
			<item quantity="one">Capturing\n%d…</item>
			<item quantity="other">Capturing\n%d…</item>
	</plurals>

#### AcuantIPLiveness

	<string name="iproov__language_file">en</string>
	<string name="iproov__prompt_genuine_presence_align_face">Put your face in the oval</string>
	<string name="iproov__prompt_liveness_align_face">Fill the oval with your face</string>
	<string name="iproov__prompt_liveness_no_target">Put your face in the frame</string>
	<string name="iproov__prompt_connecting">Connecting…</string>
	<string name="iproov__prompt_tap_to_begin">Tap the screen to begin</string>
	<string name="iproov__prompt_too_far">Move closer</string>
	<string name="iproov__prompt_too_bright">Go somewhere shadier</string>
	<string name="iproov__progress_streaming">Streaming…</string>
	<string name="iproov__progress_streaming_slow">Streaming, network is slow…</string>
	<string name="iproov__prompt_scanning">Scanning…</string>
	<string name="iproov__progress_identifying_face">Identifying face…</string>
	<string name="iproov__progress_confirming_identity">Confirming identity…</string>
	<string name="iproov__progress_assessing_genuine_presence">Assessing genuine presence…</string>
	<string name="iproov__progress_assessing_liveness">Assessing liveness…</string>
	<string name="iproov__progress_loading">Loading…</string>
	<string name="iproov__progress_creating_identity">Creating identity…</string>
	<string name="iproov__progress_finding_face">Finding face…</string>
	<string name="iproov__authenticate">Authenticate</string>
	<string name="iproov__enrol">Enrol</string>
	<string name="iproov__message_format">%1$s to %2$s</string>
	<string name="iproov__prompt_too_close">Too close</string>
	<string name="iproov__failure_ambiguous_outcome">Ambiguous outcome</string>
	<string name="iproov__failure_motion_too_much_movement">Please do not move while iProoving</string>
	<string name="iproov__failure_lighting_flash_reflection_too_low">Ambient light too strong or screen brightness too low</string>
	<string name="iproov__failure_lighting_backlit">Strong light source detected behind you</string>
	<string name="iproov__failure_lighting_too_dark">Your environment appears too dark</string>
	<string name="iproov__failure_lighting_face_too_bright">Too much light detected on your face</string>
	<string name="iproov__failure_motion_too_much_mouth_movement">Please do not talk while iProoving</string>
	<string name="iproov__failure_user_timeout">Your session has expired</string>
	<string name="iproov__message_format_with_username">%1$s as %2$s to %3$s</string>
	<string name="iproov__prompt_roll_too_high">Avoid tilting your head</string>
	<string name="iproov__prompt_roll_too_low">Avoid tilting your head</string>
	<string name="iproov__prompt_yaw_too_low">Turn slightly to your right</string>
	<string name="iproov__prompt_yaw_too_high">Turn slightly to your left</string>
	<string name="iproov__prompt_pitch_too_high">Hold the device at eye level</string>
	<string name="iproov__prompt_pitch_too_low">Hold the device at eye level</string>
	<string name="iproov__prompt_liveness_scan_completed">Scan completed</string>
	<string name="iproov__prompt_get_ready">Get ready…</string>
	<string name="iproov__debug_text_default">Loading…</string>
	<string name="iproov__error_network">Network error</string>
	<string name="iproov__error_device_not_supported">Device is not supported</string>
	<string name="iproov__error_camera">Camera error</string>
	<string name="iproov__error_camera_permission_denied">Camera permission denied</string>
	<string name="iproov__error_camera_permission_denied_message">Please allow camera access for this app in Android Settings</string>
	<string name="iproov__error_server">Server error</string>
	<string name="iproov__error_multi_window_mode_unsupported">Application is in multi-window mode</string>
	<string name="iproov__error_face_detector">Face detector error</string>
	<string name="iproov__error_capture_already_active">An existing capture is already in progress</string>
	<string name="iproov__error_listener_not_registered">Before calling IProov.launch(), you should register a listener with IProov.registerListener()</string>
	<string name="iproov__error_invalid_options">Invalid iProov options</string>
	<string name="iproov__error_unexpected_error">Unexpected error</string>

----------

### Error codes ###

		object ErrorCodes {
			const val ERROR_InvalidCredentials = -1
			const val ERROR_BlankBarcode = -2
			const val ERROR_InvalidEndpoint = -3
			const val ERROR_Network = -4
			const val ERROR_InvalidJson = -5
			const val ERROR_CouldNotCrop = -6
			const val ERROR_NotEnoughMemory = -7
			const val ERROR_LowResolutionImage = -8
			const val ERROR_Permissions = -9
			const val ERROR_SavingImage = -10
			const val ERROR_CAPTURING_FACIAL = -1001
			const val ERROR_SETUP_FACIAL = -1003
			const val ERROR_FailedToLoadOcrFiles = -2001
			const val ERROR_EChipReadError = -3001
			const val ERROR_InvalidNfcTag = -3002
			const val ERROR_InvalidNfcKeyFormatting = -3003
			const val ERROR_UnexpectedError = -9999
		}
		

### Error descriptions ###

		object ErrorDescriptions {
			const val ERROR_DESC_InvalidCredentials = "Invalid credentials"
			const val ERROR_DESC_BlankBarcode = "Blank barcode, skipped upload."
			const val ERROR_DESC_InvalidEndpoint = "Invalid/unapproved endpoint"
			const val ERROR_DESC_Network = "Network request failed"
			const val ERROR_DESC_InvalidJson = "Invalid Json response"
			const val ERROR_DESC_CouldNotCrop = "Could not crop image"
			const val ERROR_DESC_NotEnoughMemory = "Ran out of memory"
			const val ERROR_DESC_LowResolutionImage = "Low resolution image"
			const val ERROR_DESC_Permissions = "Required permission was not granted"
			const val ERROR_DESC_SavingImage = "Error while saving an image from the camera"
			const val ERROR_DESC_CAPTURING_FACIAL_IPROOV = "Failed to capture during IProov"
			const val ERROR_DESC_SETUP_FACIAL_IPROOV = "Failed to set up IProov"
			const val ERROR_DESC_FailedToLoadOcrFiles = "Failed to load ocrb.traineddata"
			const val ERROR_DESC_EChipReadError =
			"Error reading eChip. Connection lost to passport or incorrect key."
			const val ERROR_DESC_InvalidNfcTag =
			"Tag Tech list was null. Most likely means unsupported passport/not a passport"
			const val ERROR_DESC_InvalidNfcKeyFormatting =
			"Decryption key formatted incorrectly. Check DOB, DOE, and doc number."
			const val ERROR_DESC_UnexpectedError =
			"Unexpected error occurred, usually indicates a try catch caught an error that was not expected to be hit."
		}

### Image ###

		public class Image {
			public Bitmap image;
			public int dpi;
			public boolean isCorrectAspectRatio;
			public boolean isPassport;
			public float aspectRatio;
			public Error error;
			public Point[] points;
		}

### AcuantCameraOptions ###

		class DocumentCameraOptionsBuilder {
			fun setTimeInMsPerDigit(value: Int) : DocumentCameraOptionsBuilder
			fun setDigitsToShow(value: Int) : DocumentCameraOptionsBuilder
			fun setAllowBox(value: Boolean) : DocumentCameraOptionsBuilder 
			fun setAutoCapture(value: Boolean) : DocumentCameraOptionsBuilder 
			fun setBracketLengthInHorizontal(value: Int) : DocumentCameraOptionsBuilder 
			fun setBracketLengthInVertical(value: Int) : DocumentCameraOptionsBuilder 
			fun setDefaultBracketMarginWidth(value: Int) : DocumentCameraOptionsBuilder 
			fun setDefaultBracketMarginHeight(value: Int) : DocumentCameraOptionsBuilder
			fun setColorHold(value: Int) : DocumentCameraOptionsBuilder 
			fun setColorCapturing(value: Int) : DocumentCameraOptionsBuilder
			fun setColorBracketAlign(value: Int) : DocumentCameraOptionsBuilder
			fun setColorBracketHold(value: Int) : DocumentCameraOptionsBuilder
			fun setColorBracketCloser(value: Int) : DocumentCameraOptionsBuilder
			fun setColorBracketCapturing(value: Int) : DocumentCameraOptionsBuilder
			fun setPreventScreenshots(value: Boolean) : DocumentCameraOptionsBuilder
			/**
			* [ZoomType.Generic] keeps the camera zoomed out to enable you to use nearly all available
			* capture space. This is the default setting. Use this setting to capture large
			* documents (ID3) and to use old devices with low-resolution cameras.
			*
			* [ZoomType.IdOnly] zooms the camera by approximately 25%, pushing part of the capture 
			* space off the sides of the screen. Generally, IDs are smaller than passports and, on most
			* devices, the capture space is sufficient for a 600 dpi capture of an ID. The 
			* [ZoomType.IdOnly] experience is more intuitive for users because [ZoomType.Generic] makes
			* the the ID appear too far away for capture. Using [ZoomType.IdOnly] to capture large 
			* documents (ID3) usually results in a lower resolution capture that can cause 
			* classification/authentication errors.
			*/
			fun setZoomType(value: ZoomType) : DocumentCameraOptionsBuilder 
			fun build() : AcuantCameraOptions
		}

		class MrzCameraOptionsBuilder {
			fun setAllowBox(value: Boolean) : MrzCameraOptionsBuilder 
			fun setBracketLengthInHorizontal(value: Int) : MrzCameraOptionsBuilder 
			fun setBracketLengthInVertical(value: Int) : MrzCameraOptionsBuilder 
			fun setDefaultBracketMarginWidth(value: Int) : MrzCameraOptionsBuilder 
			fun setDefaultBracketMarginHeight(value: Int) : MrzCameraOptionsBuilder
			fun setColorCapturing(value: Int) : MrzCameraOptionsBuilder
			fun setColorBracketCapturing(value: Int) : MrzCameraOptionsBuilder
			fun setPreventScreenshots(value: Boolean) : MrzCameraOptionsBuilder
			fun build() : AcuantCameraOptions
		}
		
		class BarcodeCameraOptionsBuilder {
			fun setTimeToWaitAfterDetection(value: Int) : BarcodeCameraOptionsBuilder
			fun setTimeToWaitUntilTimeout(value: Int) : BarcodeCameraOptionsBuilder
			fun setColorCapturing(value: Int) : BarcodeCameraOptionsBuilder
			fun setColorAlign(value: Int) : BarcodeCameraOptionsBuilder
			fun setPreventScreenshots(value: Boolean) : BarcodeCameraOptionsBuilder
			fun build() : AcuantCameraOptions
		}

### FaceCaptureOptions ###

		class FaceCaptureOptions constructor(
			val totalCaptureTime : Int = 2,
			val colorGood : Int = Color.GREEN,
			val colorDefault : Int = Color.BLACK,
			val colorError : Int = Color.RED,
			val colorTextGood : Int = Color.GREEN,
			val colorTextDefault : Int = Color.WHITE,
			val colorTextError : Int = Color.RED,
			val showOval : Boolean = false
		)

### IdOptions ###

		public class IdOptions {
			public CardSide cardSide;
			public boolean isRetrying;
			public boolean isHealthCard;
			public AuthenticationSensitivity authenticationSensitivity;
			public TamperSensitivity tamperSensitivity;
		}
		
### NfcData (used in eChip workflow) ###

		public class NfcData {
		
			enum class OzoneResultStatus {
				SUCCESS, FAILED, UNKNOWN, NOT_PERFORMED
			}

			enum class ByteGroup {
				DG1, DG2, DG3, DG4, DG5, DG6, DG7, DG8, DG9, DG10, DG11, DG12, DG13, DG14, DG15, SOD, COM
			}

			enum class AuthStatus {
				Success, Failure, Skipped
			}
			
			enum class TranslatedDocumentType {
				Default("Default"),
				NationalPassport("National Passport"),
				EmergencyPassport("Emergency Passport"),
				DiplomaticPassport("Diplomatic Passport"),
				OfficialOrServicePassport("Official/Service Passport"),
				RefugeePassport("Refugee Passport"),
				AlienPassport("Alien Passport"),
				StatelessPassport("Stateless Passport"),
				TravelDocument("Travel Document"),
				MilitaryPassport("Military Passport");
			}
		
			var dateOfBirth: String
			var documentExpiryDate: String
			var documentCode: String
			var issuingAuthority: String
			var documentNumber: String
			var nationality: String
			var personalNumber: String
			var firstName: String
			var lastName: String
			var documentType: String 
			var documentSubType: String
			var translatedDocumentType: TranslatedDocumentType //extrapolated
			var gender: String 
			val age: Int? //extrapolated
			val isExpired: Boolean? //extrapolated
			var image: Bitmap?
			var signatureImage: Bitmap?
			var BACStatus: AuthStatus
			var PACEStatus: AuthStatus
			var activeAuthenticationStatus: AuthStatus
			var chipAuthenticationStatus: AuthStatus
			var passportDataValid: Boolean //Data Group Hash Check Status
			var passportCountrySigned: OzoneResultStatus
			var passportSigned: OzoneResultStatus
			
			fun getBytes(group: ByteGroup?): ByteArray?
		}

-------------------------------------

## Frequently Asked Questions ##

#### Why is the SDK so large ####

The SDK is large because there are several ml-kit models bundled into it. This bundling has pros and cons. Bundling models into the SDK enables it to work in areas and on devices that do not have access to Google Play services. If the size of the SDK is an issue, you can reduce the size by downloading the models from Google Play services the first time the application is launched. The download can occur in the background. To enable the download, use the open versions of the face capture and camera modules. Within the gradle files of those models, remove the following lines:

		implementation 'com.google.mlkit:face-detection:XXX'
		implementation 'com.google.mlkit:barcode-scanning:XXX'
		
and replace them with the following respectively (where XXX is the current version number on the previous lines):

		implementation 'com.google.android.gms:play-services-mlkit-face-detection:XXX'
		implementation 'com.google.android.gms:play-services-mlkit-barcode-scanning:XXX'
		
Then, add the following to your manifest:

		<application ...>
			 ...
			 <meta-data
				android:name="com.google.mlkit.vision.DEPENDENCIES"
				android:value="barcode,face" >
		</application>
		
**Important:** If the models can’t be accessed, the SDK behaves unexpectedly. Use this size-saving procedure only if you are sure that all of your clients will use phones that have access to Google Play from regions that don’t block it.

To read more about this and the ml-kit, see https://developers.google.com/ml-kit/guides.
		

#### How do I obfuscate my Android application? ####

Acuant does not provide obfuscation tools. See the Android developer documentation about obfuscation at: [https://developer.android.com/studio/build/shrink-code](https://developer.android.com/studio/build/shrink-code). Then open proguard-rules.pro for your project and set the obfuscation rules. 

-------------------------------------

**Copyright 2023 Acuant Inc. All rights reserved.**

This document contains proprietary and confidential information and creative works owned by Acuant and its respective licensors, if any. Any use, copying, publication, distribution, display, modification, or transmission of such technology, in whole or in part, in any form or by any means, without the prior express written permission of Acuant is strictly prohibited. Except where expressly provided by Acuant in writing, possession of this information shall not be construed to confer any license or rights under any Acuant intellectual property rights, whether by estoppel, implication, or otherwise.

AssureID and *i-D*entify are trademarks of Acuant Inc. Other Acuant product or service names or logos referenced this document are either trademarks or registered trademarks of Acuant.

All 3M trademarks are trademarks of Gemalto Inc./Thales

Windows is a registered trademark of Microsoft Corporation.

Certain product, service, or company designations for companies other
than Acuant may be mentioned in this document for identification
purposes only. Such designations are often claimed as trademarks or
service marks. In all instances where Acuant is aware of a claim, the
designation appears in initial capital or all capital letters. However,
you should contact the appropriate companies for more complete
information regarding such designations and their registration status.



[https://support.acuant.com](http://support.acuant.com)

**Acuant Inc.**  **6080 Center Drive, Suite 850,** **Los Angeles, CA 90045**

----------------------------------------------------
