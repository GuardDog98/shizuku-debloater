package com.example.shizukudebloater

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
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
    private lateinit var btnSettings: Button
    private lateinit var btnRestore: Button
    private lateinit var switchExpert: Switch
    private lateinit var etFilter: EditText
    private val adapter = PackageAdapter { apk, action -> onPackageAction(apk, action) }
    private val allowList by lazy { UserPreferences.loadAllowList(this) }
    private val ignoreList by lazy { UserPreferences.loadIgnoreList(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rvPackages = findViewById(R.id.rvPackages)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnSettings = findViewById(R.id.btnSettings)
        btnRestore = findViewById(R.id.btnRestore)
        switchExpert = findViewById(R.id.switchExpert)
        etFilter = findViewById(R.id.etFilter)

        rvPackages.layoutManager = LinearLayoutManager(this)
        rvPackages.adapter = adapter

        switchExpert.isChecked = UserPreferences.isExpertMode(this)
        switchExpert.setOnCheckedChangeListener { _, isChecked ->
            UserPreferences.setExpertMode(this, isChecked)
        }

        btnRefresh.setOnClickListener { checkPermissionAndScan() }
        btnSettings.setOnClickListener { startActivity(android.content.Intent(this, SettingsActivity::class.java)) }
        btnRestore.setOnClickListener { startActivity(android.content.Intent(this, RestoreActivity::class.java)) }
        etFilter.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterPackages(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

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
                if (ignoreList.contains(pkg)) continue
                if (allowList.isNotEmpty() && !allowList.contains(pkg)) continue
                try {
                    val ai = pm.getApplicationInfo(pkg, PackageManager.GET_META_DATA)
                    val label = pm.getApplicationLabel(ai).toString()
                    val isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0 || (ai.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                    val apkPath = withContext(Dispatchers.IO) { getApkPath(pkg) }
                    entries.add(AppEntry(pkg, label, isSystem, apkPath))
                } catch (e: Exception) {
                    entries.add(AppEntry(pkg, pkg, false, null))
                }
            }

            val filtered = entries.sortedWith(compareBy({ !it.isSystem }, { it.label.lowercase() }))
            adapter.submitList(filtered)
            filterPackages(etFilter.text?.toString() ?: "")
            Toast.makeText(this@MainActivity, "Found ${entries.size} packages", Toast.LENGTH_SHORT).show()
        }
    }

    private fun filterPackages(query: String) {
        val q = query.trim().lowercase()
        val list = (adapter.currentList ?: emptyList()).filter {
            q.isEmpty() || it.packageName.lowercase().contains(q) || it.label.lowercase().contains(q)
        }
        adapter.submitList(list)
    }

    private fun onPackageAction(entry: AppEntry, action: PackageAction) {
        when (action) {
            PackageAction.DISABLE -> confirmAndPerform(entry, "pm disable-user --user 0 ${entry.packageName}")
            PackageAction.UNINSTALL -> confirmAndPerform(entry, "pm uninstall --user 0 ${entry.packageName}")
            PackageAction.FORCE -> {
                if (!UserPreferences.isExpertMode(this)) {
                    showRiskDialog("Expert mode required", "Force remove is disabled in Safe mode. Enable Expert mode to continue.")
                    return
                }
                scope.launch {
                    val isRoot = withContext(Dispatchers.IO) { checkIsRoot() }
                    if (!isRoot) {
                        showRiskDialog("Root required", "Force remove requires Shizuku root access. Current session is not root.")
                        return@launch
                    }
                    val path = entry.apkPath ?: withContext(Dispatchers.IO) { getApkPath(entry.packageName) }
                    val input = EditText(this@MainActivity).apply {
                        hint = "Type package name to confirm"
                        imeOptions = EditorInfo.IME_ACTION_DONE
                    }
                    val dialog = AlertDialog.Builder(this@MainActivity)
                        .setTitle("Force remove ${entry.packageName}")
                        .setMessage("This will attempt to delete the APK from the system partition. It may impact boot or cause data loss.\n\nAPK path: ${path ?: "unknown"}\n\nType the package name exactly to confirm.")
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
                            scope.launch { performForceRemove(entry) }
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
                    if (cmd.startsWith("pm uninstall") && out.contains("Success", ignoreCase = true)) {
                        ActionLog.recordUninstall(this@MainActivity, entry.packageName)
                    }
                    Toast.makeText(this@MainActivity, out.takeIf { it.isNotBlank() } ?: "Done", Toast.LENGTH_LONG).show()
                    scanPackages()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRiskDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private suspend fun checkIsRoot(): Boolean = withContext(Dispatchers.IO) {
        try {
            val idOut = runShizukuShell("id")
            idOut.contains("uid=0", ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun performForceRemove(entry: AppEntry) = withContext(Dispatchers.IO) {
        val pkg = entry.packageName
        val results = StringBuilder()
        results.appendLine("Safe preview: package=${pkg}")
        results.appendLine("pm uninstall --user 0 $pkg")
        results.appendLine(runShizukuShellSync("pm uninstall --user 0 $pkg"))

        val path = getApkPath(pkg)
        results.appendLine("APK path: ${path ?: "unknown"}")
        if (path.isNullOrBlank()) {
            withContext(Dispatchers.Main) {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Force remove result")
                    .setMessage(results.toString())
                    .setPositiveButton("OK", null)
                    .show()
            }
            return@withContext
        }

        val remountCmds = listOf(
            "mount -o remount,rw /system",
            "mount -o remount,rw /",
            "mount -o rw,remount /system",
            "mount -o remount,rw /system_root"
        )
        for (cmd in remountCmds) {
            results.appendLine("Trying: $cmd")
            results.appendLine(runShizukuShellSync(cmd))
        }

        results.appendLine("rm -f $path")
        results.appendLine(runShizukuShellSync("rm -f $path"))
        results.appendLine(runShizukuShellSync("sync"))
        ActionLog.recordUninstall(this@MainActivity, pkg)

        withContext(Dispatchers.Main) {
            AlertDialog.Builder(this@MainActivity)
                .setTitle("Force remove log")
                .setMessage(results.toString())
                .setPositiveButton("OK", null)
                .show()
            scanPackages()
        }
    }

    private fun getApkPath(pkg: String): String? {
        val out = runShizukuShell("pm path $pkg")
        return out.lines().firstOrNull { it.startsWith("package:") }?.removePrefix("package:")?.trim()
    }

    private fun runShizukuShellSync(command: String): String {
        return try {
            val proc = Shizuku.newProcess(arrayOf("sh", "-c", command))
            val br = BufferedReader(InputStreamReader(proc.inputStream))
            val out = StringBuilder()
            var line: String? = br.readLine()
            while (line != null) { out.appendLine(line); line = br.readLine() }
            proc.waitFor()
            br.close()
            val err = proc.errorStream.bufferedReader().readText()
            if (err.isNotBlank()) out.appendLine("ERR: $err")
            out.toString().trim()
        } catch (e: Exception) { "Error: ${e.message}" }
    }

    private fun runShizukuShell(command: String): String {
        return try {
            val proc = Shizuku.newProcess(arrayOf("sh", "-c", command))
            val br = BufferedReader(InputStreamReader(proc.inputStream))
            val out = StringBuilder()
            var line: String? = br.readLine()
            while (line != null) { out.appendLine(line); line = br.readLine() }
            proc.waitFor()
            br.close()
            val err = proc.errorStream.bufferedReader().readText()
            if (err.isNotBlank()) out.appendLine("ERR: $err")
            out.toString().trim()
        } catch (e: Exception) { "Error: ${e.message}" }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
