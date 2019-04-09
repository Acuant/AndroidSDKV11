# Acuant Android Mobile SDK v11


**Last updated – February 28, 2019**

Copyright <sup>©</sup> 2003-2018 Acuant Inc. All rights reserved.

This document contains proprietary and confidential 
information and creative works owned by Acuant and its respective
licensors, if any. Any use, copying, publication, distribution, display,
modification, or transmission of such technology in whole or in part in
any form or by any means without the prior express written permission of
Acuant is strictly prohibited. Except where expressly provided by Acuant
in writing, possession of this information shall not be
construed to confer any license or rights under any Acuant intellectual
property rights, whether by estoppel, implication, or otherwise.

AssureID and *i-D*entify are trademarks of Acuant Inc. Other Acuant product or service names or logos referenced this document are either trademarks or registered trademarks of Acuant.

All 3M trademarks are trademarks of Gemalto Inc.

Windows<sup>®</sup> is a registered trademark of Microsoft Corporation.

Certain product, service, or company designations for companies other
than Acuant may be mentioned in this document for identification
purposes only. Such designations are often claimed as trademarks or
service marks. In all instances where Acuant is aware of a claim, the
designation appears in initial capital or all capital letters. However,
you should contact the appropriate companies for more complete
information regarding such designations and their registration status.

**February 2019**

<p>Acuant Inc.</p>
<p>6080 Center Drive, Suite 850</p>
<p>Los Angeles, CA 90045</p>
<p>==================</p>


**Step - 0 : Setup:**

In the App manifest file, specify the permissions 
	
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
    
Add the Acuant SDK dependency in build.gradle
	
	repositories {
		//Face Capture and Barcode reading. Only add if using acuantcamera or acuanthgliveliness
   		maven { url 'https://maven.google.com' }
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

	    implementation('org.jmrtd:jmrtd:0.5.13') {
	        transitive = true;
	    }
	    implementation('org.ejbca.cvc:cert-cvc:1.4.3') {
	        transitive = true;
	    }
	    implementation('com.madgag.spongycastle:prov:1.54.0.0') {
	        transitive = true;
	    }
	    implementation('net.sf.scuba:scuba-sc-android:0.0.9') {
	        transitive = true;
	    }
	    //end echip reading
	    
	    //internal common library
	    implementation project(path: ':acuantcommon')
	    
	    //camera with autocapture - Uses camera 1 API
	    implementation project(path: ':acuantcamera')
	    
	    //document parse, classifcation, authentication
	    implementation project(path: ':acuantdocumentprocessing')
	    
	    //face match library
	    implementation project(path: ':acuantfacematchsdk')
	    
	    //for reading epassport chips
	    implementation project(path: ':acuantechipreader')
	    
	    //face capture and liveliness
	    implementation project(path: ':acuanthgliveliness')
	    
	    //image processing (cropping, glare, sharpness)
	    implementation project(path: ':acuantimagepreparation')
    }
**Acuant Common Library :**

- Contains all shared internal models and services for Acuant.

**Acuant Camera Library :**

- Implemented in Camera 1 API with Google Vision for barcode reading
- Uses AcuantImageProcessor for cropping

**Acuant Document Processing Library :**

- Contains all the services to connect to AssureID for document parsing, classifcation, and verfication.
- Contains all the services to use face match feature. It makes a web service call to Acuant FRM endpoint.
- Will match two images and return a match score from 0-100.

**Acuant EChip Reader Library :**

- Contains passport Echip reading feature.
- Can parse Echip on passports.

**Acuant HG Liveliness Library :**

- Uses Google vision for face recognizition.
- Uses Camera 1 to capture face.
- Uses internal detection to calculate liveliness.

**Acuant Image Preparation Library :**

- Contains all image processing.
- Cropping documents, caculating sharpness and glare.	
Please refer to the sample Sample App to check how to set up the permissions and dependencies correctly    
    
**Step - 1 : Setup Acuant SDK :**

1. 	Create an xml file with the following tags.

		<?xml version="1.0" encoding="UTF-8" ?>
		<setting>
		    <acuant_username></acuant_username>
		    <acuant_password></acuant_password>
		    <acuant_subscription></acuant_subscription>
		    <frm_endpoint></frm_endpoint>
		    <med_endpoint></med_endpoint>
		    <assureid_endpoint></assureid_endpoint>
		</setting>

1.	Save file in application assets directory

		{PROJECT_ROOT_DIRECTORY} => app => src => main => assets => PATH/TO/CONFIG/FILENAME.XML

1.	Initialize SDK in Application Instance.

		class AppInstance : Application() {
		
		    override fun onCreate() {
		        super.onCreate()
		        
		        //give path to xml file created above, path will start from "assets" as root
		        //pass in Context from Application
		        //list of packages to initialize. Only need to initialize ImageProcessor
       			AcuantInitializer.intialize("PATH/TO/CONFIG/FILENAME.XML", this, listOf(ImageProcessorInitializer()))
		    }
		}
			
