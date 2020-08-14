## How to migrate from Acuant Android SDK v11.4.* to v11.4.4 

**August 2020**

----------
### Updated IPLiveness

The new IP liveness workflow will speed up the launch of the camera as well as simplify the process and give more control to the user. The old workflow of calling getFacialCaptureIntent then manually launching that intent is deprecated and no longer supported. 

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