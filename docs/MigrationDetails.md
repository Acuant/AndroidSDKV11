# Migration Details
----------
## v11.6.2

**August 2025**

### New repository added to dependencies

Amongst other dependency updates, this version of the SDK uses a more up to date and more secure version of tesseract. In order to correctly fetch this dependency implementations need to include the following maven repository in their `repositories` block:

		maven { url 'https://jitpack.io' }
		
### Edge to edge mode

Per the requirements of android API 35 all activities started by the SDK will be in edge to edge mode. There are no code changes required due to this, however as this change can impact UX it is being mentioned in this section. See [Display content edge-to-edge in views](https://developer.android.com/develop/ui/views/layout/edge-to-edge) for more details.

----------
## v11.6.0

**February 2023**

### Updated String Keys

One camera string key has been removed, and two keys have been added. Localizations need to add the following two keys to maintain full localization:

		<string name="acuant_camera_too_close">TOO CLOSE</string>
		<string name="acuant_camera_out_of_bounds">NOT IN FRAME</string>
		
### Changes to Document Camera and Image Evaluation

This release includes changes to the way images are returned from the camera. These changes affect all implementations. In the past, the camera returned a path to a cache file that contained the image and any metadata. EvaluateImage created a similar cache to store the cropped image and metadata. Although cache images are regularly cleared out by the OS, the images could still last longer than needed. The SDK code could not delete the cached images because they had to remain for an arbitrary amount of time based on client workflow, and most implementations did not manually clear the cache or call the delete method provided with `AcuantImage`.

Certain operations require us to cache the image, so cached images are still in use. With this release, cached images are created and deleted internally with the implementer now receiving a reference to a byte array that contains the data, which was previously contained in the file. As a result, there is less time for an interruption to the application that could leave images in the internal cache. However, because caches are still used (although for a much briefer period of time), Acuant still recommends manually clearing the cache when the application is paused, destroyed, or otherwise interrupted.

Changes necessary to maintain the current workflow are as follows:

Replace

		val url = data?.getStringExtra(ACUANT_EXTRA_IMAGE_URL)

with

		val bytes = AcuantCameraActivity.getLatestCapturedBytes(clearBytesAfterRead = true)
		
Because the bytes of the image would be too large to pass as part of a parcel, they are accessed through a static method in `AcuantCameraActivity`. Acuant recommends, for security and memory use, to set`clearBytesAfterRead` to true.

		CroppingData(imageUrlString: String)
		
has been replaced with

		CroppingData(imageBytes: ByteArray)
		
### Updated models returned by web calls

The Document, Classification, Health Insurance, and Passive Liveness result models have been updated. These models are now in Kotlin resulting in more explicit nullability. Fields that are nullable in the web request are nullable in the Kotlin model, while those that are not nullable in the web request are not nullable in the Kotlin model. The structure of these models has been modified to more closely match the structure of the model returned through the web call. This modification reduces ambiguity and enables future fields in the web model to be mapped more easily to the Kotlin model. Fields also will more closely match their type (int to int, boolean to boolean, etc.) whereas, in the past, most fields were read as strings. Fields that represent an emun are now parsed into the appropriate enum. The unparsed value is still exposed if a new enum value is added in the web result and not yet mapped to the Kotlin model.

Breakdowns of the new Kotlin models are shown below. Although these models might seem overwhelming, large portions remain unchanged from the previous version.

Document + Classification:

		IDResult
			val alerts: List<DocumentAlert>
			val unparsedAuthenticationSensitivity: Int
			val authenticationSensitivity: AuthenticationSensitivity?
			val biographic: DocumentBiographic?
			val classification: Classification?
			val dataFields: List<DocumentDataField>
			val device: Device?
			val engineVersion: String?
			val fields: List<DocumentField>
			val images: List<DocumentImage>
			val instanceID: String
			val libraryVersion: String?
			val unparsedProcessMode: Int
			val processMode: DocumentProcessMode?
			val regions: List<DocumentRegion>
			val unparsedResult: Int
			val result: AuthenticationResult?
			val subscription: Subscription?
			val unparsedTamperResult: Int
			val tamperResult: AuthenticationResult?
			val unparsedTamperSensitivity: Int 
			val tamperSensitivity: AuthenticationSensitivity?

		DocumentAlert
			val actions: String?
			val description: String?
			val disposition: String?
			val id: String
			val information: String?
			val key: String?
			val model: String?
			val name: String?
			val unparsedResult: Int
			val result: AuthenticationResult?
			
		DocumentBiographic
			val age: Int
			val birthDate: String?
			val expirationDate: String?
			val fullName: String?
			val unparsedGender
			val gender: GenderType?
			val photo: String?
			
		Classification
			val unparsedMode: Int
			val mode: ClassificationMode?
			val orientationChanged: Boolean
			val presentationChanged: Boolean
			val classificationDetails: ClassificationDetails?
			val type: DocumentType?
			
		ClassificationDetails
			val front: DocumentType?
			val back: DocumentType?
			
		DocumentType
			val unparsedDocumentClass: Int
			val documentClass: DocumentClass?
			val classCode: String?
			val className: String?
			val countryCode: String?
			val unparsedDocumentDataTypes: List<Int>
			val documentDataTypes: List<DocumentDataType?>
			val geographicRegions: List<String>
			val id: String
			val isGeneric: Boolean
			val issue: String?
			val issueType: String?
			val issuerCode: String?
			val issuerName: String?
			val unparsedIssuerType: Int
			val issuerType: IssuerType?
			val keesingCode: String?
			val name: String?
			val unparsedReferenceDocumentDataTypes: List<Int>
			val referenceDocumentDataTypes: List<DocumentDataType?>
			val unparsedSize: Int
			val size: DocumentSize?
			val supportedImages: List<DocumentImageType>
			
		DocumentImageType
			val unparsedLight: Int
			val light: LightSource?
			val unparsedSide: Int
			val side: DocumentSide?
			
		DocumentDataField
			val unparsedDataSource: Int
			val dataSource: DocumentDataSource?
			val description: String?
			val id: String
			val isImage: Boolean
			val key: String?
			val name: String?
			val regionOfInterest: Rectangle
			val regionReference: String
			val reliability: Double
			val type: String?
			val value: String?

		Rectangle
			val height: Int
			val width: Int
			val x: Int
			val y: Int
			
		Device
			val hasContactlessChipReader: Boolean
			val hasMagneticStripeReader: Boolean
			val serialNumber: String?
			val type: DeviceType?
			
		DeviceType
			val manufacturer: String?
			val model: String?
			val unparsedSensorType: Int
			val sensorType: SensorType?
			
		DocumentField
			val dataFieldReferences: List<String>
			val unparsedDataSource: Int
			val dataSource: DocumentDataSource?
			val description: String?
			val id: String
			val isImage: Boolean
			val key: String?
			val name: String?
			val regionReference: String
			val type: String?
			val value: String?
			
		DocumentImage
			val glareMetric: Int?
			val horizontalResolution: Int
			val id: String
			val isCropped: Boolean?
			val isTampered: Boolean?
			val unparsedLight: Int
			val light: LightSource?
			val mimeType: String?
			val sharpnessMetric: Int?
			val unparsedSide: Int
			val side: DocumentSide?
			val uri: String?
			val verticalResolution: Int
			
		DocumentRegion
			val unparsedDocumentElement: Int
			val documentElement: DocumentElement?
			val id: String
			val imageReference: String
			val key: String?
			val rectangle: Rectangle
			
		Subscription
			val unparsedDocumentProcessMode: Int
			val documentProcessMode: DocumentProcessMode?
			val id: String
			val isActive: Boolean
			val isDevelopment: Boolean
			val isTrial: Boolean
			val name: String?
			val storePII: Boolean
			
Document + Classification Enums:

		AuthenticationResult
			Unknown(0), 
			Passed(1), 
			Failed(2), 
			Skipped(3), 
			Caution(4), 
			Attention(5)
			
		AuthenticationSensitivity
			Normal(0), 
			High(1), 
			Low(2)
			
		ClassificationMode
			Automatic(0), 
			Manual(1)
			
		DocumentClass
			Unknown(0), 
			Passport(1), 
			Visa(2), 
			DriversLicense(3),
			IdentificationCard(4), 
			Permit(5), 
			Currency(6), 
			ResidenceDocument(7),
			TravelDocument(8), 
			BirthCertificate(9), 
			VehicleRegistration(10), 
			Other(11),
			WeaponLicense(12), 
			TribalIdentification(13), 
			VoterIdentification(14),
			Military(15), 
			ConsularIdentification(16)
			
		DocumentDataSource
			None(0), 
			Barcode1D(1), 
			Barcode2D(2), 
			ContactlessChip(3),
			MachineReadableZone(4), 
			MagneticStripe(5), 
			VisualInspectionZone(6), 
			Other(7)
			
		DocumentDataType
			Barcode2D(0), 
			MachineReadableZone(1), 
			MagneticStripe(2)
			
		DocumentElement
			Unknown(0), 
			None(1), 
			Photo(2), 
			Data(3), 
			Substrate(4), 
			Overlay(5)
			
		DocumentProcessMode
			Default(0), 
			CaptureData(1), 
			Authenticate(2), 
			Barcode(3)
			
		DocumentSide
			Front(0), 
			Back(1)
			
		DocumentSize
			Unknown(0), 
			ID1(1), 
			ID2(2), 
			ID3(3),
			Letter(4), 
			CheckCurrency(5), 
			Custom(6)
			
		GenderType
			Unspecified(0), 
			Male(1), 
			Female(2), 
			Unknown(3)
			
		IssuerType
			Unknown(0), 
			Country(1), 
			StateProvince(2), 
			Tribal(3),
			Municipality(4), 
			Business(5), 
			Other(6)
			
		LightSource
			White(0), 
			NearInfrared(1), 
			UltravioletA(2), 
			CoaxialWhite(3),
			CoaxialNearInfrared(4)
			
		SensorType
			Unknown(0), 
			Camera(1), 
			Scanner(2), 
			Mobile(3)
			
Additionally `CardSide` was removed from `AcuantCommon` in favor of `DocumentSide` in `AcuantDocumentProcessing`
			
Health Insurance:

		HealthInsuranceCardResult
			var instanceID: String
			val copayEr: String?
			val copayOv: String?
			val copaySp: String?
			val copayUc: String?
			val coverage: String?
			val contractCode: String?
			val dateOfBirth: String?
			val deductible: String?
			val effectiveDate: String?
			val employer: String?
			val expirationDate: String?
			val firstName: String?
			val groupName: String?
			val groupNumber: String?
			val issuerNumber: String?
			val lastName: String?
			val memberId: String?
			val memberName: String?
			val middleName: String?
			val namePrefix: String?
			val nameSuffix: String?
			val other: String?
			val payerId: String?
			val planAdmin: String?
			val planProvider: String?
			val planType: String?
			val frontImage: Bitmap?
			val rawText: String?
			val rxBin: String?
			val rxGroup: String?
			val rxId: String?
			val rxPcn: String?
			val backImage: Bitmap?
			val listAddress: List<Address>
			val listPlanCode: List<PlanCode>
			val listTelephone: List<Telephone>
			val listEmail: List<Email>
			val listWeb: List<WebAddress>)
			val transactionTimestamp: String?
		
		Address
			val fullAddress: String?
			val street: String?
			val city: String?
			val state: String?
			val zip: String?
			
		PlanCode
			val planCode: String?
			
		Telephone
			val label: String?
			val value: String?
			
		Email
			val label: String?
			val value: String?
			
		WebAddress
			val label: String?
			val value: String?
			
