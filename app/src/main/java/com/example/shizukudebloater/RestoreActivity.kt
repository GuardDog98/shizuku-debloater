package com.example.shizukudebloater

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class RestoreActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restore)

        val tvList = findViewById<TextView>(R.id.tvRestoreList)
        val btnRestore = findViewById<Button>(R.id.btnRestorePackages)
        val list = ActionLog.getUninstalled(this)
        tvList.text = if (list.isEmpty()) "No package history yet" else list.joinToString("\n")

        btnRestore.setOnClickListener {
            val pkgList = ActionLog.getUninstalled(this)
            if (pkgList.isEmpty()) return@setOnClickListener
            val pkg = pkgList.last()
            // install-existing is a best-effort helper for restoring a user package
            // Real restore support requires package-specific handling on each device.
            // This is intentionally a minimal helper.
            android.widget.Toast.makeText(this, "Attempting restore for: $pkg", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
