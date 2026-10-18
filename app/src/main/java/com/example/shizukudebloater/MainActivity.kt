package com.example.shizukudebloater

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.widget.*
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
        when (action) {
            PackageAction.DISABLE -> confirmAndPerform(entry, "pm disable-user --user 0 ${entry.packageName}")
            PackageAction.UNINSTALL -> confirmAndPerform(entry, "pm uninstall --user 0 ${entry.packageName}")
            PackageAction.FORCE -> {
                val expert = prefs.getBoolean("expert_mode", false)
                if (!expert) {
                    AlertDialog.Builder(this)
                        .setTitle("Expert mode required")
                        .setMessage("Force remove is available only in Expert mode. Enable Expert mode to continue.")
                        .setPositiveButton("OK", null)
                        .show()
                    return
                }
                // Check root
                scope.launch {
                    val isRoot = withContext(Dispatchers.IO) { checkIsRoot() }
                    if (!isRoot) {
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("Root required")
                            .setMessage("Force remove requires Shizuku running with root privileges. Current session does not appear to be root. Aborting.")
                            .setPositiveButton("OK", null)
                            .show()
                        return@launch
                    }

                    // Double-confirm dialog requiring exact package name input
                    val input = EditText(this@MainActivity).apply {
                        hint = "Type package name to confirm"
                        imeOptions = EditorInfo.IME_ACTION_DONE
                    }
                    val dialog = AlertDialog.Builder(this@MainActivity)
                        .setTitle("Force remove ${entry.packageName}")
                        .setMessage("This will attempt to permanently remove the package from the system partition. This is dangerous and may brick the device. Type the package name exactly to confirm.")
                        .setView(input)
                        .setPositiveButton("Remove", null)
                        .setNegativeButton("Cancel", null)
                        .create()

                    dialog.setOnShowListener {
                        val btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        btn.isEnabled = false
                        input.addTextChangedListener(object : TextWatcher {
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                                btn.isEnabled = s?.toString() == entry.packageName
                            }
                            override fun afterTextChanged(s: Editable?) {}
                        })
                        btn.setOnClickListener {
                            dialog.dismiss()
                            // perform force remove
                            scope.launch {
                                performForceRemove(entry)
                            }
                        }
                    }
                    dialog.show()
                }
            }
        }
    }

    private fun confirmAndPerform(entry: AppEntry, cmd: String) {
        AlertDialog.Builder(this)
            .setTitle("Confirm")
            .setMessage("This will run: $cmd\nProceed?")
            .setPositiveButton("Yes") { _, _ ->
                scope.launch {
                    val out = withContext(Dispatchers.IO) { runShizukuShell(cmd) }
                    if (cmd.startsWith("pm uninstall") && out.contains("Success")) ActionLog.recordUninstall(this@MainActivity, entry.packageName)
                    Toast.makeText(this@MainActivity, out.takeIf { it.isNotBlank() } ?: "Done", Toast.LENGTH_LONG).show()
                    scanPackages()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private suspend fun checkIsRoot(): Boolean = withContext(Dispatchers.IO) {
        try {
            val idOut = runShizukuShell("id")
            return@withContext idOut?.contains("uid=0") == true
        } catch (e: Exception) {
            return@withContext false
        }
    }

    private suspend fun performForceRemove(entry: AppEntry) = withContext(Dispatchers.IO) {
        val pkg = entry.packageName
        val results = StringBuilder()
        // Step 1: uninstall for user (soft)
        results.appendLine("pm uninstall --user 0 $pkg")
        results.appendLine(runShizukuShellSync("pm uninstall --user 0 $pkg"))

        // Step 2: find APK path
        results.appendLine("pm path $pkg")
        val pathOut = runShizukuShellSync("pm path $pkg").lines().firstOrNull()?.removePrefix("package:")
        if (pathOut.isNullOrBlank()) {
            results.appendLine("APK path not found — may already be removed from system or package manager can't locate it.")
            withContext(Dispatchers.Main) {
                ActionLog.recordUninstall(this@MainActivity, pkg)
                Toast.makeText(this@MainActivity, results.toString(), Toast.LENGTH_LONG).show()
                scanPackages()
            }
            return@withContext
        }

        results.appendLine("Found path: $pathOut")

        // Step 3: try remount variants
        val remountCmds = listOf(
            "mount -o remount,rw /system",
            "mount -o remount,rw /",
            "mount -o rw,remount /system",
            "mount -o remount,rw /system_root",
            "mount -o remount,rw /system_root/system"
        )
        var remounted = false
        for (cmd in remountCmds) {
            results.appendLine("Trying: $cmd")
            val r = runShizukuShellSync(cmd)
            results.appendLine(r)
            if (!r.contains("No such file or directory") && !r.contains("not found") ) {
                remounted = true
                break
            }
        }

        // Step 4: attempt delete
        results.appendLine("rm -f $pathOut")
        results.appendLine(runShizukuShellSync("rm -f $pathOut"))
        results.appendLine(runShizukuShellSync("sync"))

        // Optional: try restorecon
        results.appendLine(runShizukuShellSync("restorecon -v $pathOut"))

        // Record action
        ActionLog.recordUninstall(this@MainActivity, pkg)

        withContext(Dispatchers.Main) {
            Toast.makeText(this@MainActivity, "Force remove finished. See log for details (Toast may truncate).", Toast.LENGTH_LONG).show()
            // For convenience show a dialog with short log
            AlertDialog.Builder(this@MainActivity)
                .setTitle("Force remove log")
                .setMessage(results.toString())
                .setPositiveButton("OK", null)
                .show()
            scanPackages()
        }
    }

    // Helper to run shell synchronously inside IO dispatcher (used by force remove steps)
    private fun runShizukuShellSync(command: String): String {
        return try {
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
            out.toString().trim()
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    private fun runShizukuShell(command: String): String {
        return try {
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
            out.toString().trim()
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
