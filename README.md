# Acuant Android SDK v11.2.3
**October 2019**

## Introduction ##

This document provides detailed information about the Acuant Android SDK. The Acuant recommended workflow is described below.

![](https://i.imgur.com/KR0J94S.png)

**Note** The accceptable quality image is well-cropped, sharp and with no glare present, has a resolution of at least 300 dpi (for data capture) or 600 dpi (for authentication). The aspect ratio should be acceptable and matches an ID document.


## Modules ##

The SDK includes the following modules:

**Acuant Common Library (AcuantCommon) :**

- Contains all shared internal models and supporting classes

**Acuant Camera Library (AcuantCamera) :**

- Implemented using Camera 2 API with Google Vision for reading PDF417 barcodes
- Uses AcuantImagePreparation for cropping.

**Acuant Image Preparation Library (AcuantImagePreparation) :**

- Contains all image processing including cropping and calculation of sharpness and glare

**Acuant Document Processing Library (AcuantDocumentProcessing) :**

- Contains all the methods to upload the document images, process, and get results

**Acuant Face Match Library (AcuantFaceMatch) :**    

- Contains a method to match two facial images 

**Acuant EChip Reader Library (AcuantEChipReader):**

- Contains methods for e-Passport chip reading and authentication 

**Acuant IP Liveness Library (AcuantIPLiveness):**

- Uses library for capturing a facial image and calculating liveness
- Enhanced Face Liveness

**Acuant HG Liveness Library (AcuantHGLiveness):**

- Uses Camera 1 to capture facial liveness using a proprietary algorithm



### Setup ###

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
    
2. Add the Acuant SDK dependency in **build.gradle**:

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

		//Face Capture and Barcode reading. Only add if using acuantcamera or acuanthgliveliness
	    implementation 'com.google.android.gms:play-services-vision:15.0.2'
	    
	    //external libraries for echip reading
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
	    
	    //document parse, classifcation, authentication
	    implementation project(path: ':acuantdocumentprocessing')
	    
	    //face match library
	    implementation project(path: ':acuantfacematchsdk')
	    
	    //for reading epassport chips
	    implementation project(path: ':acuantechipreader')

    	 //face capture and liveliness
	    implementation project(path: ':acuantipliveness')
	    implementation('com.iproov.sdk:iproov:4.4.0@aar') {
	        transitive = true
	    }
	    
	    //face capture and liveliness
	    implementation project(path: ':acuanthgliveness')
	    
	    //image processing (cropping, glare, sharpness)
	    implementation project(path: ':acuantimagepreparation')
  		}
  		
3. Add the Acuant SDK dependency in **build.gradle** if using Maven

	- Add the following Maven URL
	
			maven {url  "https://dl.bintray.com/acuant/Acuant"}
        	maven { url 'https://raw.githubusercontent.com/iProov/android/master/maven/' }
        	
    - Add the following depedencies

    		implementation 'com.acuant:acuantcommon:11.2.3'
    		implementation 'com.acuant:acuantcamera:11.2.3'
    		implementation 'com.acuant:acuantimagepreparation:11.2.3'
    		implementation 'com.acuant:acuantdocumentprocessing:11.2.3'
    		implementation 'com.acuant:acuantechipreader:11.2.3'
    		implementation 'com.acuant:acuantfacematch:11.2.3'
    		implementation 'com.acuant:acuanthgliveness:11.2.3'
    		implementation ('com.acuant:acuantipliveness:11.2.3'){
        		transitive = true
    		}


4. 	Create an xml file with the following tags:

		<?xml version="1.0" encoding="UTF-8" ?>
		<setting>
		    <acuant_username></acuant_username>
		    <acuant_password></acuant_password>
		    <acuant_subscription></acuant_subscription>
		    <frm_endpoint></frm_endpoint>
		    <med_endpoint></med_endpoint>
		    <assureid_endpoint></assureid_endpoint>
		</setting>

5.	Save the file in the application assets directory:

		{PROJECT_ROOT_DIRECTORY} => app => src => main => assets => PATH/TO/CONFIG/FILENAME.XML
			
### Capture an image using AcuantCamera ###

1. Start camera activity:

        val cameraIntent = Intent(this, AcuantCameraActivity::class.java)
        
        cameraIntent.putExtra(ACUANT_EXTRA_IS_AUTO_CAPTURE, boolean)//default is true
        cameraIntent.putExtra(ACUANT_EXTRA_BORDER_ENABLED, boolean)//default is true

        startActivityForResult(cameraIntent, REQUEST_CODE) 
