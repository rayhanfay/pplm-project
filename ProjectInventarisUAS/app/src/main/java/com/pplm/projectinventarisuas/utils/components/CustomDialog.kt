package com.pplm.projectinventarisuas.utils.components

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import com.pplm.projectinventarisuas.R

object CustomDialog {

    private var loadingDialog: AlertDialog? = null

    fun showLoading(context: Context, message: String = "Memproses...") {
        if (loadingDialog?.isShowing == true) return

        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_loading, null)
        view.findViewById<TextView>(R.id.tvMessage).text = message

        loadingDialog = AlertDialog.Builder(context)
            .setView(view)
            .setCancelable(false)
            .create()
        loadingDialog?.show()
    }

    fun dismissLoading() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    fun success(context: Context, message: String, onDismiss: (() -> Unit)? = null) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_succes, null)
        view.findViewById<TextView>(R.id.tvMessage).text = message

        val dialog = AlertDialog.Builder(context)
            .setView(view)
            .setCancelable(false)
            .create()

        view.findViewById<Button>(R.id.btnOk).setOnClickListener {
            dialog.dismiss()
            onDismiss?.invoke()
        }

        dialog.show()
    }

    fun alert(context: Context, message: String, onDismiss: (() -> Unit)? = null) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_alert, null)
        view.findViewById<TextView>(R.id.tvMessage).text = message

        val dialog = AlertDialog.Builder(context)
            .setView(view)
            .setCancelable(false)
            .create()

        view.findViewById<Button>(R.id.btnOk).setOnClickListener {
            dialog.dismiss()
            onDismiss?.invoke()
        }

        dialog.show()
    }

    fun confirm(
        context: Context,
        message: String,
        onConfirm: () -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_confirm, null)
        view.findViewById<TextView>(R.id.tvMessage).text = message

        val dialog = AlertDialog.Builder(context)
            .setView(view)
            .setCancelable(false)
            .create()

        view.findViewById<Button>(R.id.btnYes).setOnClickListener {
            dialog.dismiss()
            onConfirm()
        }

        view.findViewById<Button>(R.id.btnNo).setOnClickListener {
            dialog.dismiss()
            onCancel?.invoke()
        }

        dialog.show()
    }

    fun options(
        context: Context,
        title: String = "Pilih Opsi!",
        onView: () -> Unit,
        onEdit: () -> Unit,
        onDelete: () -> Unit
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_options, null)
        view.findViewById<TextView>(R.id.tvOption).text = title

        val btnView = view.findViewById<Button>(R.id.btnView)
        val btnEdit = view.findViewById<Button>(R.id.btnEdit)
        val btnDelete = view.findViewById<Button>(R.id.btnDelet)

        val dialog = AlertDialog.Builder(context)
            .setView(view)
            .setCancelable(true)
            .create()

        btnView.setOnClickListener {
            dialog.dismiss()
            onView()
        }

        btnEdit.setOnClickListener {
            dialog.dismiss()
            onEdit()
        }

        btnDelete.setOnClickListener {
            dialog.dismiss()
            onDelete()
        }

        dialog.show()
    }
}