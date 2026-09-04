package com.lodgy.app.data.repository

import com.lodgy.app.data.dao.BedDao
import com.lodgy.app.data.dao.BedLocation
import com.lodgy.app.data.dao.FloorOccupancy
import com.lodgy.app.data.dao.RoomOccupancy
import com.lodgy.app.data.dao.VacantBedDetail
import com.lodgy.app.data.dao.VacantBedRow
import com.lodgy.app.data.entity.Bed
import com.lodgy.app.data.entity.BedStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BedRepositoryTest {

    private val bedDao: BedDao = mockk()
    private val repository = BedRepository(bedDao)

    private fun bed(status: BedStatus, id: String = "bed-1") =
        Bed(id = id, roomId = "room-1", label = "A", status = status, createdAt = 0L, updatedAt = 0L)

    @Test
    fun `hasOccupiedBed is true when any bed in the room is occupied`() = runTest {
        every { bedDao.getByRoomId("room-1") } returns flowOf(listOf(bed(BedStatus.VACANT), bed(BedStatus.OCCUPIED)))
        assertTrue(repository.hasOccupiedBed("room-1"))
    }

    @Test
    fun `hasOccupiedBed is false when every bed in the room is vacant`() = runTest {
        every { bedDao.getByRoomId("room-1") } returns flowOf(listOf(bed(BedStatus.VACANT), bed(BedStatus.VACANT)))
        assertFalse(repository.hasOccupiedBed("room-1"))
    }

    @Test
    fun `generateForRoom creates sequentially lettered vacant beds`() = runTest {
        val inserted = mutableListOf<Bed>()
        coEvery { bedDao.insert(capture(inserted)) } returns Unit

        repository.generateForRoom("room-1", 3)

        assertEquals(listOf("A", "B", "C"), inserted.map { it.label })
        assertTrue(inserted.all { it.roomId == "room-1" && it.status == BedStatus.VACANT })
    }

    @Test
    fun `setOccupied flips a vacant bed's status and bumps updatedAt`() = runTest {
        coEvery { bedDao.getById("bed-1") } returns bed(BedStatus.VACANT).copy(updatedAt = 1L)
        val updated = slot<Bed>()
        coEvery { bedDao.update(capture(updated)) } returns Unit

        repository.setOccupied("bed-1")

        assertEquals(BedStatus.OCCUPIED, updated.captured.status)
        assertTrue(updated.captured.updatedAt >= 1L)
    }

    @Test
    fun `setVacant flips an occupied bed's status`() = runTest {
        coEvery { bedDao.getById("bed-1") } returns bed(BedStatus.OCCUPIED)
        val updated = slot<Bed>()
        coEvery { bedDao.update(capture(updated)) } returns Unit

        repository.setVacant("bed-1")

        assertEquals(BedStatus.VACANT, updated.captured.status)
    }

    @Test
    fun `setStatus is a no-op when the bed no longer exists`() = runTest {
        coEvery { bedDao.getById("missing") } returns null

        repository.setOccupied("missing")

        coVerify(exactly = 0) { bedDao.update(any()) }
    }

    @Test
    fun `the aggregate and lookup queries pass straight through to the dao`() = runTest {
        val roomOccupancy = listOf(RoomOccupancy("r1", totalBeds = 3, occupiedBeds = 1))
        val floorOccupancy = listOf(FloorOccupancy("f1", totalBeds = 6, occupiedBeds = 4))
        val vacantRows = listOf(VacantBedRow("b1", "A", "101", 5000.0, "Ground"))
        val longVacant = listOf(VacantBedDetail("b1", "A", "101", "Ground", "Sunrise", 0L))

        coEvery { bedDao.getLocation("b1") } returns BedLocation("101", "A")
        every { bedDao.observeOccupancyByFloor("f1") } returns flowOf(roomOccupancy)
        every { bedDao.observeOccupancyByHostel("h1") } returns flowOf(floorOccupancy)
        every { bedDao.observeRoomOccupancyByHostel("h1") } returns flowOf(roomOccupancy)
        coEvery { bedDao.getVacantBedsByHostel("h1") } returns vacantRows
        coEvery { bedDao.getLongVacantBeds(500L) } returns longVacant
        coEvery { bedDao.getVacantBedIds() } returns listOf("b1")

        assertEquals(BedLocation("101", "A"), repository.getLocation("b1"))
        assertEquals(roomOccupancy, repository.observeOccupancyByFloor("f1").first())
        assertEquals(floorOccupancy, repository.observeOccupancyByHostel("h1").first())
        assertEquals(roomOccupancy, repository.observeRoomOccupancyByHostel("h1").first())
        assertEquals(vacantRows, repository.getVacantBedsByHostel("h1"))
        assertEquals(longVacant, repository.getLongVacantBeds(500L))
        assertEquals(listOf("b1"), repository.getVacantBedIds())
    }

    @Test
    fun `occupancy rows derive vacant beds from the totals`() {
        assertEquals(2, RoomOccupancy("r1", totalBeds = 3, occupiedBeds = 1).vacantBeds)
        assertEquals(2, FloorOccupancy("f1", totalBeds = 6, occupiedBeds = 4).vacantBeds)
    }
}
