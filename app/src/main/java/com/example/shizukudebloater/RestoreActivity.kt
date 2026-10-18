package com.example.shizukudebloater

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RestoreActivity : AppCompatActivity() {
    private val scope = CoroutineScope(Dispatchers.Main)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restore)

        val lv = findViewById<ListView>(R.id.lvRestore)
        val tvHint = findViewById<TextView>(R.id.tvHint)
        val etManual = findViewById<EditText>(R.id.etManualPkg)
        val btnRestore = findViewById<Button>(R.id.btnRestorePackages)

        val list = ActionLog.getUninstalled(this).distinct().reversed()
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_single_choice, list)
        lv.adapter = adapter
        if (list.isNotEmpty()) lv.setItemChecked(0, true)

        btnRestore.setOnClickListener {
            val selected = if (lv.checkedItemPosition != AdapterView.INVALID_POSITION) list[lv.checkedItemPosition] else null
            val manual = etManual.text.toString().trim().takeIf { it.isNotEmpty() }
            val pkg = manual ?: selected
            if (pkg.isNullOrEmpty()) {
                Toast.makeText(this, "Select or type a package name to restore", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(this)
                .setTitle("Restore $pkg?")
                .setMessage("This will attempt to run: cmd package install-existing $pkg\nThis works only if the APK still exists in a system image or package cache. Results vary by device.")
                .setPositiveButton("Restore") { _, _ ->
                    scope.launch {
                        val out = withContext(Dispatchers.IO) { ShellUtils.runShizukuShell("cmd package install-existing $pkg") }
                        AlertDialog.Builder(this@RestoreActivity)
                            .setTitle("Restore result")
                            .setMessage(out)
                            .setPositiveButton("OK", null)
                            .show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