**Step - 2 : Capture an Image :**

1. Start camera activity.

        val cameraIntent = Intent(this, AcuantCameraActivity::class.java)
		cameraIntent.putExtra(ACUANT_EXTRA_IS_AUTO_CAPTURE, boolean)//default is true

        startActivityForResult(cameraIntent, REQUEST_CODE) 
        
1. Get activity result.
	
		override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        	super.onActivityResult(requestCode, resultCode, data)
        	
        	if (requestCode == REQUEST_CODE && AcuCameraActivity.RESULT_SUCCESS_CODE) 
        	{
            	val capturedImageUrl = data?.getStringExtra(ACUANT_EXTRA_IMAGE_URL)
            	val capturedBarcodeString = data?.getStringExtra(ACUANT_EXTRA_PDF417_BARCODE)
        	}
        }

		 
**Step - 3 : Cropping Image :**

Once image is captured, its sent to the cropping library for cropping.

1. Setting cropping options.

	Set whether the captured images is an Health Insurance card or not and whether Image metrics (Sharpness and Glare) are required or not.
	
		class CroppingOptions (
			val imageMetricsRequired: Boolean = false, 
			val isHealthCard: Boolean = false)
		
1.	Setting the Image to be cropped
		
		class CroppingData (val image: Bitmap)
		
1. Crop.

		@JvmStatic fun crop(options: CroppingOptions, data: CroppingData)
 		
 		val acuantImage : Image = AcuantImagePreparation.crop(options, data);
 		
1. Image result.

		class Image (
			val image: Bitmap, 
			val hasImageMetrics: Boolean,
			val isSharp: Boolean,
			val hasGlare: Boolean,
			val sharpnessGrade: Float,
			val glareGrade: Float,
			val dpi: Int,
			val aspectRatio: Float,
			val isCorrectAspectRatio: Boolean,
			val error: Error)
		
		
		
**Step 4 : Process captured images (Web Service call) :**


1. Create Instance
		
		class IdOptions (
			//data to be returned from processing
			//set to ProcessingMode.Authentication for document valdiation
			val processingMode: ProcessingMode = ProcessingMode.Default,
			
			//front or back side of document
			val cardSide: CardSide = CardSide.Front,
			
			//if we need to reupload image
			val isRetrying: Boolean = false,
			
			//if is health card
			val isHealthCard: Boolean = false)

		AcuantDocumentProcessor.createInstance(idOptions) 
		{ instanceId, error ->
			if(error != null){
				//handle error
			}
			else{
				//save instanceId for future use
			}
		}
		
1. Upload Image and classify.

		class IdData (val image: Bitmap, val barcodString: String?)

		AcuantDocumentProcessor.uploadImage(instanceId, idData, idOptions) 
		{ error, classification ->
            if (error == null) {
                // Successfully uploaded
                if(classification.presentationChanged){
                	//back image was uploaded, now upload front image
                }
                else if(isBackSideRequired(classification)) {
                  	//upload back image
                }else{
                    //get data
                }

            } else {
                //handle error  
            }
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

1. Upload another image (If needed).

1. Get Data.
		
		AcuantDocumentProcessor.getData(instanceId, isHealthCard) 
		{ result ->
            if (result == null || result.error != null) {
                //handle error
            } 
            else {
                //success!! use result
            }
        }
        
**Step 5: Face Capture with AcuantHGLiveliness:**

1. Start Activity

		val cameraIntent = Intent(
                this@MainActivity,
                FacialLivelinessActivity::class.java
        )
        startActivityForResult(cameraIntent, Constants.REQUEST_CAMERA_PHOTO)
        
1. Get Activity result.

		
		override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		    super.onActivityResult(requestCode, resultCode, data)
		
		    if (requestCode == REQUEST_CODE) {
	            if(resultCode == 2){
            		val faceImage = FaceCapturedImage.bitmapImage
	            }
	            else{
            		//handle error
	            }
        	}
		}
		
 
**Step 6: Face Match :**

1. Set face match data.
		 
		 class FacialMatchData (faceImageOne: Bitmap, faceImageTwo:Bitmap)

2. Start face match request with controller. Use result.

		class FacialMatchResult (isMatch: Boolean, score: Int, transactionId: String, error:Error)

		AcuantFaceMatch.processFacialMatch(facialMatchData, object : FacialMatchListener {
	        override fun facialMatchFinished(result: FacialMatchResult?) {
        			//use result
	            }
	        }
        })



**Extra Classes :**
	
1. ErrorCodes 

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
		
1. Error Description 

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




        