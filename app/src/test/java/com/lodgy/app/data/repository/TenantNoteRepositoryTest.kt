package com.lodgy.app.data.repository

import com.lodgy.app.data.dao.TenantNoteDao
import com.lodgy.app.data.entity.NoteType
import com.lodgy.app.data.entity.TenantNote
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TenantNoteRepositoryTest {

    private val dao: TenantNoteDao = mockk()
    private val repository = TenantNoteRepository(dao)

    @Test
    fun `create persists a note with the given fields`() = runTest {
        val inserted = slot<TenantNote>()
        coEvery { dao.insert(capture(inserted)) } returns Unit

        val created = repository.create("t1", NoteType.COMPLAINT, "Loud music", "photo.jpg", 100L)

        assertEquals(NoteType.COMPLAINT, created.type)
        assertEquals("Loud music", created.text)
        assertEquals("photo.jpg", created.photoPath)
        assertEquals(created, inserted.captured)
    }

    @Test
    fun `update replaces the mutable fields`() = runTest {
        val existing = TenantNote(id = "n1", tenantId = "t1", type = NoteType.GENERAL, text = "old", photoPath = null, occurredOn = 0L, createdAt = 0L, updatedAt = 0L)
        val updated = slot<TenantNote>()
        coEvery { dao.update(capture(updated)) } returns Unit

        repository.update(existing, NoteType.DAMAGE, "broke a window", null, 200L)

        assertEquals(NoteType.DAMAGE, updated.captured.type)
        assertEquals("broke a window", updated.captured.text)
        assertEquals(200L, updated.captured.occurredOn)
    }

    @Test
    fun `delete delegates to the dao`() = runTest {
        val existing = TenantNote(id = "n1", tenantId = "t1", type = NoteType.GENERAL, text = "x", photoPath = null, occurredOn = 0L, createdAt = 0L, updatedAt = 0L)
        coEvery { dao.delete(existing) } returns Unit

        repository.delete(existing)

        coVerify { dao.delete(existing) }
    }
}
