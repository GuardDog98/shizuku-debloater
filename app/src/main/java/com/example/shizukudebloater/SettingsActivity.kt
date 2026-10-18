package com.example.shizukudebloater

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val tvModeInfo = findViewById<TextView>(R.id.tvModeInfo)
        val etAllow = findViewById<EditText>(R.id.etAllowList)
        val etIgnore = findViewById<EditText>(R.id.etIgnoreList)
        val btnSave = findViewById<Button>(R.id.btnSaveLists)
        val btnExport = findViewById<Button>(R.id.btnExport)
        val btnImport = findViewById<Button>(R.id.btnImport)

        tvModeInfo.text = "Current mode: ${if (UserPreferences.isExpertMode(this)) "Expert mode" else "Safe mode"}"
        etAllow.setText(UserPreferences.loadAllowList(this).joinToString("\n"))
        etIgnore.setText(UserPreferences.loadIgnoreList(this).joinToString("\n"))

        btnSave.setOnClickListener {
            val allow = etAllow.text.toString().lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toSet()
            val ignore = etIgnore.text.toString().lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toSet()
            UserPreferences.saveAllowList(this, allow)
            UserPreferences.saveIgnoreList(this, ignore)
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
            finish()
        }

        btnExport.setOnClickListener {
            // intentionally compact export for easy copy/paste
            val payload = "{\n  \"allow\": ${UserPreferences.loadAllowList(this).toList()},\n  \"ignore\": ${UserPreferences.loadIgnoreList(this).toList()}\n}"
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("shizuku_lists", payload))
            AlertDialog.Builder(this)
                .setTitle("Exported lists")
                .setMessage(payload)
                .setPositiveButton("OK", null)
                .show()
        }

        btnImport.setOnClickListener {
            val input = EditText(this)
            input.hint = "Paste JSON here"
            AlertDialog.Builder(this)
                .setTitle("Import lists")
                .setView(input)
                .setPositiveButton("Import") { _, _ ->
                    val raw = input.text.toString()
                    try {
                        val obj = org.json.JSONObject(raw)
                        val allow = mutableSetOf<String>()
                        val ignore = mutableSetOf<String>()
                        val allowArr = obj.optJSONArray("allow")
                        val ignoreArr = obj.optJSONArray("ignore")
                        if (allowArr != null) for (i in 0 until allowArr.length()) allow.add(allowArr.optString(i))
                        if (ignoreArr != null) for (i in 0 until ignoreArr.length()) ignore.add(ignoreArr.optString(i))
                        UserPreferences.saveAllowList(this, allow)
                        UserPreferences.saveIgnoreList(this, ignore)
                        Toast.makeText(this, "Imported", Toast.LENGTH_SHORT).show()
                        finish()
                    } catch (e: Exception) {
                        Toast.makeText(this, "Invalid JSON", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