Passive Liveness:

		PassiveLivenessResult
			val livenessResult: LivenessResult?
			val transactionId: String?
			val errorDesc: String?
			val unparsedErrorCode: String?
			val errorCode: PassiveLivenessErrorCode?
			
		LivenessResult
			val unparsedLivenessAssessment: String?
			val livenessAssessment: LivenessAssessment?
			val score: Int
			
Passive Liveness Enums:

		LivenessAssessment
			Error,
			PoorQuality,
			Live,
			NotLive

		PassiveLivenessErrorCode
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

### Notes on possible backwards incompatibility in Initialization

This release contains minor changes to initialization that might cause backwards incompatibility in some implementations. Most implementations will not be affected by these changes.

The changes are as follows:

- The two main functions used to initialize the SDK, `initializeWithToken` and `initialize`, remain unchanged. The recommended function to create a `Credential` prior to initializing the SDK, `initFromXml` also remains unchanged.

- The `Credential` and `Endpoint` classes were migrated to Kotlin. This means that fields that might have been nullable in the past might no longer be nullable. Some fields in the credential that used to be freely modifiable are now vals (set once during the creation of the object). Most implementations do not set or access values from these classes and, therefore, should be unaffected.

- The names of some endpoints inside the `Endpoint` class have changed. The old names have been left accessible with a deprecated flag. The names of the endpoints within the XML file remain unchanged. Most implementations do not need to set or access these values.

