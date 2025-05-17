package com.pplm.projectinventarisuas.utils.components

import android.app.AlertDialog
import android.content.Context
import android.util.Log

object CustomDialog {

    private var loadingDialog: AlertDialog? = null

    fun showLoading(context: Context, message: String = "Memproses...") {
        if (loadingDialog?.isShowing == true) return

        loadingDialog = AlertDialog.Builder(context)
            .setCancelable(false)
            .setMessage(message)
            .create()
        loadingDialog?.show()
    }

    fun dismissLoading() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    fun alert(context: Context, message: String, onDismiss: (() -> Unit)? = null) {
        val dialog = AlertDialog.Builder(context)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("OK") { dialogInterface, _ ->
                dialogInterface.dismiss()
                onDismiss?.invoke()
            }
            .create()
        dialog.show()
        Log.d("CustomDialog", "Dialog displayed")
    }

    fun confirm(
        context: Context,
        message: String,
        onConfirm: () -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        AlertDialog.Builder(context)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("Ya") { dialog, _ ->
                dialog.dismiss()
                onConfirm()
            }
            .setNegativeButton("Tidak") { dialog, _ ->
                dialog.dismiss()
                onCancel?.invoke()
            }
            .create()
            .show()
    }

    fun options(
        context: Context,
        title: String,
        options: List<String>,
        onSelect: (Int) -> Unit
    ) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setItems(options.toTypedArray()) { _, which ->
                onSelect(which)
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
