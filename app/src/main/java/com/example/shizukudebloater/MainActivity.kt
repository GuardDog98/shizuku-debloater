package com.example.shizukudebloater

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import moe.shizuku.api.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var rvPackages: RecyclerView
    private lateinit var btnRefresh: Button
    private lateinit var switchExpert: Switch
    private val adapter = PackageAdapter { pkg, action -> onPackageAction(pkg, action) }
    private val prefs by lazy { getSharedPreferences("shizuku_debloater", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rvPackages = findViewById(R.id.rvPackages)
        btnRefresh = findViewById(R.id.btnRefresh)
        switchExpert = findViewById(R.id.switchExpert)

        rvPackages.layoutManager = LinearLayoutManager(this)
        rvPackages.adapter = adapter

        switchExpert.isChecked = prefs.getBoolean("expert_mode", false)
        switchExpert.setOnCheckedChangeListener { _, isChecked -> prefs.edit().putBoolean("expert_mode", isChecked).apply() }

        btnRefresh.setOnClickListener { checkPermissionAndScan() }

        if (!Shizuku.pingBinder()) {
            Toast.makeText(this, "Shizuku not running — install and start Shizuku Manager", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkPermissionAndScan() {
        if (!Shizuku.pingBinder()) {
            Toast.makeText(this, "Shizuku not running", Toast.LENGTH_SHORT).show()
            return
        }

        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(123)
            Toast.makeText(this, "Requested Shizuku permission. Please grant and retry.", Toast.LENGTH_LONG).show()
            return
        }

        scanPackages()
    }

    private fun scanPackages() {
        scope.launch {
            val out = withContext(Dispatchers.IO) { runShizukuShell("pm list packages --user 0") }
            val pkgNames = out.lines()
                .mapNotNull { it.trim().takeIf { it.isNotEmpty() }?.removePrefix("package:") }
                .distinct()

            val entries = mutableListOf<AppEntry>()
            val pm = packageManager
            for (pkg in pkgNames) {
                try {
                    val ai = pm.getApplicationInfo(pkg, PackageManager.GET_META_DATA)
                    val label = pm.getApplicationLabel(ai).toString()
                    val isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0 || (ai.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                    entries.add(AppEntry(pkg, label, isSystem))
                } catch (e: Exception) {
                    entries.add(AppEntry(pkg, pkg, false))
                }
            }

            adapter.submitList(entries.sortedWith(compareBy({ !it.isSystem }, { it.label.toLowerCase() })))
            Toast.makeText(this@MainActivity, "Found ${entries.size} packages", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onPackageAction(entry: AppEntry, action: PackageAction) {
        val expert = prefs.getBoolean("expert_mode", false)
        if (!expert && entry.isSystem) {
            AlertDialog.Builder(this)
                .setTitle("System app")
                .setMessage("${entry.label} appears to be a system app. In non-expert mode we recommend only disabling. Enable expert mode to allow uninstall. Continue?")
                .setPositiveButton("Continue") { _, _ -> performAction(entry, action) }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }
        performAction(entry, action)
    }

    private fun performAction(entry: AppEntry, action: PackageAction) {
        val cmd = when (action) {
            PackageAction.DISABLE -> "pm disable-user --user 0 ${entry.packageName}"
            PackageAction.UNINSTALL -> "pm uninstall --user 0 ${entry.packageName}"
        }

        AlertDialog.Builder(this)
            .setTitle("Confirm")
            .setMessage("This will run: $cmd\nProceed?")
            .setPositiveButton("Yes") { _, _ ->
                scope.launch {
                    val out = withContext(Dispatchers.IO) { runShizukuShell(cmd) }
                    if (action == PackageAction.UNINSTALL) ActionLog.recordUninstall(this@MainActivity, entry.packageName)
                    Toast.makeText(this@MainActivity, out.takeIf { it.isNotBlank() } ?: "Done", Toast.LENGTH_LONG).show()
                    scanPackages()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Allows restoring previously-uninstalled packages (install-existing)
    private fun restorePackage(pkg: String) {
        scope.launch {
            val out = withContext(Dispatchers.IO) { runShizukuShell("cmd package install-existing $pkg") }
            Toast.makeText(this@MainActivity, out.takeIf { it.isNotBlank() } ?: "Restore attempted", Toast.LENGTH_LONG).show()
            scanPackages()
        }
    }

    private fun runShizukuShell(command: String): String {
        try {
            val proc = Shizuku.newProcess(arrayOf("sh", "-c", command))
            val br = BufferedReader(InputStreamReader(proc.inputStream))
            val out = StringBuilder()
            var line: String? = br.readLine()
            while (line != null) {
                out.appendLine(line)
                line = br.readLine()
            }
            proc.waitFor()
            br.close()
            val err = proc.errorStream.bufferedReader().readText()
            if (err.isNotBlank()) out.appendLine("ERR: $err")
            return out.toString().trim()
        } catch (e: Exception) {
            return "Error: ${e.message}"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
