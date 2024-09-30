package com.acuant.acuantfacecapture.camera

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.core.impl.utils.Exif
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.acuant.acuantcommon.model.AcuantError
import com.acuant.acuantcommon.model.ErrorCodes
import com.acuant.acuantcommon.model.ErrorDescriptions
import com.acuant.acuantfacecapture.databinding.FragmentFaceCameraBinding
import com.acuant.acuantfacecapture.interfaces.IFaceCameraActivityFinish
import com.acuant.acuantfacecapture.model.FaceCaptureOptions
import com.acuant.acuantfacecapture.R
import com.acuant.acuantfacecapture.interfaces.IAcuantSavedImage
import com.acuant.acuantimagepreparation.AcuantImagePreparation
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


abstract class AcuantBaseFaceCameraFragment: Fragment() {

    private var displayId: Int = -1
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    protected var capturing: Boolean = false
    protected var fragmentCameraBinding: FragmentFaceCameraBinding? = null
    protected var imageAnalyzer: ImageAnalysis? = null //set up by implementations
    protected lateinit var cameraExecutor: ExecutorService
    protected lateinit var cameraActivityListener: IFaceCameraActivityFinish
    protected lateinit var acuantOptions: FaceCaptureOptions

    private val permissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            setUpCamera()
        } else {
            val alertDialog = AlertDialog.Builder(requireContext()).create()
            alertDialog.setMessage(getString(R.string.no_camera_permission))
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.ok)) { _, _ ->
                cameraActivityListener.onError(AcuantError(ErrorCodes.ERROR_Permissions, ErrorDescriptions.ERROR_DESC_Permissions))
                alertDialog.dismiss()
            }
            alertDialog.setOnCancelListener {
                cameraActivityListener.onError(AcuantError(ErrorCodes.ERROR_Permissions, ErrorDescriptions.ERROR_DESC_Permissions))
                alertDialog.dismiss()
            }
            alertDialog.show()
        }
    }

    abstract fun resetWorkflow()

    override fun onPause() {
        capturing = false
        resetWorkflow()
        super.onPause()
    }

    override fun onDestroyView() {
        fragmentCameraBinding = null
        // Shut down our background executor
        imageAnalyzer?.clearAnalyzer()
        cameraExecutor.shutdown()
        cameraProvider?.unbindAll()
        super.onDestroyView()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentCameraBinding = FragmentFaceCameraBinding.inflate(inflater, container, false)
        return fragmentCameraBinding!!.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraActivityListener = requireActivity() as IFaceCameraActivityFinish

        val opts = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireArguments().getSerializable(INTERNAL_OPTIONS, FaceCaptureOptions::class.java)
        } else {
            @Suppress("DEPRECATION")
            requireArguments().getSerializable(INTERNAL_OPTIONS) as FaceCaptureOptions?
        }

        if (opts != null) {
            acuantOptions = opts
        } else {
            cameraActivityListener.onError(AcuantError(ErrorCodes.ERROR_UnexpectedError, ErrorDescriptions.ERROR_DESC_UnexpectedError, "Options were unexpectedly null"))
            return
        }

        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Wait for the views to be properly laid out
        fragmentCameraBinding?.viewFinder?.post {
            val binding = fragmentCameraBinding

            if (binding != null) {
                // Keep track of the display in which this view is attached
                displayId = binding.viewFinder.display.displayId

                requestCameraPermissionIfNeeded ()
            }
        }
    }

    private fun requestCameraPermissionIfNeeded() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                val alertDialog = AlertDialog.Builder(requireContext()).create()
                alertDialog.setMessage(getString(R.string.cam_perm_request_text))
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.ok)) { _, _ ->
                    permissionRequest.launch(Manifest.permission.CAMERA)
                }
                alertDialog.setOnCancelListener {
                    permissionRequest.launch(Manifest.permission.CAMERA)
                }
                alertDialog.show()
            } else {
                permissionRequest.launch(Manifest.permission.CAMERA)
            }
        } else {
            setUpCamera()
        }
    }

    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({

            // CameraProvider
            cameraProvider = cameraProviderFuture.get()

            if (!hasFrontCamera()) {
                cameraActivityListener.onError(AcuantError(ErrorCodes.ERROR_UnexpectedError, ErrorDescriptions.ERROR_DESC_UnexpectedError, "Phone does not have a camera/app can not see the camera"))
                return@addListener
            }
            // Build and bind the camera use cases
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun hasFrontCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false
    }

    /** Declare and bind preview, capture and analysis use cases */
    private fun bindCameraUseCases() {

        val width: Int = requireContext().resources.displayMetrics.widthPixels
        val height: Int = requireContext().resources.displayMetrics.heightPixels

        val screenAspectRatio = aspectRatio(width, height)

        val rotation = fragmentCameraBinding!!.viewFinder.display.rotation

        // CameraProvider
        val cameraProvider = cameraProvider

        if (cameraProvider == null) {
            cameraActivityListener.onError(AcuantError(ErrorCodes.ERROR_UnexpectedError, ErrorDescriptions.ERROR_DESC_UnexpectedError, "Camera initialization failed."))
            return
        }

        // CameraSelector
        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        // Preview
        preview = Preview.Builder()
            // We request aspect ratio but no resolution
            .setTargetAspectRatio(screenAspectRatio)
            // Set initial target rotation
            .setTargetRotation(rotation)
            .build()

        // ImageCapture
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            // We request aspect ratio but no resolution to match preview config, but letting
            // CameraX optimize for whatever specific resolution best fits our use cases
            .setTargetAspectRatio(screenAspectRatio)
            // Set initial target rotation, we will have to call this again if rotation changes
            // during the lifecycle of this use case
            .setTargetRotation(rotation)
            .build()

        buildImageAnalyzer(screenAspectRatio, rotation)

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = if (imageAnalyzer == null) {
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture)
            } else {
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer)
            }

            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(fragmentCameraBinding!!.viewFinder.surfaceProvider)
            observeCameraState(camera?.cameraInfo!!)
        } catch (exc: Exception) {
            cameraActivityListener.onError(AcuantError(ErrorCodes.ERROR_UnexpectedError, ErrorDescriptions.ERROR_DESC_UnexpectedError, exc.toString()))
        }
    }

    abstract fun buildImageAnalyzer(screenAspectRatio: Int, rotation: Int)

    fun captureImage (listener: IAcuantSavedImage) {
        if (!capturing) {
            imageCapture?.let { imageCapture ->
                capturing = true

                // Create output file to hold the image (will automatically add numbers to create a uuid)
                val photoFile =
                    File.createTempFile("AcuantCameraImage", ".jpg", requireActivity().cacheDir)

                // Create output options object which contains file + metadata
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                // Setup image capture listener which is triggered after photo has been taken
                imageCapture.takePicture(
                    outputOptions,
                    cameraExecutor,
                    object : ImageCapture.OnImageSavedCallback {
                        @SuppressLint("RestrictedApi")
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {

                            var savedUri = outputFileResults.savedUri?.path ?: photoFile.absolutePath

                            val file = File(savedUri)
                            val exif = Exif.createFromFile(file)
                            val rotation = exif.rotation

                            val instream = file.inputStream()
                            var bmp: Bitmap = BitmapFactory.decodeStream(instream)


                            if (rotation != 0) {
                                val matrix = Matrix()
                                matrix.postRotate(rotation.toFloat())
                                bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                            }

                            bmp = AcuantImagePreparation.resize(bmp, 720) ?: bmp

                            instream.close()

                            var fOut: FileOutputStream? = null
                            try {
                                val newPhotoFile =
                                    File.createTempFile("AcuantCameraImage", ".jpg", requireActivity().cacheDir)
                                fOut = newPhotoFile.outputStream()
                                bmp.compress(Bitmap.CompressFormat.JPEG, 100, fOut)
                                file.delete()
                                savedUri = newPhotoFile.absolutePath
                            } catch (e1: FileNotFoundException) {
                                e1.printStackTrace()
                            } catch (e: IOException) {
                                e.printStackTrace()
                            } finally {
                                fOut?.flush()
                                fOut?.close()
                            }

                            listener.onSaved(savedUri)
                        }

                        override fun onError(exception: ImageCaptureException) {
                            listener.onError(
                                AcuantError(
                                    ErrorCodes.ERROR_SavingImage,
                                    ErrorDescriptions.ERROR_DESC_SavingImage,
                                    exception.toString()
                                )
                            )
                        }
                    })
            }
        }
    }

    private fun observeCameraState(cameraInfo: CameraInfo) {
        cameraInfo.cameraState.observe(viewLifecycleOwner) { cameraState ->
            cameraState.error?.let { error ->
                val text: String? = when (error.code) {
                    // Open errors
                    CameraState.ERROR_STREAM_CONFIG -> {
                        // Make sure to setup the use cases properly
                            "Stream config error"
                    }
                    // Opening errors
                    CameraState.ERROR_CAMERA_IN_USE -> {
                        // Close the camera or ask user to close another camera app that's using the
                        // camera
                            "Camera in use"
                    }
                    CameraState.ERROR_MAX_CAMERAS_IN_USE -> {
                        // Close another open camera in the app, or ask the user to close another
                        // camera app that's using the camera
                            "Max cameras in use"
                    }
                    // Closing errors
                    CameraState.ERROR_CAMERA_DISABLED -> {
                        // Ask the user to enable the device's cameras
                            "Camera disabled"
                    }
                    CameraState.ERROR_CAMERA_FATAL_ERROR -> {
                        // Ask the user to reboot the device to restore camera function
                            "Fatal error"
                    }
                    // Closed errors
                    CameraState.ERROR_DO_NOT_DISTURB_MODE_ENABLED -> {
                        // Ask the user to disable the "Do Not Disturb" mode, then reopen the camera
                            "Do not disturb mode enabled"
                    }
                    else -> null
                }
                cameraActivityListener.onError(AcuantError(ErrorCodes.ERROR_UnexpectedError, ErrorDescriptions.ERROR_DESC_UnexpectedError, text))
            }
        }
    }

    companion object {
        internal const val INTERNAL_OPTIONS = "options_internal"

        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0

        //we currently want all doc cameras to be in 4:3 to use as much of the available camera space as possible
        private fun aspectRatio(width: Int, height: Int): Int {
            val previewRatio = max(width, height).toDouble() / min(width, height)
            if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
                return AspectRatio.RATIO_4_3
            }
            return AspectRatio.RATIO_16_9
        }
    }
}