Alternatively use the new options object. This method allows you to configure much more about the camera (see AcuantCameraOptions):
        
        val cameraIntent = Intent(this, AcuantCameraActivity::class.java)
        
        cameraIntent.putExtra(ACUANT_EXTRA_CAMERA_OPTIONS, AcuantCameraOptions(allowBox = boolean, autoCapture = boolean)
        
        startActivityForResult(cameraIntent, REQUEST_CODE) 
        
2. Get activity result:
	
		override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        	super.onActivityResult(requestCode, resultCode, data)
        	
        	if (requestCode == REQUEST_CODE && AcuCameraActivity.RESULT_SUCCESS_CODE) 
        	{
            	val capturedImageUrl = data?.getStringExtra(ACUANT_EXTRA_IMAGE_URL)
            	val capturedBarcodeString = data?.getStringExtra(ACUANT_EXTRA_PDF417_BARCODE)
        	}
        }

	**Note:**   **AcuantCamera** is depdendent on **AcuantImagePreparation** and  **AcuantCommon**.

		 
#### AcuantImagePreparation ####

This section describes how to use **AcuantImagePreparation**.

- **Initialization**

		class MainActivity : Activity() {
		
		    override fun onCreate() {
		        super.onCreate()
		        
		        //Specify the path to the previously created XML file, using “assets” as root
		        //Pass in Context from Application
		        //List the packages to intitialize; only ImageProcessor is required

				try{
					AcuantInitializer.intialize("PATH/TO/CONFIG/FILENAME.XML", this, 
						listOf(ImageProcessorInitializer()), 
						object: IAcuantPackageCallback{
							override fun onInitializeSuccess() {
								//success
							}
							
							override fun onInitializeFailed(error: List<Error>) {
								//error
							}
						})
				}
				catch(e: AcuantException){
					Log.e("Acuant Error", e.toString())
				}
		    }
		}
		
		

	**Note:** If you are *not* using a configuration file for initialization, then use the following statement (providing appropriate credentials for *username*, *password*, and *subscription ID*):
	
		Credential.init("xxxxxxx",
		"xxxxxxxx",
		"xxxxxxxxxx",
		"https://frm.acuant.net",
		"https://services.assureid.net",
		"https://medicscan.acuant.net")
		AcuantInitializer.intialize("", this, listOf(ImageProcessorInitializer()), callback)
		
#### Initialization without a Subscription ID ####

**AcuantImagePreparation** may be initialized by providing only a username and a password. However, without providing a Subscription ID, the application can *only* capture an image and get the image. Without a Subscription ID:

1. Only the **AcuantCamera**, **AcuantImagePreparation**, and **AcuantHGLiveness** modules may be used.
2. The SDK can be used to capture the identity documents.
3. The captured images can be exported from the SDK. See the **onActivityResult** in the **MainActivity** of the sample application.

		override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        	if (requestCode == Constants.REQUEST_CAMERA_PHOTO && 
        		resultCode == AcuantCameraActivity.RESULT_SUCCESS_CODE) {
            		val bytes = readFromFile(data?.getStringExtra(ACUANT_EXTRA_IMAGE_URL))
            		val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size);
            		capturedBarcodeString = data?.getStringExtra(ACUANT_EXTRA_PDF417_BARCODE)
        	}
        	...
        }

- **Crop** 

	After an image is captured, it is sent to the cropping library for cropping.


		//CroppingData & Image are part of AcuantCommon
	
		val data = CroppingData()
		data.image = image
		acuantImage = AcuantImagePreparation.crop(data)
 		
- **Sharpness**

	This method returns the sharpness value of an image. If the sharpness value is greater than 50, then the image is considered sharp.

		public static Integer sharpness(Bitmap image)
		
- **Glare**

	This method returns the glare value of an image. If glare value is greater than 50, then the image does not have glare.

		public static Integer glare(Bitmap image)
		
### AcuantDocumentProcessing ###

After you capture a document image is captured, use the following steps to process the image.

**Note:**  If an upload fails with an error, retry the image upload using a better image.

1. Create an instance:
		
		public static void createInstance(IdOptions options, CreateInstanceListener listener)
		
		public interface CreateInstanceListener {
    		void instanceCreated(String instanceId, Error error);
		}
		
