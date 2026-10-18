package com.example.shizukudebloater

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
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
    private val adapter = PackageAdapter { pkg, action -> onPackageAction(pkg, action) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rvPackages = findViewById(R.id.rvPackages)
        btnRefresh = findViewById(R.id.btnRefresh)

        rvPackages.layoutManager = LinearLayoutManager(this)
        rvPackages.adapter = adapter

        btnRefresh.setOnClickListener { checkPermissionAndScan() }

        // Try to ping Shizuku early
        if (!Shizuku.pingBinder()) {
            Toast.makeText(this, "Shizuku not running — install and start Shizuku Manager", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkPermissionAndScan() {
        if (!Shizuku.pingBinder()) {
            Toast.makeText(this, "Shizuku not running", Toast.LENGTH_SHORT).show()
            return
        }

        // Check Shizuku permission
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(123) // user will be asked by Shizuku Manager
            Toast.makeText(this, "Requested Shizuku permission. Please grant and retry.", Toast.LENGTH_LONG).show()
            return
        }

        scanPackages()
    }

    private fun scanPackages() {
        scope.launch {
            val pkgs = withContext(Dispatchers.IO) {
                runShizukuShell("pm list packages --user 0")
            }.lines()
                .mapNotNull { it.trim().takeIf { it.isNotEmpty() }?.removePrefix("package:") }
                .distinct()
            adapter.submitList(pkgs)
            Toast.makeText(this@MainActivity, "Found ${pkgs.size} packages", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onPackageAction(pkg: String, action: PackageAction) {
        val (title, cmd) = when (action) {
            PackageAction.DISABLE -> "Disable $pkg?" to "pm disable-user --user 0 $pkg"
            PackageAction.UNINSTALL -> "Uninstall for user $pkg?" to "pm uninstall --user 0 $pkg"
        }
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage("This will run: $cmd\nProceed?")
            .setPositiveButton("Yes") { _, _ ->
                scope.launch {
                    val out = withContext(Dispatchers.IO) { runShizukuShell(cmd) }
                    Toast.makeText(this@MainActivity, out.takeIf { it.isNotBlank() } ?: "Done", Toast.LENGTH_LONG).show()
                    // refresh list after action
                    scanPackages()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun runShizukuShell(command: String): String {
        try {
            // Build shell process via Shizuku
            // Using "sh -c" so complex commands work
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
