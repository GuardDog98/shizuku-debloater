package com.example.shizukudebloater

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

object ActionLog {
    private const val PREFS = "shizuku_debloater"
    private const val KEY_UNINSTALLED = "uninstalled_packages"

    fun recordUninstall(ctx: Context, pkg: String) {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val arr = JSONArray(prefs.getString(KEY_UNINSTALLED, "[]"))
        arr.put(pkg)
        prefs.edit().putString(KEY_UNINSTALLED, arr.toString()).apply()
    }

    fun getUninstalled(ctx: Context): List<String> {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_UNINSTALLED, "[]") ?: "[]"
        val arr = JSONArray(raw)
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) list.add(arr.optString(i))
        return list
    }

    fun clear(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_UNINSTALLED).apply()
    }
}
