package com.example.shizukudebloater

import android.content.Context
import androidx.appcompat.app.AlertDialog

object UIUtils {
    fun showInfo(ctx: Context, title: String, message: String) {
        AlertDialog.Builder(ctx).setTitle(title).setMessage(message).setPositiveButton("OK", null).show()
    }

    fun showConfirm(ctx: Context, title: String, message: String, onYes: () -> Unit) {
        AlertDialog.Builder(ctx).setTitle(title).setMessage(message).setPositiveButton("Yes") { _, _ -> onYes() }.setNegativeButton("No", null).show()
    }
}
