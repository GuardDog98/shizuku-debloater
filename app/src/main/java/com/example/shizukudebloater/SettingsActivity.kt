package com.example.shizukudebloater

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val etAllow = findViewById<EditText>(R.id.etAllowList)
        val etIgnore = findViewById<EditText>(R.id.etIgnoreList)
        val btnSave = findViewById<Button>(R.id.btnSaveLists)
        val btnExport = findViewById<Button>(R.id.btnExport)
        val btnImport = findViewById<Button>(R.id.btnImport)

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
            val json = JSONObject()
            json.put("allow", UserPreferences.loadAllowList(this).toTypedArray())
            json.put("ignore", UserPreferences.loadIgnoreList(this).toTypedArray())
            val txt = json.toString(2)

            // Copy to clipboard + show in dialog for convenience
            val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cb.setPrimaryClip(ClipData.newPlainText("shizuku-debloater-lists", txt))

            AlertDialog.Builder(this)
                .setTitle("Exported lists (copied to clipboard)")
                .setMessage(txt)
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
                    val s = input.text.toString()
                    try {
                        val obj = JSONObject(s)
                        val allowArr = obj.optJSONArray("allow")
                        val ignoreArr = obj.optJSONArray("ignore")
                        val allow = mutableSetOf<String>()
                        val ignore = mutableSetOf<String>()
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
