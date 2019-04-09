package com.acuant.sampleapp.utils

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.util.Log

import android.content.ContentValues.TAG

/**
 * Created by tapasbehera on 4/16/18.
 */

object DialogUtils {

    /**
     * @param context
     * @param message
     * @return
     */

    fun dismissDialog(dialog: Dialog?) {
        if (dialog != null && dialog.isShowing) {
            try {
                dialog.setCancelable(true)
                dialog.dismiss()
            } catch (e: IllegalArgumentException) // even sometimes happens?: http://stackoverflow.com/questions/12533677/illegalargumentexception-when-dismissing-dialog-in-async-task
            {
                Log.i(TAG, "Error when attempting to dismiss dialog, it is an android problem.", e)
            }

        }
    }

    fun showDialog(context: Activity, message: String): AlertDialog {

        val clickListener = DialogInterface.OnClickListener { dialog, which ->
            dismissDialog(dialog as Dialog)
        }
        return showDialog(context, message, clickListener)
    }

    /**
     * @param context
     * @param message
     * @param clickListener
     * @return
     */
    fun showDialog(context: Activity, message: String, clickListener: DialogInterface.OnClickListener): AlertDialog {
        val alertDialog = AlertDialog.Builder(context).create()
        alertDialog.setMessage(message)
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Ok", clickListener)
        alertDialog.setCanceledOnTouchOutside(false)
        alertDialog.show()

        return alertDialog
    }

    /**
     * @param context
     * @param message
     * @param okListener
     * @param noListener
     * @return
     */
    fun showDialog(context: Activity, message: String, okListener: DialogInterface.OnClickListener, noListener: DialogInterface.OnClickListener): AlertDialog {
        val alertDialog = AlertDialog.Builder(context).create()
        alertDialog.setMessage(message)
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Ok", okListener)
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No", noListener)
        alertDialog.setCanceledOnTouchOutside(false)
        alertDialog.show()

        return alertDialog
    }

    /**
     * @param context
     * @param message
     * @return
     */
    fun showProgessDialog(context: Activity, message: String): ProgressDialog {
        val progressDialog = ProgressDialog(context)
        progressDialog.setMessage(message)
        progressDialog.isIndeterminate = true
        progressDialog.setCancelable(false)
        progressDialog.show()

        return progressDialog
    }

}