- The `Credential` class used to have many manual overloads of the static `init` and `initWithToken` methods. These methods were combined into singular Kotlin functions with some default parameters. Implementations that load the credential from an XML file (the recommended workflow) will be unaffected by this change. Implementations that load the credential using one of these two methods should be reviewed because the signatures have changed. In Kotlin implementations it is encouraged to explicitly set parameter names within the calls to the respective functions. The new signatures of the functions are shown below.

		fun init(
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
		
		fun initWithToken(
			token: String,
			acasEndpoint: String,
			assureIdEndpoint: String? = null,
			frmEndpoint: String? = null,
			passiveLivenessEndpoint: String? = null,
			ipLivenessEndpoint: String? = null,
			ozoneEndpoint: String? = null,
			healthInsuranceEndpoint: String? = null
		)
		
- The `parseXml` method in `AcuantInitializer` has been made internal. Most implementations do not use this method. The recommended replacement for it (if in use) is the previously existing static `initFromXml` within the `Credential` class.

- The list of packages to initialize no longer needs to contains packages that don't have content to initialize. `EchipInitializer` and `ImageProcessorInitializer` are no longer needed because those packages will now just check the initialization state of the active credential. The `MrzCameraInitializer` is still required (if MRZ reading is in use) because that class loads OCR data.

### Initialization Behavioral changes

- Previously all methods for creating the Credential and initializing the SDK would function only once. Future calls to these methods would be ignored.

- Now the Credential can be recreated at will and used to reinitialize the SDK. This allows use-cases where the implementer wants to swap credentials or endpoints after having performed one or more workflows.

----------
## v11.5.0

**January 2022**

### General Migration Details

- The Android SDK now uses AndroidX and is not compatible with most applications that do not use AndroidX.

- Instances in which credentials had to be specified by the implementer have been removed. The SDK will always use the result of Credential.get().

- The Error class in AcuantCommon has been renamed to AcuantError. The renaming helps prevent confusion between `kotlin.Error`, `java.lang.Error`, and `acuant.common.model.Error` because IDEs would always default to the first or second. Additionally, AcuantError now contains an additionalDetails field. This nullable field will in some circumstances contain information that is helpful in debugging, such as a stack trace or server response code, but follows no standard format. Error codes for network requests have been simplified to `ERROR_Network`, whereas previously, there were approximately 20 error codes that all meant the network request failed.

		class AcuantError(
			val errorCode: Int, 
			val errorDescription: String, 
			val additionalDetails: String? = null
		)

- Most instances in which a parameter was nullable are now non-nullable. Nullability was a carryover from a Java-heavy code base. If a null parameter would short circuit or otherwise prevent the function/class from working, that parameter is now non-nullable. Some exceptions exist (mostly to facilitate testing).

- Most listeners now extend off of a new interface, AcuantListener. This change standardizes error reporting across all modules of the Acuant SDK. Previously, errors were reported as a mixture of nullable fields, optional secondary parameters in callbacks, and various strings instead of a standard pattern. Now, most listeners have an onError method that corresponds with an error or failure.

		interface AcuantListener {
		    fun onError(error: AcuantError)
		}

- AsyncTasks (deprecated as of Android API 30) have been replaced with a new class called AcuantAsync. Methods that previously returned an AsyncTask now return AcuantAsync. Most methods that previously conducted async operations, but did not return an AsyncTask, now return an AcuantAsync class. AcuantAsync contains a single method called cancel(). This method attempts to cancel the relevant async operation. In most instances, this attempt will not have an effect because the async task will have started already and currently, most operations can't be interrupted after starting. The cancel() method is included mostly for future proofing.

- AcuantDocumentProcessing has been redesigned. Previously, methods returned information (such as instance id, whether the document side has been uploaded previously, and so forth) that the user had to keep track of and pass into subsequent methods in the workflow. After the redesign, starting the workflow returns an instanced object, and all further methods must be called on that object. The object then tracks the state of the instance within itself. This redesign creates neater workflows, reduces space for implementation mistakes, and increases support for maintaining more than one instance running simultaneously. For more information, see AcuantDocumentProcessing in the Readme

- AcuantHGLiveness has been removed as a standalone module and is accessible through the AcuantFaceCapture module. For more information, see AcuantFaceCapture in the Readme.

- The library used by AcuantIPLiveness has been updated to the latest version. As a result, additional functions have been added to IPLivenessListener. For more information, see AcuantIPLiveness in the Readme.

### CameraX and other camera changes

- Camera1 and Camera2 APIs are no longer used directly. The camera code has been rewritten to use the CameraX API. Otherwise, calling the camera remains mostly unchanged.

- The results of camera activities are returned along with `RESULT_OK`, `RESULT_ERROR`, and `RESULT_CANCELLED`. Previously, most responses came back as `AcuantCamerActivity.RESULT_SUCCESS_CODE`.

- The deprecated extras `ACUANT_EXTRA_IS_AUTO_CAPTURE` and `ACUANT_EXTRA_BORDER_ENABLED` in the Document Camera have been removed. All options are now passed through the AcuantCamerOptions object.

### Removal of deprecated methods

In addition to many other changes, 11.5.0 removes many classes/methods that had previously been marked as deprecated. This section covers only methods/classes that were previously marked as deprecated.

**AcuantCamera**

- Large Redesign
- CapturedImage => deleted (entirely unused code, no replacement needed)
- AcuantDocumentDectectorHandler => AcuantDocumentDetectorHandler (typo fix)
- AcuantCameraOptions => constructor now internal (use one of DocumentCameraOptionsBuilder, BarcodeCameraOptionsBuilder, or MrzCameraOptionsBuilder)

**AcuantCommon**

- Authorizations => replaced with SecureAuthorizations (Not used since ACAS)
- Resizer => replaced with resize method in AcuantImagePreparation
- AcuantInitializer.intialize(...) => replaced with AcuantInitializer.initialize(...) (typo fix)

**AcuantImagePreparation**

- AcuantImagePreparation.init(Credential, InitializationListener) => replaced with current ACAS based initialization (Not used since ACAS)
- ConnectInitializationWS => deleted as part of the above
- InitializationListener => deleted as part of the above
- NetworkListener => deleted as part of the above
- AcuantImagePreparation.detect(CroppingData) => replaced with detect(DetectData)
- AcuantImagePreparation.detectMrz(CroppingData) => replaced with detectMrz(DetectData)
- CroppingData() => constructor removed (pass the image url returned by the camera)
- CroppingData(Bitmap?) => constructor removed (pass the image url returned by the camera)
- CroppingData.image => now internal (use constructor, passing the image url returned by the camera)

**AcuantDocumentProcessing**

- Large Redesign
- AcuantDocumentUploadHelper => deleted (previously used internally)

**AcuantIPLiveness**

- FacialCaptureConstant.<< error codes >> => deleted (all error codes are now in common)
- FacialSetupResult(String, String, String, String, Boolean) => replaced with FacialSetupResult(String, String, String, Boolean) (removed apiKey)
- getFacialCaptureIntent(Context, FacialSetupResult) => replaced with runFacialCapture(Context, FacialSetupResult, IPLivenessListener)


----------
## v11.4.4

**August 2020**

### Updated IPLiveness

The new IP liveness workflow speeds up the launch of the camera and simplifies the process to give more control to the user. The old workflow of calling getFacialCaptureIntent then manually launching that intent is deprecated and no longer supported. 

The new workflow uses runFacialCapture that takes the same parameters as the previous method along with a new IPLivenessListener. 

In the old workflow the results for IP Liveness would be received through overrides of the onActivityResult method. In the new workflow all results are returned through methods in the listener 

		onSuccess(userId: String, token: String)
		onFail(error: Error)
		onCancel()
		
Additionally the old mandatory IP Liveness progress bar is gone. In its place is is a method in the listener

		onProgress(status: String, progress: Int)
		
Using the two parameters passed to this method you can display/stylize the progress bar in any way you want.

Additionally your app level gradle file needs the following under android:

		compileOptions {
			sourceCompatibility JavaVersion.VERSION_1_8
			targetCompatibility JavaVersion.VERSION_1_8
		}
		kotlinOptions {
			jvmTarget = "1.8"
		}
		
-------------------------------------------------------------
**Copyright 2022 Acuant Inc. All rights reserved.**

This document contains proprietary and confidential information and creative works owned by Acuant and its respective licensors, if any. Any use, copying, publication, distribution, display, modification, or transmission of such technology, in whole or in part, in any form or by any means, without the prior express written permission of Acuant is strictly prohibited. Except where expressly provided by Acuant in writing, possession of this information shall not be construed to confer any license or rights under any Acuant intellectual property rights, whether by estoppel, implication, or otherwise.

AssureID and *i-D*entify are trademarks of Acuant Inc. Other Acuant product or service names or logos referenced this document are either trademarks or registered trademarks of Acuant.

All 3M trademarks are trademarks of Gemalto Inc/Thales

Windows is a registered trademark of Microsoft Corporation.

Certain product, service, or company designations for companies other
than Acuant may be mentioned in this document for identification
purposes only. Such designations are often claimed as trademarks or
service marks. In all instances where Acuant is aware of a claim, the
designation appears in initial capital or all capital letters. However,
you should contact the appropriate companies for more complete
information regarding such designations and their registration status.

For technical support, go to: [https://support.acuant.com](https://support.acuant.com)

**Acuant Inc. 6080 Center Drive, Suite 850, Los Angeles, CA 90045**

----------------------------------------------------------