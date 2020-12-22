/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.acuant.acuantcamera.camera.document.cameraone


import android.Manifest
import android.content.Context
import android.content.res.Configuration
import android.support.annotation.RequiresPermission
import android.support.v7.app.AlertDialog
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.acuant.acuantcamera.R
import java.io.IOException

class DocumentCameraSourcePreview(private val mContext: Context, attrs: AttributeSet?) : ViewGroup(mContext, attrs) {
    var mSurfaceView: SurfaceView
    var pointXOffset: Int = 0
    var pointYOffset: Int = 0
    private var mStartRequested: Boolean = false
    private var mSurfaceAvailable: Boolean = false
    private var documentCameraSource: DocumentCameraSource? = null

    private val isPortraitMode: Boolean
        get() {
            val orientation = mContext.resources.configuration.orientation
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                return false
            }
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                return true
            }

            Log.d(TAG, "isPortraitMode returning false by default")
            return false
        }

    init {
        mStartRequested = false
        mSurfaceAvailable = false

        val previewParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT)
        previewParams.addRule(RelativeLayout.CENTER_IN_PARENT)
        mSurfaceView = SurfaceView(mContext)
        mSurfaceView.layoutParams = previewParams
        mSurfaceView.holder.addCallback(SurfaceCallback())
        addView(mSurfaceView)
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    @Throws(IOException::class, SecurityException::class)
    fun start(documentCameraSource: DocumentCameraSource?) {
        if (documentCameraSource == null) {
            stop()
        }

        this.documentCameraSource = documentCameraSource

        if (this.documentCameraSource != null) {
            mStartRequested = true
            startIfReady()
        }
    }

    fun stop() {
        if (documentCameraSource != null) {
            documentCameraSource!!.stop()
        }
    }

    fun release() {
        if (documentCameraSource != null) {
            documentCameraSource!!.release()
            documentCameraSource = null
        }
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    @Throws(IOException::class, SecurityException::class)
    private fun startIfReady() {
        if (mStartRequested && mSurfaceAvailable) {
            documentCameraSource!!.start(mSurfaceView.holder, this)
            mStartRequested = false
        }
    }

    private inner class SurfaceCallback : SurfaceHolder.Callback {

        override fun surfaceCreated(surface: SurfaceHolder) {
            mSurfaceAvailable = true
            try {
                startIfReady()
            } catch (se: SecurityException) {
                Log.e(TAG, "Do not have permission to start the camera", se)
            } catch (e: IOException) {
                Log.e(TAG, "Could not start camera source.", e)
                val dialogBuilder = AlertDialog.Builder(context)
                dialogBuilder.setMessage(R.string.error_starting_cam)
                        .setPositiveButton(R.string.ok
                        ) { dialog, _ ->
                            dialog.dismiss()

                        }
                dialogBuilder.create().show()
            }

        }

        override fun surfaceDestroyed(surface: SurfaceHolder) {
            mSurfaceAvailable = false
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {

        val viewWidth = right - left
        val viewHeight = bottom - top

        var previewWidth = viewWidth
        var previewHeight = viewHeight
        if (documentCameraSource != null) {
            val size = documentCameraSource?.previewSize
            if (size != null) {
                previewWidth = size.width
                previewHeight = size.height
            }
        }

        // Swap width and height sizes when in portrait, since it will be rotated 90 degrees
        if (isPortraitMode) {
            val tmp = previewWidth
            previewWidth = previewHeight
            previewHeight = tmp
        }
        val childWidth: Int
        val childHeight: Int
        val childXOffset: Int
        val childYOffset: Int
        val widthRatio = viewWidth.toFloat() / previewWidth.toFloat()
        val heightRatio = viewHeight.toFloat() / previewHeight.toFloat()

        if (widthRatio < heightRatio) {
            childWidth = viewWidth
            childHeight = (previewHeight.toFloat() * widthRatio).toInt()
        } else {
            childWidth = (previewWidth.toFloat() * heightRatio).toInt()
            childHeight = viewHeight
        }

        childXOffset = (childWidth - viewWidth) / 2
        childYOffset = (childHeight - viewHeight) / 2

        pointXOffset = childXOffset
        pointYOffset = childYOffset

        for (i in 0 until childCount) {
            getChildAt(i).layout(
                    -1 * childXOffset, -1 * childYOffset,
                    childWidth - childXOffset, childHeight - childYOffset)
            getChildAt(i).requestLayout()
        }
        try {
            startIfReady()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (se: SecurityException) {
            se.printStackTrace()
        }
    }

    companion object {
        private const val TAG = "CameraSourcePreview"
    }
}
