package com.lodgy.app.data.repository

import com.lodgy.app.data.dao.RoomDao
import com.lodgy.app.data.entity.Room
import com.lodgy.app.data.entity.RoomType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomRepositoryTest {

    private val roomDao: RoomDao = mockk()
    private val repository = RoomRepository(roomDao)

    @Test
    fun `create persists a room with the given fields`() = runTest {
        val inserted = slot<Room>()
        coEvery { roomDao.insert(capture(inserted)) } returns Unit

        val created = repository.create("floor-1", "101", RoomType.DOUBLE, 4000.0, "AC")

        assertEquals("101", created.roomNumber)
        assertEquals(RoomType.DOUBLE, created.type)
        assertEquals("AC", created.amenities)
        assertEquals(created, inserted.captured)
    }

    @Test
    fun `update replaces the mutable fields`() = runTest {
        val existing = Room(id = "r1", floorId = "f1", roomNumber = "101", type = RoomType.SINGLE, pricePerBed = 3000.0, amenities = "", createdAt = 0L, updatedAt = 0L)
        val updated = slot<Room>()
        coEvery { roomDao.update(capture(updated)) } returns Unit

        repository.update(existing, "102", RoomType.TRIPLE, 2000.0, "Balcony")

        assertEquals("102", updated.captured.roomNumber)
        assertEquals(RoomType.TRIPLE, updated.captured.type)
        assertEquals(2000.0, updated.captured.pricePerBed, 0.0001)
        assertEquals("Balcony", updated.captured.amenities)
    }

    @Test
    fun `delete delegates to the dao`() = runTest {
        val room = Room(id = "r1", floorId = "f1", roomNumber = "101", type = RoomType.SINGLE, pricePerBed = 3000.0, amenities = "", createdAt = 0L, updatedAt = 0L)
        coEvery { roomDao.delete(room) } returns Unit

        repository.delete(room)

        coVerify { roomDao.delete(room) }
    }
}
