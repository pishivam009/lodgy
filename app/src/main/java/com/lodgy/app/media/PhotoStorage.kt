package com.lodgy.app.media

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PhotoStorage @Inject constructor(@ApplicationContext private val context: Context) {

    /** A content:// Uri the system camera app can write a full-size photo into. Kept in its own
     *  cacheDir subdirectory, which is the only path the FileProvider is configured to share. */
    fun createCameraOutputUri(): Uri {
        val cameraDir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(cameraDir, "camera_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /** Copies whatever [sourceUri] points at into app-private storage; returns the saved file's absolute path. */
    suspend fun persist(sourceUri: Uri): String = withContext(Dispatchers.IO) {
        val photosDir = File(context.filesDir, "photos").apply { mkdirs() }
        val destFile = File(photosDir, "${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            destFile.outputStream().use { output -> input.copyTo(output) }
        }
        destFile.absolutePath
    }
}
