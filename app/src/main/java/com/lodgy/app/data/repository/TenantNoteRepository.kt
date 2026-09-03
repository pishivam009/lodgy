package com.lodgy.app.data.repository

import com.lodgy.app.data.dao.TenantNoteDao
import com.lodgy.app.data.entity.NoteType
import com.lodgy.app.data.entity.TenantNote
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class TenantNoteRepository @Inject constructor(private val tenantNoteDao: TenantNoteDao) {
    fun getByTenantId(tenantId: String): Flow<List<TenantNote>> = tenantNoteDao.getByTenantId(tenantId)

    suspend fun getById(id: String): TenantNote? = tenantNoteDao.getById(id)

    suspend fun create(
        tenantId: String,
        type: NoteType,
        text: String,
        photoPath: String?,
        occurredOn: Long,
    ): TenantNote {
        val now = System.currentTimeMillis()
        val note = TenantNote(
            tenantId = tenantId,
            type = type,
            text = text,
            photoPath = photoPath,
            occurredOn = occurredOn,
            createdAt = now,
            updatedAt = now,
        )
        tenantNoteDao.insert(note)
        return note
    }

    suspend fun update(
        note: TenantNote,
        type: NoteType,
        text: String,
        photoPath: String?,
        occurredOn: Long,
    ) {
        tenantNoteDao.update(
            note.copy(
                type = type,
                text = text,
                photoPath = photoPath,
                occurredOn = occurredOn,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun delete(note: TenantNote) = tenantNoteDao.delete(note)
}
