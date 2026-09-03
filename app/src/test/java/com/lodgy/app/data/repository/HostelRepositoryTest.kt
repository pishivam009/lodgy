package com.lodgy.app.data.repository

import com.lodgy.app.data.dao.HostelDao
import com.lodgy.app.data.entity.Hostel
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class HostelRepositoryTest {

    private val hostelDao: HostelDao = mockk()
    private val repository = HostelRepository(hostelDao)

    @Test
    fun `create persists and returns a hostel with the given fields`() = runTest {
        val inserted = slot<Hostel>()
        coEvery { hostelDao.insert(capture(inserted)) } returns Unit

        val created = repository.create("warden-1", "Sunrise PG", "MG Road", "9999999999")

        assertEquals("warden-1", created.wardenId)
        assertEquals("Sunrise PG", created.name)
        assertEquals("MG Road", created.address)
        assertEquals("9999999999", created.contactPhone)
        assertEquals(created, inserted.captured)
    }

    @Test
    fun `update replaces the mutable fields and bumps updatedAt`() = runTest {
        val existing = Hostel(id = "h1", wardenId = "w1", name = "Old", address = "Old Addr", contactPhone = "1", createdAt = 0L, updatedAt = 0L)
        val updated = slot<Hostel>()
        coEvery { hostelDao.update(capture(updated)) } returns Unit

        repository.update(existing, "New", "New Addr", "2")

        assertEquals("New", updated.captured.name)
        assertEquals("New Addr", updated.captured.address)
        assertEquals("2", updated.captured.contactPhone)
        assertEquals("h1", updated.captured.id)
    }
}
