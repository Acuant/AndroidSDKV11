# Migration Details
----------
## v11.5.0

**January 2022**

### General Migration Details

- The Android SDK now uses AndroidX and is not compatible with most applications that do not use AndroidX.

- Instances in which credentials had to be specified by the implementer have been removed. The SDK will always use the result of Credential.get().

- The Error class in AcuantCommon has been renamed to AcuantError. The renaming helps prevent confusion between `kotlin.Error`, `java.lang.Error`, and `acuant.common.model.Error` because IDEs would always default to the first or second. Additionally, AcuantError now contains and additionalDetails field. This nullable field will in some circumstances contain information that is helpful in debugging, such as a stack trace or server response code, but follows no standard format. Error codes for network requests have been simplified to `ERROR_Network`, whereas previously, there were approximately 20 error codes that all meant the network request failed.

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
**Copyright 2020 Acuant Inc. All rights reserved.**

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