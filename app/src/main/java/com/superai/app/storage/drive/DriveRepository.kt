package com.superai.app.storage.drive

import android.content.Context
import timber.log.Timber
import java.io.File

class DriveRepository(private val context: Context) {

    suspend fun syncAll() {
        Timber.d("Drive: syncAll")
        uploadPendingFiles()
        fetchLatest()
    }

    suspend fun uploadPendingFiles() {
        Timber.d("Drive: uploadPendingFiles")
        val pendingDir = File(context.filesDir, "pending_upload")
        if (!pendingDir.exists()) return
        pendingDir.listFiles()?.forEach { f ->
            Timber.d("Drive: would upload %s", f.name)
        }
    }

    suspend fun fetchLatest() {
        Timber.d("Drive: fetchLatest — stub (requires OAuth token at runtime)")
    }
}
