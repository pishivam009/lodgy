package com.lodgy.app.data.repository

import com.lodgy.app.data.dao.WardenDao
import com.lodgy.app.data.entity.Warden
import io.mockk.coEvery
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
    fun `createWarden persists the given pin hash under the default name`() = runTest {
        val inserted = slot<Warden>()
        coEvery { dao.insert(capture(inserted)) } returns Unit

        repository.createWarden("hashed-pin")

        assertEquals("hashed-pin", inserted.captured.pinHash)
        assertEquals("Warden", inserted.captured.name)
    }
}
