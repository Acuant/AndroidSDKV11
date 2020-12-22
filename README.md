# Acuant Android SDK v11.4.9
**December 2020**

See [https://github.com/Acuant/AndroidSDKV11/releases](https://github.com/Acuant/AndroidSDKV11/releases) for release notes.

----------

## License
This software is subject to Acuant's end user license agreement (EULA), which can be found [here](EULA.pdf).

----------

## Introduction ##

This document provides detailed information about the Acuant Android SDK. The Acuant recommended workflow is described below.

![](https://i.imgur.com/KR0J94S.png)

**Note:** The acceptable quality image is well-cropped, sharp and with no glare present, has a resolution of at least 300 dpi (for data capture) or 600 dpi (for authentication). The aspect ratio should be acceptable and matches an ID document.

----------

## Updates

**v11.4.4:** Please review [Migration Details](docs/MigrationDetails.md) for migration details (last updated for v11.4.4).

----------

## AndroidX Support

In order to maintain backward compatibility, the Acuant SDK is currently not compiled using AndroidX support libraries. However, the SDK may be used with AndroidX by using [Jetifier](https://developer.android.com/jetpack/androidx/migrate).

**Note:** This should be enabled by default when you migrate your project to AndroidX.

- If you are using Acuant’s compiled AAR files, or if you get the SDK from Maven, no additional action is required (aside from verifying that Jetifier is enabled).

- If you are customizing any of Acuant’s open modules, you will need to build the library into an AAR file, and then use that AAR file in your app along with Jetifier.

----------

## Prerequisites ##

- Supports Android SDK versions 21-29


## Modules ##

The SDK includes the following modules:

**Acuant Common Library (AcuantCommon) :**

- Contains all shared internal models and supporting classes

**Acuant Camera Library (AcuantCamera) :**

- Implemented using Camera 2 API with Google Vision for reading PDF417 barcodes
- Encompasses two different versions of the camera, one for reading documents, the other for reading MRZ zones.
- Uses AcuantImagePreparation for cropping

**Acuant Image Preparation Library (AcuantImagePreparation) :**

- Contains all image processing including cropping and calculation of sharpness and glare

**Acuant Document Processing Library (AcuantDocumentProcessing) :**

- Contains all the methods to upload the document images, process, and get results

**Acuant Face Match Library (AcuantFaceMatch) :**    

- Contains a method to match two facial images 

**Acuant EChip Reader Library (AcuantEChipReader):**

- Contains methods for e-Passport chip reading and authentication using Ozone

**Acuant IP Liveness Library (AcuantIPLiveness):**

- Uses library for capturing a facial image and calculating liveness
- Enhanced Face Liveness

**Acuant HG Liveness Library (AcuantHGLiveness):**

- Uses Camera 1 to capture facial liveness using a proprietary algorithm

**Acuant Face Capture Library (AcuantFaceCapture):**

- Uses Camera 1 to capture a single face image for use with our passive liveness system

**Acuant Passive Liveness Library (AcuantPassiveLiveness):**

- Processes a single photo using our web service to determine liveness.

----------

## Setup ##

1. Specify the permissions in the App manifest file:
	
	    <uses-permission android:name="android.permission.INTERNET" />
	    <uses-permission android:name="android.permission.CAMERA" />
	    <uses-permission android:name="android.permission.NFC" />
	    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
	
		<uses-feature android:name="android.hardware.camera" />
    	<uses-feature android:name="android.hardware.camera.autofocus" />

	    <meta-data
    	    android:name="com.google.android.gms.vision.DEPENDENCIES"
    	    android:value="barcode,face"
    	    tools:replace="android:value"/>
    
1. Add the Acuant SDK dependency in **build.gradle**:

		repositories {
			//Face Capture and Barcode reading. Only add if using acuantcamera or acuanthgliveness
			maven { url 'https://maven.google.com' }
			maven { url 'https://raw.githubusercontent.com/iProov/android/master/maven/' }
		}

    	dependencies {
			//if possible, use v7:28.0.0 for android support version
			//implementation 'com.android.support:appcompat-v7:28'
			//implementation 'com.android.support:support-v4:28.0.0'
			//implementation 'com.android.support:appcompat-v7:28.0.0'
			//implementation 'com.android.support:exifinterface:28.0.0'

			//Face Capture and Barcode reading. Only add if using acuantcamera or acuanthgliveness
			implementation 'com.google.android.gms:play-services-vision:17.0.2'

			//External library for MRZ reading. Only add if using the MRZ part of acuantcamera
			implementation 'com.rmtheis:tess-two:9.0.0'
		    
			//external libraries for echip reading. Only add if using acuantechipreader
			implementation group: 'com.github.mhshams', name: 'jnbis', version: '1.0.4'
			implementation('org.jmrtd:jmrtd:0.7.11') {
				transitive = true;
			}
			implementation('org.ejbca.cvc:cert-cvc:1.4.6') {
				transitive = true;
			}
			implementation('org.bouncycastle:bcprov-jdk15on:1.61') {
				transitive = true;
			}
			implementation('net.sf.scuba:scuba-sc-android:0.0.18') {
				transitive = true;
			}
			//end echip reading
		    
			//internal common library
			implementation project(path: ':acuantcommon')

			//camera with autocapture - Uses camera 2 API
			implementation project(path: ':acuantcamera')

			//document parse, classification, authentication
			implementation project(path: ':acuantdocumentprocessing')

			//face match library
			implementation project(path: ':acuantfacematchsdk')

			//for reading epassport chips
			implementation project(path: ':acuantechipreader')

			//face capture and liveliness
			implementation project(path: ':acuantipliveness')
			implementation('com.iproov.sdk:iproov:5.2.1@aar') {
				transitive = true
			}

			//face capture and liveliness
			implementation project(path: ':acuanthgliveness')

			//image processing (cropping, glare, sharpness)
			implementation project(path: ':acuantimagepreparation')

			//face capture
			implementation project(path: ':acuantfacecapture')

			//passive liveness
			implementation project(path: ':acuantpassiveliveness')
  		}
  		
1. Add the Acuant SDK dependency in **build.gradle** if using Maven:

	- Add the following Maven URL

    		maven { url 'https://dl.bintray.com/acuant/Acuant' }
    		maven { url 'https://raw.githubusercontent.com/iProov/android/master/maven/' }
        	
   - Add the following dependencies

    		implementation 'com.acuant:acuantcommon:11.4.9'
    		implementation 'com.acuant:acuantcamera:11.4.9'
    		implementation 'com.acuant:acuantimagepreparation:11.4.9'
    		implementation 'com.acuant:acuantdocumentprocessing:11.4.9'
    		implementation 'com.acuant:acuantechipreader:11.4.9'
    		implementation 'com.acuant:acuantfacematch:11.4.9'
    		implementation 'com.acuant:acuanthgliveness:11.4.9'
    		implementation ('com.acuant:acuantipliveness:11.4.9'){
        		transitive = true
    		}
    		implementation 'com.acuant:acuantfacecapture:11.4.9'
    		implementation 'com.acuant:acuantpassiveliveness:11.4.9'
		
   - Acuant also relies on Google Play services dependencies, which are pre-installed on almost all Android devices.


1. 	Create an xml file with the following tags (If you plan to use bearer tokens to initialize, then username and password can be left blank):

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

Before you use the SDK, you need to initialize it either by using the credentials saved on the device or by using bearer tokens (provided by an external server).

**Note:** If you are *not* using a configuration file for initialization, then use the following statement (providing appropriate credentials for *username*, *password*, and *subscription ID*) and leave the "PATH/TO/CONFIG/FILENAME.XML" in the initialize method as ""
	
		Credential.init("xxxxxxx",
		"xxxxxxxx",
		"xxxxxxxxxx",
		"https://frm.acuant.net",
		"https://services.assureid.net",
		"https://medicscan.acuant.net",
		"https://us.passlive.acuant.net",
		"https://acas.acuant.net",
		"https://ozone.acuant.net")

1. Using credentials saved on a device:

		
		//Specify the path to the previously created XML file, using “assets” as root
		//Pass in Context from Application
		//List the packages to initialize; only ImageProcessor is required

		try{
			AcuantInitializer.initialize("PATH/TO/CONFIG/FILENAME.XML",
							context,
							listOf(ImageProcessorInitializer(), EchipInitializer(), MrzCameraInitializer() /\*Exclude the ones you don't use\*/),
							listener)
		}
		catch(e: AcuantException){
			Log.e("Acuant Error", e.toString())
		}
		
2. Using bearer tokens:	

		
		//Specify the path to the previously created XML file, using “assets” as root
		//Pass in Context from Application
		//List the packages to initialize; only ImageProcessor is required
		
		//having received the bearer token from your service
		try{
			AcuantInitializer.initializeWithToken("PATH/TO/CONFIG/FILENAME.XML",
							token,
							context,
							listOf(ImageProcessorInitializer(), EchipInitializer(), MrzCameraInitializer() /\*Exclude the ones you don't use\*/),
							listener)
		}
		catch(e: AcuantException){
			Log.e("Acuant Error", e.toString())
		}

Here is the interface for the initialize listener:

		interface IAcuantPackageCallback{
			fun onInitializeSuccess()

			fun onInitializeFailed(error: List<Error>)
		}
		
### Initialization without a Subscription ID ###

**AcuantImagePreparation** may be initialized by providing only a username and a password. However, without providing a Subscription ID, the application can *only* capture an image and get the image. Without a Subscription ID:

1. Only the **AcuantCamera**, **AcuantImagePreparation**, and **AcuantHGLiveness** modules may be used.

2. The SDK can be used to capture identity documents.

3. The captured images can be exported from the SDK. See the **onActivityResult** in the following section.

----------

## AcuantCamera ##

### Capturing a document ###
**Note:**   **AcuantCamera** is dependent on **AcuantImagePreparation** and  **AcuantCommon**.

1. Start camera activity:

		val cameraIntent = Intent(this, AcuantCameraActivity::class.java)

		cameraIntent.putExtra(ACUANT_EXTRA_IS_AUTO_CAPTURE, boolean)//default is true
		cameraIntent.putExtra(ACUANT_EXTRA_BORDER_ENABLED, boolean)//default is true

		startActivityForResult(cameraIntent, REQUEST_CODE)
	   
	Alternatively use the new options object. This method allows you to configure much more about the camera (see **AcuantCameraOptions**):
        
		val cameraIntent = Intent(this, AcuantCameraActivity::class.java)

		cameraIntent.putExtra(ACUANT_EXTRA_CAMERA_OPTIONS,
			AcuantCameraOptions
				.DocumentCameraOptionsBuilder()
				.build()
				/*Acuant has temporarily kept the constructor public for backward compatibility,
				 * but it will become private in the near future. Acuant strongly recommends that
				 * you use the provided builder for all new implementations.*/
		)

		startActivityForResult(cameraIntent, REQUEST_CODE) 
**Note:**  When the camera is launched, the image processing speed is automatically checked.

	- Live document detection and auto capture features are enabled if the device supports a speed of at least 130ms.
	- For devices that don't meet the processing threshold, tap to capture will be enabled. Live document detection and auto capture features are disabled and switched to tap to capture. The user will have to manually capture the document. 
 
1. Get activity result:
	
		override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
			super.onActivityResult(requestCode, resultCode, data)
			
			if (requestCode == REQUEST_CODE && AcuantCameraActivity.RESULT_SUCCESS_CODE) {
				val capturedImageUrl = data?.getStringExtra(ACUANT_EXTRA_IMAGE_URL)
				val capturedBarcodeString = data?.getStringExtra(ACUANT_EXTRA_PDF417_BARCODE)
			}
		}
		
### Capturing MRZ data in a passport document ###

**Note:**   To use the MRZ features, your credentials must be enabled to use Ozone.

- **Initialization**

	**MrzCameraInitializer()** must be included in initialization (see **Initializing the SDK**).

	**Important Note:** You must grant external storage permissions in order to save the OCRB information to the phone. Otherwise, the MRZ camera will not function.

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
					.build()
					/*Please note that this uses a different builder than the document camera.
					 * This is how the camera knows that it is being launched in MRZ mode.*/
			)
			
			startActivityForResult(cameraIntent, REQUEST_CODE)
        
  	1. Get activity result:

			override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
				super.onActivityResult(requestCode, resultCode, data)
				
				if (requestCode == REQUEST_CODE && resultCode == AcuantCameraActivity.RESULT_SUCCESS_CODE) {
					val result = data?.getSerializableExtra(ACUANT_EXTRA_MRZ_RESULT) as MrzResult
				}
			}
		 
## AcuantImagePreparation ##

**Note:**   **AcuantImagePreparation** uses @Keep annotations. These are supported by the default Android configuration. If you override or modify the Android ProGuard file, then support for these annotations must be included.

This section describes how to use **AcuantImagePreparation**.

- **Initialization**

	Must have included ImageProcessorInitializer() in initialization (See **Initializing the SDK**).

- **Cropping, Sharpness, and Glare** 

	After an image is captured, it is cropped and checked for sharpness and glare. This is done using the evaluateImage of **AcuantImagePreparation**.
	
		evaluateImage(context: Context, croppingData: CroppingData, listener: EvaluateImageListener)
	
	passing in the cropping data:
	
		class CroppingData(Bitmap image)
	
	and a callback listener:
	
		interface EvaluateImageListener {
			fun onSuccess(image: AcuantImage)
			fun onError(error: Error)
		}
		
	The AcuantImage can be used to verify the crop, sharpness, and glare as well as upload in the next step:
	
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
		
-------------------------------------

## AcuantDocumentProcessing ##

After you capture a document image is captured, use the following steps to process the image.

**Note:**  If an upload fails with an error, retry the image upload using a better image.

1. Create an instance:
		
		public static void createInstance(IdOptions options, CreateInstanceListener listener)
		
		public interface CreateInstanceListener {
			void instanceCreated(String instanceId, Error error);
		}
		
2. Upload an image:

		public static void uploadImage(String instanceID, EvaluatedImageData imageData, IdOptions options, UploadImageListener listener)
		
		class EvaluatedImageData (
			val imageBytes: ByteArray,
			val barcodeString: String? = null
		)
		
		public interface UploadImageListener {
			void imageUploaded(Error error, Classification classification);
		}
		
**Important Note:** The image bytes in EvaluatedImageData should be the bytes from AcuantImage.rawBytes not the bytes from the bitmap stored within. Similarly if you are not using **AcuantDocumentProcessing** and uploading the image in some other way you should also be uploading these bytes.
		
3. Get the data:
		
		public static void getData(String instanceID,boolean isHealthCard, GetDataListener listener)
		
		public interface GetDataListener {
			void processingResultReceived(ProcessingResult result);
		}
        
4. Delete the instance:


		public static void deleteInstance(String instanceId, DeleteType type, DeleteListener listener)

		public interface DeleteListener {
			public void instanceDeleted(boolean success);
		}
		
-------------------------------------

## AcuantIPLiveness ##

**Important Note:** The following must be in your root level gradle in the android{} section otherwise a runtime failure may occur:

		compileOptions {
			sourceCompatibility JavaVersion.VERSION_1_8
			targetCompatibility JavaVersion.VERSION_1_8
		}
		kotlinOptions {
			jvmTarget = "1.8"
		}

1. Get the setup from the controller and begin Activity:

		AcuantIPLiveness.getFacialSetup(object :FacialSetupLisenter{
			override fun onDataReceived(result: FacialSetupResult?) {
				if(result != null) {
					//start face capture activity
                    result.allowScreenshots = true //Set to false by default; set to true to enable allowScreenshots
                    AcuantIPLiveness.runFacialCapture(context, result, listener)
				}
				else {
					//handle error
				}
			}

			override fun onError(errorCode: Int, description: String?) {
				//handle error
			}
		})
        
2. Get the result:

		//implement the following listener
		interface IPLivenessListener {
			fun onProgress(status: String, progress: Int) // for displaying the progress of liveness analysis after capture, progress = 0 to 100, status = text description of current step
			fun onSuccess(userId: String, token: String)
			fun onFail(error: Error)
			fun onCancel() // called when no error occurred but user canceled/backed out of the process
		}
		
3. Get the facial capture result (call after onSuccess in IPLivenessListener):
		
		//isPassed = true if face is live; otherwise false
		//frame contains the base64 encoded image
		data class FacialCaptureResult (isPassed: Boolean, frame: String) 

		AcuantIPLiveness.getFacialLiveness(
			token,
			userId,
			object: FacialCaptureLisenter {
				override fun onDataReceived(result: FacialCaptureResult) {
					//use result
				}

				override fun onError(errorCode:Int, errorDescription: String) {
					//handle error
				}
			}
		)

-------------------------------------
		
### AcuantHGLiveness ###

This module checks for liveness (whether the subject is a live person) by using blink detection. 

1. Begin Activity:

		val cameraIntent = Intent(
			this@MainActivity,
			FacialLivenessActivity::class.java
		)
		startActivityForResult(cameraIntent, YOUR_REQUEST_CODE)
        
2. Get the Activity result:

		
		override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		    super.onActivityResult(requestCode, resultCode, data)
		

			if (requestCode == YOUR_REQUEST_CODE) {
				if(resultCode == FacialLivenessActivity.RESPONSE_SUCCESS_CODE){
					val faceImage = FaceCapturedImage.bitmapImage
				}
				else{
					//handle error
				}
			}
		}

	
## AcuantFaceMatch ##

This module is used to match two facial images:

		fun processFacialMatch(facialData: FacialMatchData, listener: FacialMatchListener?)

		public interface FacialMatchListener {
			public void facialMatchFinished(FacialMatchResult result);
		}

-------------------------------------

## AcuantEChipReader ##

**Initialization**

Must include EchipInitializer() in initialization (See **Initializing the SDK**).
	
1. If you are using ProGuard, then you must add the the following to the configuration file (otherwise the echip read will fail at runtime):

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
		   
			AcuantEchipReader.readNfcTag(this, intent, Credential.get(), docNumber, dateOfBirth, dateOfExpiry, listener)
		}
		
	This is the interface for the listener:
	
		interface NfcTagReadingListener {
			fun tagReadSucceeded(nfcData: NfcData)

			fun tagReadFailed(error: Error)

			fun tagReadStatus(status: String)
		}
		
