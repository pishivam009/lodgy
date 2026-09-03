package com.lodgy.app.ui.tenant

import com.lodgy.app.data.entity.Bed
import com.lodgy.app.data.entity.BedStatus
import com.lodgy.app.data.entity.Floor
import com.lodgy.app.data.entity.Room
import com.lodgy.app.data.entity.RoomType
import com.lodgy.app.data.prefs.HostelPreferences
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.FloorRepository
import com.lodgy.app.data.repository.RoomRepository
import com.lodgy.app.testutil.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BedPickerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val hostelPreferences: HostelPreferences = mockk()
    private val floorRepository: FloorRepository = mockk()
    private val roomRepository: RoomRepository = mockk()
    private val bedRepository: BedRepository = mockk()

    private fun viewModel() = BedPickerViewModel(hostelPreferences, floorRepository, roomRepository, bedRepository)

    @Test
    fun `no active hostel reports hasActiveHostel false with no options`() {
        every { hostelPreferences.selectedHostelId } returns flowOf(null)

        val state = viewModel().uiState.value

        assertFalse(state.loading)
        assertFalse(state.hasActiveHostel)
        assertTrue(state.options.isEmpty())
    }

    @Test
    fun `only vacant beds across every floor and room are offered, sorted by label`() {
        val floor1 = Floor(id = "f1", hostelId = "h1", label = "Ground", sortOrder = 0, createdAt = 0L, updatedAt = 0L)
        val room1 = Room(id = "r1", floorId = "f1", roomNumber = "101", type = RoomType.DOUBLE, pricePerBed = 3000.0, amenities = "", createdAt = 0L, updatedAt = 0L)
        val bedB = Bed(id = "b2", roomId = "r1", label = "B", status = BedStatus.VACANT, createdAt = 0L, updatedAt = 0L)
        val bedA = Bed(id = "b1", roomId = "r1", label = "A", status = BedStatus.OCCUPIED, createdAt = 0L, updatedAt = 0L)

        every { hostelPreferences.selectedHostelId } returns flowOf("h1")
        every { floorRepository.getByHostelId("h1") } returns flowOf(listOf(floor1))
        every { roomRepository.getByFloorId("f1") } returns flowOf(listOf(room1))
        every { bedRepository.getByRoomId("r1") } returns flowOf(listOf(bedB, bedA))

        val state = viewModel().uiState.value

        assertFalse(state.loading)
        assertTrue(state.hasActiveHostel)
        assertEquals(1, state.options.size)
        assertEquals("B", state.options.single().bed.label)
        assertEquals("101", state.options.single().roomNumber)
        assertEquals("Ground", state.options.single().floorLabel)
    }
}
