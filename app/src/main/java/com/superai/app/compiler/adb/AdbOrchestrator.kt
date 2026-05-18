package com.superai.app.compiler.adb

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class AdbResult(
    val command: String,
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val success: Boolean = exitCode == 0
)

@Singleton
class AdbOrchestrator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val adbPath: String
        get() = "${System.getenv("ANDROID_HOME") ?: "/usr/lib/android-sdk"}/platform-tools/adb"

    suspend fun execute(vararg args: String): AdbResult = withContext(Dispatchers.IO) {
        runCatching {
            val cmd = listOf(adbPath) + args.toList()
            Timber.d("ADB: ${cmd.joinToString(" ")}")
            val proc = ProcessBuilder(cmd)
                .redirectErrorStream(false)
                .start()
            val stdout = proc.inputStream.bufferedReader().readText()
            val stderr = proc.errorStream.bufferedReader().readText()
            val code   = proc.waitFor()
            AdbResult(cmd.joinToString(" "), stdout.trim(), stderr.trim(), code)
        }.getOrElse { e ->
            AdbResult(args.joinToString(" "), "", e.message ?: "error", -1)
        }
    }

    suspend fun devices(): List<String> {
        val result = execute("devices")
        return result.stdout.lines()
            .drop(1)
            .filter { it.contains("\tdevice") }
            .map { it.substringBefore("\t") }
    }

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

    suspend fun pullLog(deviceSerial: String? = null): AdbResult =
        shell("logcat -d -t 200", deviceSerial)
}
