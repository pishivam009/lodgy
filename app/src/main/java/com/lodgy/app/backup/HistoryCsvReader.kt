package com.lodgy.app.backup

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads a warden-picked CSV off the SAF Uri. Null when the file cannot be opened or decoded. */
class HistoryCsvReader @Inject constructor(@ApplicationContext private val context: Context) {

    suspend fun read(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()
    }
}