2. Upload an image:

		public static void uploadImage(String instanceID, IdData idData, IdOptions options, UploadImageListener listener)
		
		public interface UploadImageListener {
    		void imageUploaded(Error error, Classification classification);
		}
		
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
		
		
### AcuantIPLiveness ###
1. Get the setup from the controller and begin Activity:

		AcuantIPLiveness.getFacialSetup(object :FacialSetupLisenter{
            override fun onDataReceived(result: FacialSetupResult?) {
                if(result != null){
                	//start face capture activity
                    val facialIntent = AcuantIPLiveness.getFacialCaptureIntent(this@MainActivity, result)
                    startActivityForResult(facialIntent, REQUEST_CODE)
                }
                else{
                	//handle error
                }
            }

            override fun onError(errorCode: Int, description: String?) {
                //handle error
            }
        })
        
2. Get the Activity result:
		
		override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		    super.onActivityResult(requestCode, resultCode, data)
		
		    if (requestCode == REQUEST_CODE) {
	            if(resultCode == ErrorCodes.ERROR_CAPTURING_FACIAL){
	                //handle capture error
	            }
	            else if (resultCode == ErrorCodes.USER_CANCELED_FACIAL){
	                //user canceled activity
	            }
	            else{
	        	   	//success capturing. now get the result with parameters.
	                val userId = data?.getStringExtra(FacialCaptureConstant.ACUANT_USERID_KEY)!!
	                val token = data?.getStringExtra(FacialCaptureConstant.ACUANT_TOKEN_KEY)!!
	            }
        	}
		}
		
3. Get the facial capture result:
		
		//isPassed = true if face is live. false if face is not live.
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

	
### AcuantFaceMatch ###

This module is used to match two facial images:

	fun processFacialMatch(facialData: FacialMatchData, listener: FacialMatchListener?)
	
	public interface FacialMatchListener {
    	public void facialMatchFinished(FacialMatchResult result);
	}

### AcuantEChipReader ###

1. Check that the permission is provided in the manifest file:

		<uses-permission android:name="android.permission.NFC" />

2. Make sure that the NFC sensor on the device is turned on.

3. Initialize the Android NFC Adapter:

		NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);

4. Use the SDK API to listen to NFC tags available in an e-Passport:

		public static void listenNFC(Activity activity,
		 NfcAdapter nfcAdapterParam, NFCTagReadingListener listener)
		 
		 public interface NFCTagReadingListener {
    		public void tagReadSucceeded(final NFCData nfcData, 
    		final Bitmap face_image, final Bitmap sign_image);
    	
    		public void tagReadFailed(final String tag_read_error_message);
		}
		
	If an NFC tag is discovered, then the control will return to the method of the Activity that was previously overridden:

		override fun onNewIntent(intent: Intent) {
        	super.onNewIntent(intent)
        
        	// Read the information from the tag as below
        	AcuantEchipReader.readNFCTag(this, intent, docNumber, dateOfBirth, dateOfExpiry)
    	}

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
            val colorBracketCapturing : Int = Color.GREEN
        )

### IdOptions ###

        public class IdOptions {
            public CardSide cardSide;
            public boolean isRetrying;
            public boolean isHealthCard;
            public AuthenticationSensitivity authenticationSensitivity;
        }


## Frequently Asked Questions ##

#### How do I obfuscate my Android application? ####

Acuant does not provide obfuscation tools. See the Android developer documentation about obfuscation at: [https://developer.android.com/studio/build/shrink-code](https://developer.android.com/studio/build/shrink-code). Then open proguard-rules.pro for your project and set the obfuscation rules. 

-------------------------------------

**Copyright 2019 Acuant Inc. All rights reserved.**

This document contains proprietary and confidential information and creative works owned by Acuant and its respective licensors, if any. Any use, copying, publication, distribution, display, modification, or transmission of such technology, in whole or in part, in any form or by any means, without the prior express written permission of Acuant is strictly prohibited. Except where expressly provided by Acuant in writing, possession of this information shall not be construed to confer any license or rights under any Acuant intellectual property rights, whether by estoppel, implication, or otherwise.

AssureID and *i-D*entify are trademarks of Acuant Inc. Other Acuant product or service names or logos referenced this document are either trademarks or registered trademarks of Acuant.

All 3M trademarks are trademarks of Gemalto Inc.

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
        
