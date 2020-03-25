package com.acuant.sampleapp

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.acuant.acuanthgliveness.detector.LiveFaceDetector
import com.acuant.acuanthgliveness.detector.LiveFaceListener
import com.acuant.acuanthgliveness.detector.LiveFaceProcessor
import com.acuant.acuanthgliveness.model.FaceCapturedImage
import com.acuant.acuanthgliveness.model.LiveFaceDetails
import com.acuant.sampleapp.facecapture.CameraSourcePreview
import com.acuant.sampleapp.facecapture.FacialGraphic
import com.acuant.sampleapp.facecapture.FacialGraphicOverlay
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.vision.CameraSource
import java.io.IOException
import kotlin.concurrent.thread


class FacialLivenessActivity : AppCompatActivity(), LiveFaceListener {

    private var mCameraSource: CameraSource? = null
    private var mPreview: CameraSourcePreview? = null
    private var mFacialGraphicOverlay: FacialGraphicOverlay? = null
    private var mFacialGraphic: FacialGraphic? = null
    private var selfieCaptured = false
    private var liveFaceDetector : LiveFaceDetector? = null
    private var targetFps: Float = 10f
    //==============================================================================================
    // Activity Methods
    //==============================================================================================

    /**
     * Initializes the UI and initiates the creation of a face detector.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_facial_liveness)

        mPreview = findViewById<View>(R.id.preview) as CameraSourcePreview
        mFacialGraphicOverlay = findViewById<View>(R.id.faceOverlay) as FacialGraphicOverlay

        targetFps = intent.getFloatExtra(Constants.HG_FRAME_RATE_TARGET, 10f)

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        val rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource()
        } else {
            requestCameraPermission()
        }
    }

    /**
     * Handles the requesting of the camera permission.  This includes showing a "Snackbar" message
     * of why the permission is needed then sending the request.
     */
    private fun requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission")

        val permissions = arrayOf(Manifest.permission.CAMERA)

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM)
            return
        }

        val thisActivity = this

        val listener = View.OnClickListener {
            ActivityCompat.requestPermissions(thisActivity, permissions,
                    RC_HANDLE_CAMERA_PERM)
        }

        Snackbar.make(mFacialGraphicOverlay!!, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show()
    }

    /**
     * Restarts the camera.
     */
    override fun onResume() {
        super.onResume()
        mFacialGraphic = FacialGraphic(mFacialGraphicOverlay!!)
        startCameraSource()
    }

    /**
     * Stops the camera.
     */
    override fun onPause() {
        super.onPause()
        mPreview!!.stop()
    }

    /**
     * Releases the resources associated with the camera source, the associated detector, and the
     * rest of the processing pipeline.
     */
    override fun onDestroy() {
        super.onDestroy()
        if (mCameraSource != null) {
            mCameraSource!!.release()
        }
        if(liveFaceDetector!=null){
            liveFaceDetector!!.release()
        }
    }

    /**
     * Callback for the result from requesting permissions. This method is invoked for every call on
     * [.requestPermissions].
     *
     *
     *
     * **Note:** It is possible that the permissions request interaction with the user
     * is interrupted. In this case you will receive empty permissions and results arrays which
     * should be treated as a cancellation.
     *
     *
     *
     * @param requestCode  The request code passed in [.requestPermissions].
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     * which is either [PackageManager.PERMISSION_GRANTED]
     * or [PackageManager.PERMISSION_DENIED]. Never null.
     * @see .requestPermissions
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: $requestCode")
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            return
        }

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source")
            // we have permission, so create the camerasource
            createCameraSource()
            return
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.size +
                " Result code = " + if (grantResults.isNotEmpty()) grantResults[0] else "(empty)")

        val listener = DialogInterface.OnClickListener { _, _ -> finish() }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Face Tracker sample")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show()
    }

    //==============================================================================================
    // Camera Source
    //==============================================================================================

    /**
     * Creates the face detector and the camera.
     */

    private fun createCameraSource() {
        val context = applicationContext
        liveFaceDetector = LiveFaceProcessor.initLiveFaceDetector(context, this)
        val facing = CameraSource.CAMERA_FACING_FRONT
        // The camera source is initialized to use either the front or rear facing camera.  We use a
        // relatively low resolution for the camera preview, since this is sufficient for this app
        // and the face detector will run faster at lower camera resolutions.
        //
        // However, note that there is a speed/accuracy trade-off with respect to choosing the
        // camera resolution.  The face detector will run faster with lower camera resolutions,
        // but may miss smaller faces, landmarks, or may not correctly detect eyes open/closed in
        // comparison to using higher camera resolutions.  If you have any of these issues, you may

        // want to increase the resolution.
        mCameraSource = CameraSource.Builder(context, liveFaceDetector)
                .setFacing(facing)
                .setRequestedFps(targetFps)
                .setAutoFocusEnabled(true)
                .build()
    }

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private fun startCameraSource() {
        // check that the device has play services available.
        val code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                applicationContext)
        if (code != ConnectionResult.SUCCESS) {
            val dlg = GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS)
            dlg.show()
        }

        if (mCameraSource != null) {
            try {
                mPreview!!.start(mCameraSource!!, mFacialGraphicOverlay!!)
            } catch (e: IOException) {
                Log.e(TAG, "Unable to start camera source.", e)
                mCameraSource!!.release()
                mCameraSource = null
            }

        }
    }

    override fun liveFaceDetailsCaptured(liveFaceDetails: LiveFaceDetails) {
        if (liveFaceDetails.error == null) {
            mFacialGraphicOverlay!!.setState(liveFaceDetails.state)
            mFacialGraphicOverlay!!.add(mFacialGraphic!!)
            mFacialGraphic!!.updateLiveFaceDetails(liveFaceDetails)
            if(liveFaceDetails.isLiveFace){
                if(!selfieCaptured) {
                    selfieCaptured = true
                    thread {
                        FaceCapturedImage.setImage(liveFaceDetails.image)
                        val result = Intent()
                        this@FacialLivenessActivity.setResult(RESPONSE_SUCCESS_CODE, result)
                        this@FacialLivenessActivity.finish()
                    }
                }
            }
        } else {
            mFacialGraphicOverlay!!.clear()
        }
    }

    companion object {
        private const val TAG = "GooglyEyes"

        private const val RC_HANDLE_GMS = 9001

        // permission request codes need to be < 256
        private const val RC_HANDLE_CAMERA_PERM = 2

        const val RESPONSE_SUCCESS_CODE = 2
    }
}