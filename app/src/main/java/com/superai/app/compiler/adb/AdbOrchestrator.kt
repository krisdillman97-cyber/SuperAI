package com.superai.app.compiler.adb

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

data class AdbResult(
    val command: String,
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val success: Boolean = exitCode == 0
)

class AdbOrchestrator {

    private val adbPath: String
        get() = "${System.getenv("ANDROID_HOME") ?: "/usr/lib/android-sdk"}/platform-tools/adb"

    suspend fun execute(vararg args: String): AdbResult = withContext(Dispatchers.IO) {
        runCatching {
            val cmd = listOf(adbPath) + args.toList()
            Timber.d("ADB: %s", cmd.joinToString(" "))
            val proc = ProcessBuilder(cmd).redirectErrorStream(false).start()
            val stdout = proc.inputStream.bufferedReader().readText()
            val stderr = proc.errorStream.bufferedReader().readText()
            AdbResult(cmd.joinToString(" "), stdout.trim(), stderr.trim(), proc.waitFor())
        }.getOrElse { e ->
            AdbResult(args.joinToString(" "), "", e.message ?: "error", -1)
        }
    }

    suspend fun devices(): List<String> =
        execute("devices").stdout.lines()
            .drop(1).filter { it.contains("\tdevice") }
            .map { it.substringBefore("\t") }

    suspend fun install(apkPath: String, deviceSerial: String? = null): AdbResult {
        val args = buildList {
            if (deviceSerial != null) { add("-s"); add(deviceSerial) }
            add("install"); add("-r"); add("-t"); add(apkPath)
        }
        return execute(*args.toTypedArray())
    }

    suspend fun push(localPath: String, remotePath: String, deviceSerial: String? = null): AdbResult {
        val args = buildList {
            if (deviceSerial != null) { add("-s"); add(deviceSerial) }
            add("push"); add(localPath); add(remotePath)
        }
        return execute(*args.toTypedArray())
    }

    suspend fun shell(cmd: String, deviceSerial: String? = null): AdbResult {
        val args = buildList {
            if (deviceSerial != null) { add("-s"); add(deviceSerial) }
            add("shell"); add(cmd)
        }
        return execute(*args.toTypedArray())
    }
}
