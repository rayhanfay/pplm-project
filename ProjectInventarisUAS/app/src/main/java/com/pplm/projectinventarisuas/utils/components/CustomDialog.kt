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
        title: String,
        options: List<String>,
        onSelect: (Int) -> Unit
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_options, null)
        view.findViewById<TextView>(R.id.tvTitle).text = title

        val listView = view.findViewById<ListView>(R.id.listOptions)
        val adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, options)
        listView.adapter = adapter

        val dialog = AlertDialog.Builder(context)
            .setView(view)
            .setCancelable(true)
            .create()

        listView.setOnItemClickListener { _, _, position, _ ->
            dialog.dismiss()
            onSelect(position)
        }

        dialog.show()
    }
}