**Important Note:** All the data in nfcData is directly read from the passport chip except for *age* and *isExpired*. These two fields are extrapolated from the data read from the chip and the current date (obtained via Calendar.getInstance().time). This can potentially lead to inaccuracy due to either the device time being wrong or the DOB or DOE being calculated incorrectly from the data on the chip. This is an unfortunate restraint of passport chips as the DOE and DOB are stored in YYMMDD format and therefore suffers from the y2k issue (given a year of 22 we can not with 100% certainty determine if it stands for 1922 or 2022 or even theoretically 2122). The way we work around this is as follows: For age we use the current year as the breakpoint (eg. in 2020, 25 would be interpreted as 1925 but in 2030 25 would be interpreted as 2025). For isExpired we do the same but going forward 20 years from the current year.

-------------------------------------

## AcuantFaceCapture ##

This module is used to automate capturing an image of a face appropriate for use with passive liveness.

1. Start the face capture activity:

		val cameraIntent = Intent(
			this@MainActivity,
			FaceCaptureActivity::class.java
		)

		/\*Optional, should only be used if you are changing some of the options, pointless to pass default options \*/
		cameraIntent.putExtra(ACUANT_EXTRA_FACE_CAPTURE_OPTIONS, FaceCaptureOptions())

		startActivityForResult(cameraIntent, YOUR_REQUEST_CODE)

