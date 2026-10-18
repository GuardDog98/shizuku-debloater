package com.example.shizukudebloater

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray

object UserPreferences {
    private const val PREFS = "shizuku_debloater"
    private const val KEY_ALLOW = "allow_list"
    private const val KEY_IGNORE = "ignore_list"
    private const val KEY_EXPERT = "expert_mode"

    fun saveAllowList(ctx: Context, packages: Set<String>) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_ALLOW, packages.joinToString("\n")).apply()
    }

    fun loadAllowList(ctx: Context): Set<String> {
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_ALLOW, "") ?: ""
        return raw.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    fun saveIgnoreList(ctx: Context, packages: Set<String>) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_IGNORE, packages.joinToString("\n")).apply()
    }

    fun loadIgnoreList(ctx: Context): Set<String> {
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_IGNORE, "") ?: ""
        return raw.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    fun setExpertMode(ctx: Context, enabled: Boolean) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_EXPERT, enabled).apply()
    }

    fun isExpertMode(ctx: Context): Boolean {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_EXPERT, false)
    }
}

object ActionLog {
    private const val PREFS = "shizuku_debloater"
    private const val KEY_UNINSTALLED = "uninstalled_packages"

    fun recordUninstall(ctx: Context, packageName: String) {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val arr = JSONArray(prefs.getString(KEY_UNINSTALLED, "[]") ?: "[]")
        arr.put(packageName)
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

data class AppEntry(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    val apkPath: String? = null
)

enum class PackageAction { DISABLE, UNINSTALL, FORCE }
