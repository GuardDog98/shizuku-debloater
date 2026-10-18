package com.example.shizukudebloater

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val etAllow = findViewById<EditText>(R.id.etAllowList)
        val etIgnore = findViewById<EditText>(R.id.etIgnoreList)
        val btnSave = findViewById<Button>(R.id.btnSaveLists)

        etAllow.setText(UserPreferences.loadAllowList(this).joinToString("\n"))
        etIgnore.setText(UserPreferences.loadIgnoreList(this).joinToString("\n"))

        btnSave.setOnClickListener {
            val allow = etAllow.text.toString().lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toSet()
            val ignore = etIgnore.text.toString().lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toSet()
            UserPreferences.saveAllowList(this, allow)
            UserPreferences.saveIgnoreList(this, ignore)
            finish()
        }
    }
}