2. Receive the result from the face capture activity:

		override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
			super.onActivityResult(requestCode, resultCode, data)

			if (requestCode == YOUR_REQUEST_CODE) {
				when (resultCode) {
					FaceCaptureActivity.RESPONSE_SUCCESS_CODE -> {
						val bytes = readFromFile(data?.getStringExtra(FaceCaptureActivity.OUTPUT_URL))
						val capturedSelfieImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
						//do whatever you want with the image
					}
					FaceCaptureActivity.RESPONSE_CANCEL_CODE -> {
						//handle user canceling
					}
					else -> {
						//handle error during capture
					}
				}
			}
		}
		
-------------------------------------

## AcuantPassiveLiveness ##

This module is used to determine liveness from a single selfie image.

1. Call and handle response:

		val plData = PassiveLivenessData(capturedSelfieImage)
		AcuantPassiveLiveness.processFaceLiveness(plData, object : PassiveLivenessListener {
			override fun passiveLivenessFinished(result: PassiveLivenessResult) {
				when (result.livenessAssessment) {
					AcuantPassiveLiveness.LivenessAssessment.Live -> {
						//handle live person
					}
					AcuantPassiveLiveness.LivenessAssessment.NotLive -> {
						//handle not live person
					}
					AcuantPassiveLiveness.LivenessAssessment.PoorQuality -> {
						//handle input image being too poor quality
					}
					else -> {
						//handle error
					}
				}
			}
		})

