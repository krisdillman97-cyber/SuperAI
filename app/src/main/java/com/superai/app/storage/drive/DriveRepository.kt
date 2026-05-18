package com.superai.app.storage.drive

import android.content.Context
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File as DriveFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class DriveFileInfo(
    val id: String,
    val name: String,
    val mimeType: String,
    val size: Long,
    val modifiedTime: Long
)

@Singleton
class DriveRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var _drive: Drive? = null
    private var _accountName: String? = null

    val isSignedIn: Boolean get() = _accountName != null

    fun initWithAccount(accountName: String) {
        _accountName = accountName
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_FILE)
        ).also { it.selectedAccountName = accountName }

        _drive = Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("SuperAI/1.0").build()
        Timber.d("Drive initialised for $accountName")
    }

    suspend fun listFiles(folderId: String = "root"): List<DriveFileInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                _drive!!.files().list()
                    .setQ("'$folderId' in parents and trashed = false")
                    .setFields("files(id,name,mimeType,size,modifiedTime)")
                    .setPageSize(100)
                    .execute()
                    .files
                    ?.map { f ->
                        DriveFileInfo(
                            id           = f.id ?: "",
                            name         = f.name ?: "",
                            mimeType     = f.mimeType ?: "",
                            size         = f.getSize() ?: 0L,
                            modifiedTime = f.modifiedTime?.value ?: 0L
                        )
                    } ?: emptyList()
            }.getOrElse { e -> Timber.e(e, "Drive listFiles failed"); emptyList() }
        }

    suspend fun uploadFile(localFile: File, mimeType: String, folderId: String = "root"): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val meta = DriveFile().apply {
                    name = localFile.name
                    parents = listOf(folderId)
                }
                val content = FileContent(mimeType, localFile)
                _drive!!.files().create(meta, content)
                    .setFields("id")
                    .execute().id
            }.getOrElse { e -> Timber.e(e, "Drive upload failed"); null }
        }

    suspend fun downloadFile(fileId: String, dest: File): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                dest.outputStream().use { out ->
                    _drive!!.files().get(fileId).executeMediaAndDownloadTo(out)
                }
                true
            }.getOrElse { e -> Timber.e(e, "Drive download failed"); false }
        }

    suspend fun deleteFile(fileId: String): Boolean =
        withContext(Dispatchers.IO) {
            runCatching { _drive!!.files().delete(fileId).execute(); true }
                .getOrElse { e -> Timber.e(e, "Drive delete failed"); false }
        }

    fun signOut() { _drive = null; _accountName = null }
}
