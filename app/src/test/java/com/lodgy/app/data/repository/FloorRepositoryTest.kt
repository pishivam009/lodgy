package com.lodgy.app.data.repository

import com.lodgy.app.data.dao.FloorDao
import com.lodgy.app.data.entity.Floor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FloorRepositoryTest {

    private val floorDao: FloorDao = mockk()
    private val repository = FloorRepository(floorDao)

    private fun floor(id: String, sortOrder: Int) =
        Floor(id = id, hostelId = "hostel-1", label = id, sortOrder = sortOrder, createdAt = 0L, updatedAt = 0L)

    @Test
    fun `create appends after the current highest sort order`() = runTest {
        every { floorDao.getByHostelId("hostel-1") } returns flowOf(listOf(floor("f1", 0), floor("f2", 2)))
        coEvery { floorDao.insert(any()) } returns Unit

        val created = repository.create("hostel-1", "Ground")

        assertEquals(3, created.sortOrder)
        assertEquals("Ground", created.label)
    }

    @Test
    fun `create starts at zero for the first floor in a hostel`() = runTest {
        every { floorDao.getByHostelId("hostel-1") } returns flowOf(emptyList())
        coEvery { floorDao.insert(any()) } returns Unit

        val created = repository.create("hostel-1", "Ground")

        assertEquals(0, created.sortOrder)
    }

    @Test
    fun `moveUp swaps sort order with the previous floor`() = runTest {
        val f1 = floor("f1", 0)
        val f2 = floor("f2", 1)
        every { floorDao.getByHostelId("hostel-1") } returns flowOf(listOf(f1, f2))
        val updates = mutableListOf<Floor>()
        coEvery { floorDao.update(capture(updates)) } returns Unit

        repository.moveUp(f2, "hostel-1")

        assertEquals(2, updates.size)
        assertEquals(0, updates.first { it.id == "f2" }.sortOrder)
        assertEquals(1, updates.first { it.id == "f1" }.sortOrder)
    }

    @Test
    fun `moveUp on the topmost floor does nothing`() = runTest {
        val f1 = floor("f1", 0)
        val f2 = floor("f2", 1)
        every { floorDao.getByHostelId("hostel-1") } returns flowOf(listOf(f1, f2))

        repository.moveUp(f1, "hostel-1")

        coVerify(exactly = 0) { floorDao.update(any()) }
    }

    @Test
    fun `moveDown on the bottommost floor does nothing`() = runTest {
        val f1 = floor("f1", 0)
        val f2 = floor("f2", 1)
        every { floorDao.getByHostelId("hostel-1") } returns flowOf(listOf(f1, f2))

        repository.moveDown(f2, "hostel-1")

        coVerify(exactly = 0) { floorDao.update(any()) }
    }

    @Test
    fun `rename updates only the label`() = runTest {
        val f1 = floor("f1", 0)
        val updated = slot<Floor>()
        coEvery { floorDao.update(capture(updated)) } returns Unit

        repository.rename(f1, "First Floor")

        assertEquals("First Floor", updated.captured.label)
        assertEquals(0, updated.captured.sortOrder)
    }

    @Test
    fun `delete delegates straight to the dao`() = runTest {
        val f1 = floor("f1", 0)
        coEvery { floorDao.delete(f1) } returns Unit

        repository.delete(f1)

        coVerify { floorDao.delete(f1) }
    }
}