Relevant Enums:

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
			NotFound,
			InternalError,
			InvalidJson
		}
-------------------------------------

### Error codes ###

		public class ErrorCodes
		{
			public final static int ERROR_InvalidCredentials = -1;
			public final static int ERROR_InvalidLicenseKey = -2;
			public final static int ERROR_InvalidEndpoint = -3;
			public final static int ERROR_InitializationNotFinished = -4;
			public final static int ERROR_Network = -5;
			public final static int ERROR_InvalidJson = -6;
			public final static int ERROR_CouldNotCrop = -7;
			public final static int ERROR_NotEnoughMemory = -8;
			public final static int ERROR_BarcodeCaptureFailed = -9;
			public final static int ERROR_BarcodeCaptureTimedOut = -10;
			public final static int ERROR_BarcodeCaptureNotAuthorized = -11;
			public final static int ERROR_LiveFaceCaptureNotAuthorized = -12;
			public final static int ERROR_CouldNotCreateConnectInstance = -13;
			public final static int ERROR_CouldNotUploadConnectImage = -14;
			public final static int ERROR_CouldNotUploadConnectBarcode = -15;
			public final static int ERROR_CouldNotGetConnectData = -16;
			public final static int ERROR_CouldNotProcessFacialMatch = -17;
			public final static int ERROR_CardWidthNotSet = -18;
			public final static int ERROR_CouldNotGetHealthCardData = -19;
			public final static int ERROR_CouldNotClassifyDocument = -20;
			public final static int ERROR_LowResolutionImage = -21;
			public final static int ERROR_CAPTURING_FACIAL = -22;
			public final static int ERROR_NETWORK_FACIAL = -23;
			public final static int USER_CANCELED_FACIAL = -24;
		}
		
