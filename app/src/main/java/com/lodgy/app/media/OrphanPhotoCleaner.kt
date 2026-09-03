package com.lodgy.app.media

import android.content.Context
import com.lodgy.app.data.repository.TenantRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * PhotoStorage.persist() writes a picked image to app-private storage before the form that asked
 * for it is ever saved, so a cancelled or interrupted pick leaves a file nothing points at. Those
 * accumulate silently and ride along in every backup zip, so they get swept on app start.
 */
class OrphanPhotoCleaner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tenantRepository: TenantRepository,
) {

    suspend fun clean(): Int = withContext(Dispatchers.IO) {
        cleanIn(File(context.filesDir, "photos"))
    }

    /** Returns how many files were removed. */
    internal suspend fun cleanIn(photosDir: File): Int {
        val files = photosDir.listFiles()?.toList().orEmpty()
        if (files.isEmpty()) return 0

        // Every tenant, vacated included - a checked-out tenant's ID proof is still referenced.
        val referenced = tenantRepository.getAll().first()
            .flatMap { listOf(it.photoPath, it.idProofPhotoPath) }
            .filterNotNull()
            // Matched on file name, not the stored absolute path: restore (LODGY-29) copies photos
            // back by name while the DB keeps whatever path the old install wrote, so comparing
            // full paths would declare a freshly restored library entirely orphaned and delete it.
            .map { File(it).name }
            .toSet()

        return files.count { it.name !in referenced && it.delete() }
    }
}
