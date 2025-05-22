package com.pplm.projectinventarisuas.utils.components

import android.app.AlertDialog
import android.app.Dialog // Import Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity // Import Gravity
import android.view.LayoutInflater
import android.view.View // Import View
import android.view.ViewGroup // Import ViewGroup for LayoutParams
import android.view.Window // Import Window
import android.widget.Button
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

        loadingDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        loadingDialog?.window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        loadingDialog?.window?.setGravity(Gravity.CENTER)

        loadingDialog?.show()
    }

    fun dismissLoading() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    fun success(
        context: Context,
        title: String = "Sukses",
        message: String,
        onDismiss: (() -> Unit)? = null
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_succes, null)
        view.findViewById<TextView>(R.id.tvTitle).text = title
        view.findViewById<TextView>(R.id.tvMessage).text = message

        val dialog = AlertDialog.Builder(context)
            .setView(view)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setGravity(Gravity.CENTER)

        view.findViewById<Button>(R.id.btnOk).setOnClickListener {
            dialog.dismiss()
            onDismiss?.invoke()
        }

        dialog.setOnDismissListener {
            onDismiss?.invoke()
        }

        dialog.show()
    }

    fun alert(
        context: Context,
        title: String = "Peringatan",
        message: String,
        onDismiss: (() -> Unit)? = null
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_alert, null)
        view.findViewById<TextView>(R.id.tvTitle).text = title
        view.findViewById<TextView>(R.id.tvMessage).text = message

        val dialog = AlertDialog.Builder(context)
            .setView(view)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setGravity(Gravity.CENTER)

        view.findViewById<Button>(R.id.btnOk).setOnClickListener {
            dialog.dismiss()
            onDismiss?.invoke()
        }

        dialog.setOnDismissListener {
            onDismiss?.invoke()
        }

        dialog.show()
    }

    fun confirm(
        context: Context,
        title: String = "Konfirmasi",
        message: String,
        onConfirm: () -> Unit,
        onCancel: (() -> Unit)? = null,
        onDismiss: (() -> Unit)? = null
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_confirm, null)
        view.findViewById<TextView>(R.id.tvTitle).text = title
        view.findViewById<TextView>(R.id.tvMessage).text = message

        val dialog = AlertDialog.Builder(context)
            .setView(view)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setGravity(Gravity.CENTER)

        view.findViewById<Button>(R.id.btnYes).setOnClickListener {
            dialog.dismiss()
            onConfirm()
        }

        view.findViewById<Button>(R.id.btnNo).setOnClickListener {
            dialog.dismiss()
            onCancel?.invoke()
        }

        dialog.setOnDismissListener {
            onDismiss?.invoke()
        }

        dialog.show()
    }

    fun options(
        context: Context,
        title: String = "Pilih Opsi!",
        onView: () -> Unit,
        onEdit: () -> Unit,
        onDelete: () -> Unit,
        onDismiss: (() -> Unit)? = null
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

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setGravity(Gravity.CENTER)

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

        dialog.setOnDismissListener {
            onDismiss?.invoke()
        }

        dialog.show()
    }
}