### Error descriptions ###

		public class ErrorDescriptions {
			public final static String ERROR_DESC_InvalidCredentials = "Invalid credentials";
			public final static String ERROR_DESC_InvalidLicenseKey = "Invalid License Key";
			public final static String ERROR_DESC_InvalidEndpoint = "Invalid endpoint";
			public final static String ERROR_DESC_InitializationNotFinished = "Initialization not finished";
			public final static String ERROR_DESC_InvalidJson = "Invalid Json response";
			public final static String ERROR_DESC_CouldNotCrop = "Could not crop image";
			public final static String ERROR_DESC_BarcodeCaptureFailed = "Barcode capture failed";
			public final static String ERROR_DESC_BarcodeCaptureTimedOut = "Barcode capture timed out";
			public final static String ERROR_DESC_BarcodeCaptureNotAuthorized = "Barcode capture is not authorized";
			public final static String ERROR_DESC_LiveFaceCaptureNotAuthorized = "Live face capture is not authorized";
			public final static String ERROR_DESC_CouldNotCreateConnectInstance = "Could not create connect Instance";
			public final static String ERROR_DESC_CouldNotUploadConnectImage = "Could not upload image to connect instance";
			public final static String ERROR_DESC_CouldNotUploadConnectBarcode = "Could not upload barcode to connect instance";
			public final static String ERROR_DESC_CouldNotGetConnectData = "Could not get connect image data";
			public final static String ERROR_DESC_CardWidthNotSet = "Card width not set";
			public final static String ERROR_DESC_CouldNotGetHealthCardData = "Could not get health card data";
			public final static String ERROR_DESC_CouldNotClassifyDocument = "Could not classify document";
			public final static String ERROR_DESC_LowResolutionImage = "Low resolution image";
			public final static String ERROR_DESC_NETWORK_FACIAL_IPROOV = "Failed to connect to IProov";
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

		class AcuantCameraOptions constructor(
			val timeInMsPerDigit: Int = 900,
			val digitsToShow: Int = 2,
			val allowBox : Boolean = true,
			val autoCapture : Boolean = true,
			val bracketLengthInHorizontal : Int = 155,
			val bracketLengthInVertical : Int = 255,
			val defaultBracketMarginWidth : Int = 160,
			val defaultBracketMarginHeight : Int = 160,
			val colorHold : Int = Color.YELLOW,
			val colorCapturing : Int = Color.GREEN,
			val colorBracketAlign : Int = Color.BLACK,
			val colorBracketCloser : Int = Color.RED,
			val colorBracketHold : Int = Color.YELLOW,
			val colorBracketCapturing : Int = Color.GREEN,
			val cardRatio : Float = 0.65f
		)
		
**Note:**   While the constructor has been left public for backwards compatibility purposes, we encourage everyone to instead update to one of the two Options Builders included with the class:

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
			}

-------------------------------------

## Frequently Asked Questions ##

#### How do I obfuscate my Android application? ####

Acuant does not provide obfuscation tools. See the Android developer documentation about obfuscation at: [https://developer.android.com/studio/build/shrink-code](https://developer.android.com/studio/build/shrink-code). Then open proguard-rules.pro for your project and set the obfuscation rules. 

-------------------------------------

**Copyright 2020 Acuant Inc. All rights reserved.**

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
        
