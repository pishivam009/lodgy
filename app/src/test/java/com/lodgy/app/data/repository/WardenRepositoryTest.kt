package com.lodgy.app.data.repository

import com.lodgy.app.data.dao.WardenDao
import com.lodgy.app.data.entity.Warden
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WardenRepositoryTest {

    private val dao: WardenDao = mockk()
    private val repository = WardenRepository(dao)

    @Test
    fun `getWarden returns null when none exists yet`() = runTest {
        coEvery { dao.getFirst() } returns null
        assertNull(repository.getWarden())
    }

    @Test
    fun `getWarden returns the stored warden`() = runTest {
        val warden = Warden(id = "w1", pinHash = "hash", name = "Warden", createdAt = 0L, updatedAt = 0L)
        coEvery { dao.getFirst() } returns warden
        assertEquals(warden, repository.getWarden())
    }

    @Test
    fun `setPin inserts a new warden on first launch`() = runTest {
        coEvery { dao.getFirst() } returns null
        val inserted = slot<Warden>()
        coEvery { dao.insert(capture(inserted)) } returns Unit

        repository.setPin("hashed-pin")

        assertEquals("hashed-pin", inserted.captured.pinHash)
        assertEquals("Warden", inserted.captured.name)
    }

    @Test
    fun `setPin updates the existing warden in place, keeping its id`() = runTest {
        // Re-setup after a reset must reuse the row: hostels foreign-key to warden.id.
        val existing = Warden(id = "w1", pinHash = "", name = "Warden", createdAt = 5L, updatedAt = 5L)
        coEvery { dao.getFirst() } returns existing
        val updated = slot<Warden>()
        coEvery { dao.update(capture(updated)) } returns Unit

        repository.setPin("new-hash")

        assertEquals("w1", updated.captured.id)
        assertEquals("new-hash", updated.captured.pinHash)
        coVerify(exactly = 0) { dao.insert(any()) }
    }

    @Test
    fun `clearWarden blanks the pin but keeps the row, so the foreign key from hostels holds`() = runTest {
        val existing = Warden(id = "w1", pinHash = "hash", name = "Warden", createdAt = 5L, updatedAt = 5L)
        coEvery { dao.getFirst() } returns existing
        val updated = slot<Warden>()
        coEvery { dao.update(capture(updated)) } returns Unit

        repository.clearWarden()

        assertEquals("w1", updated.captured.id)
        assertEquals("", updated.captured.pinHash)
    }
}
