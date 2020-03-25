package com.acuant.sampleapp.utils

import android.app.Dialog
import android.content.DialogInterface
import android.util.Log

import android.content.ContentValues.TAG
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity

/**
 * Created by tapasbehera on 4/16/18.
 */

object DialogUtils {

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

    fun showDialog(context: AppCompatActivity, message: String): AlertDialog {

        val clickListener = DialogInterface.OnClickListener { dialog, which ->
            dismissDialog(dialog as Dialog)
        }
        return showDialog(context, message, clickListener)
    }

    fun showDialog(context: AppCompatActivity, message: String, clickListener: DialogInterface.OnClickListener): AlertDialog {
        val alertDialog = AlertDialog.Builder(context).create()
        alertDialog.setMessage(message)
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Ok", clickListener)
        alertDialog.setCanceledOnTouchOutside(false)
        alertDialog.show()

        return alertDialog
    }

    fun showDialog(context: AppCompatActivity, message: String, okListener: DialogInterface.OnClickListener, noListener: DialogInterface.OnClickListener): AlertDialog {
        val alertDialog = AlertDialog.Builder(context).create()
        alertDialog.setMessage(message)
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Ok", okListener)
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No", noListener)
        alertDialog.setCanceledOnTouchOutside(false)
        alertDialog.show()

        return alertDialog
    }

}
