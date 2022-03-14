package com.acuant.acuantcamera.camera

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.core.impl.utils.Exif
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import com.acuant.acuantcamera.R
import com.acuant.acuantcamera.databinding.FragmentCameraBinding
import com.acuant.acuantcamera.interfaces.IAcuantSavedImage
import com.acuant.acuantcamera.interfaces.ICameraActivityFinish
import com.acuant.acuantcamera.overlay.BaseRectangleView
import com.acuant.acuantcommon.model.AcuantError
import com.acuant.acuantcommon.model.ErrorCodes
import com.acuant.acuantcommon.model.ErrorDescriptions
import com.acuant.acuantimagepreparation.AcuantImagePreparation
import com.google.common.util.concurrent.ListenableFuture
import org.json.JSONObject
import java.io.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


abstract class AcuantBaseCameraFragment: Fragment() {

    private var displayId: Int = -1
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var orientationEventListener: OrientationEventListener? = null
    private var preview: Preview? = null
    private var failedToFocus: Boolean = false
    protected var imageCapture: ImageCapture? = null
    protected var capturing: Boolean = false
    protected var fragmentCameraBinding: FragmentCameraBinding? = null
    protected var imageAnalyzer: ImageAnalysis? = null //set up by implementations
    protected lateinit var cameraExecutor: ExecutorService
    protected lateinit var cameraActivityListener: ICameraActivityFinish
    protected lateinit var acuantOptions: AcuantCameraOptions

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
        fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false)
        return fragmentCameraBinding!!.root
    }

    override fun onStart() {
        super.onStart()
        orientationEventListener?.enable()
    }

    override fun onStop() {
        super.onStop()
        orientationEventListener?.disable()
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraActivityListener = requireActivity() as ICameraActivityFinish

        val opts = requireArguments().getSerializable(INTERNAL_OPTIONS) as AcuantCameraOptions?

        if (opts != null) {
            acuantOptions = opts
        } else {
            cameraActivityListener.onError(AcuantError(ErrorCodes.ERROR_UnexpectedError, ErrorDescriptions.ERROR_DESC_UnexpectedError, "Options were unexpectedly null"))
            return
        }

        orientationEventListener = object : OrientationEventListener(requireContext()) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) {
                    return
                }

                when (orientation) {
                    in 45 until 135 -> {
                        val rotation = Surface.ROTATION_270
                        imageAnalyzer?.targetRotation = rotation
                        imageCapture?.targetRotation = rotation
                        rotateUi(270)
                    }
                    in 225 until 315 -> {
                        val rotation = Surface.ROTATION_90
                        imageAnalyzer?.targetRotation = rotation
                        imageCapture?.targetRotation = rotation
                        rotateUi(90)
                    }
                }
            }
        }

        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Wait for the views to be properly laid out
        fragmentCameraBinding?.viewFinder?.post {
            val binding = fragmentCameraBinding

            if (binding != null) {
                if (acuantOptions.zoomType == AcuantCameraOptions.ZoomType.IdOnly) {
                    (binding.viewFinder.layoutParams as ConstraintLayout.LayoutParams?)?.dimensionRatio = ""
                }
                // Keep track of the display in which this view is attached
                displayId = binding.viewFinder.display.displayId

                requestCameraPermissionIfNeeded()
            }
        }
    }

    abstract fun rotateUi(rotation: Int)

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

            // Select lensFacing depending on the available cameras
            lensFacing = when {
                hasBackCamera() -> CameraSelector.LENS_FACING_BACK
                else -> {
                    cameraActivityListener.onError(AcuantError(ErrorCodes.ERROR_UnexpectedError, ErrorDescriptions.ERROR_DESC_UnexpectedError, "Phone does not have a camera/app can not see the camera"))
                    return@addListener
                }
            }
            // Build and bind the camera use cases
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun hasBackCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
    }

    /** Declare and bind preview, capture and analysis use cases */
    private fun bindCameraUseCases() {

        val screenAspectRatio = aspectRatio()

        var rotation = fragmentCameraBinding!!.viewFinder.display.rotation

        // CameraProvider
        val cameraProvider = cameraProvider

        if (cameraProvider == null) {
            cameraActivityListener.onError(AcuantError(ErrorCodes.ERROR_UnexpectedError, ErrorDescriptions.ERROR_DESC_UnexpectedError, "Camera initialization failed."))
            return
        }

        // CameraSelector
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        // Preview
        preview = Preview.Builder()
            // We request aspect ratio but no resolution
            .setTargetAspectRatio(screenAspectRatio)
            // Set initial target rotation
            .setTargetRotation(rotation)
            .build()

        rotation = Surface.ROTATION_90

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

        val trueScreenRatio = fragmentCameraBinding!!.viewFinder.height / fragmentCameraBinding!!.viewFinder.width.toFloat()

        buildImageAnalyzer(screenAspectRatio, trueScreenRatio, rotation)

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            val useCaseGroupBuilder = UseCaseGroup.Builder().addUseCase(preview!!).addUseCase(imageCapture!!)

            if (imageAnalyzer != null) {
                useCaseGroupBuilder.addUseCase(imageAnalyzer!!)
            }

            camera = cameraProvider.bindToLifecycle(this, cameraSelector, useCaseGroupBuilder.build())

            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(fragmentCameraBinding!!.viewFinder.surfaceProvider)
            observeCameraState(camera?.cameraInfo!!)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    abstract fun buildImageAnalyzer(screenAspectRatio: Int, trueScreenRatio: Float, rotation: Int)

    abstract fun resetWorkflow()

    fun captureImage (listener: IAcuantSavedImage, middle: Point? = null, captureType: String? = null) {
        if (!capturing) {
            capturing = true
            if (!failedToFocus) {
                val width = fragmentCameraBinding?.root?.width?.toFloat()
                val height = fragmentCameraBinding?.root?.height?.toFloat()
                val action: FocusMeteringAction =
                    if (middle == null || width == null || height == null || middle.x == 0) {
                        val factory = SurfaceOrientedMeteringPointFactory(1f, 1f)
                        val point: MeteringPoint = factory.createPoint(0.5f, 0.5f)
                        FocusMeteringAction.Builder(point).build()
                    } else {
                        val factory = SurfaceOrientedMeteringPointFactory(width, height)
                        val point: MeteringPoint =
                            factory.createPoint(middle.x.toFloat(), middle.y.toFloat())
                        FocusMeteringAction.Builder(point).build()
                    }
                val future: ListenableFuture<FocusMeteringResult>? =
                    camera?.cameraControl?.startFocusAndMetering(action)
                if (future != null) {
                    future.addListener({
                        try {
                            val result = future.get()
                            if (result.isFocusSuccessful) {
                                performCapture(listener, captureType)
                            } else {
                                failedToFocus = true
                                capturing = false
                                resetWorkflow()
                            }
                        } catch (e: Exception) {
                            failedToFocus = true
                            capturing = false
                            resetWorkflow()
                        }
                    }, ContextCompat.getMainExecutor(requireContext()))
                } else {
                    listener.onError(
                        AcuantError(
                            ErrorCodes.ERROR_UnexpectedError,
                            ErrorDescriptions.ERROR_DESC_UnexpectedError,
                            "ListenableFuture<FocusMeteringResult> was null, which likely means camera was null. This should not happen."
                        )
                    )
                }
            } else {
                performCapture(listener, captureType)
            }
        }
    }

    private fun performCapture(listener: IAcuantSavedImage, captureType: String?) {

        imageCapture?.let { imageCapture ->

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

                        val savedUri = outputFileResults.savedUri?.path ?: photoFile.absolutePath

                        val exif = Exif.createFromFileString(savedUri)
                        val rotation = exif.rotation

                        if (captureType != null) {
                            addExif(File(savedUri), captureType, rotation)
                        } else {
                            addExif(File(savedUri), "NOT SPECIFIED (implementer used deprecated constructor that lacks this data)", rotation)
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

    protected fun setOptions(rectangleView: BaseRectangleView?) {
        if (rectangleView == null)
            return
        rectangleView.allowBox = acuantOptions.allowBox
        rectangleView.bracketLengthInHorizontal = acuantOptions.bracketLengthInHorizontal
        rectangleView.bracketLengthInVertical = acuantOptions.bracketLengthInVertical
        rectangleView.defaultBracketMarginHeight = acuantOptions.defaultBracketMarginHeight
        rectangleView.defaultBracketMarginWidth = acuantOptions.defaultBracketMarginWidth
        rectangleView.paintColorCapturing = acuantOptions.colorCapturing
        rectangleView.paintColorHold = acuantOptions.colorHold
        rectangleView.paintColorBracketAlign = acuantOptions.colorBracketAlign
        rectangleView.paintColorBracketCapturing = acuantOptions.colorBracketCapturing
        rectangleView.paintColorBracketCloser = acuantOptions.colorBracketCloser
        rectangleView.paintColorBracketHold = acuantOptions.colorBracketHold
        rectangleView.cardRatio = acuantOptions.cardRatio
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

    //extension credit to: https://stackoverflow.com/questions/1967039/onclicklistener-x-y-location-of-event
    @SuppressLint("ClickableViewAccessibility")
    protected fun View.setOnClickListenerWithPoint(action: (Point) -> Unit) {
        val coordinates = Point()
        val screenPosition = IntArray(2)
        setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                v.getLocationOnScreen(screenPosition)
                coordinates.set(event.x.toInt() + screenPosition[0], event.y.toInt() + screenPosition[1])
            }
//            v.performClick()
            false
        }
        setOnClickListener {
            action.invoke(coordinates)
        }
    }

    companion object {
        private const val TAG = "Acuant Camera"
        internal const val INTERNAL_OPTIONS = "options_internal"

        private fun addExif(file: File, captureType: String, rotation: Int) {
            val exif = ExifInterface(file.absolutePath)
            val json = JSONObject()


            json.put(AcuantImagePreparation.CAPTURE_TYPE_TAG, captureType)
            json.put(AcuantImagePreparation.ROTATION_TAG, rotation)

            when (rotation) {
                270 ->  exif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_270.toString())
                else -> exif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            }

            exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, json.toString())
            exif.saveAttributes()
        }

        //we currently want all doc cameras to be in 4:3 to use as much of the available camera space as possible
        private fun aspectRatio(): Int {
                return AspectRatio.RATIO_4_3
        }
    }
}