package com.example.shizukudebloater

import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

object ShellUtils {
    fun runShizukuShell(command: String): String {
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
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    fun runShizukuShellSync(command: String): String = runShizukuShell(command)
}
