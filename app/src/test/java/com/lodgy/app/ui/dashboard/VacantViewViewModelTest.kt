package com.lodgy.app.ui.dashboard

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
import com.lodgy.app.ui.common.BedFilter
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class VacantViewViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val hostelPreferences: HostelPreferences = mockk()
    private val floorRepository: FloorRepository = mockk()
    private val roomRepository: RoomRepository = mockk()
    private val bedRepository: BedRepository = mockk()

    private fun viewModel() = VacantViewViewModel(hostelPreferences, floorRepository, roomRepository, bedRepository)

    @Test
    fun `no active hostel leaves the list empty and stops loading`() {
        every { hostelPreferences.selectedHostelId } returns flowOf(null)

        val state = viewModel().uiState.value

        assertFalse(state.loading)
        assertFalse(state.hasActiveHostel)
        assertTrue(state.items.isEmpty())
    }

    @Test
    fun `the default filter shows only vacant beds, sorted by bed label, under their room and floor`() {
        val floor1 = Floor(id = "f1", hostelId = "h1", label = "Ground", sortOrder = 0, createdAt = 0L, updatedAt = 0L)
        val room1 = Room(id = "r1", floorId = "f1", roomNumber = "101", type = RoomType.DOUBLE, pricePerBed = 3000.0, amenities = "", createdAt = 0L, updatedAt = 0L)
        val bedB = Bed(id = "b2", roomId = "r1", label = "B", status = BedStatus.VACANT, createdAt = 0L, updatedAt = 0L)
        val bedA = Bed(id = "b1", roomId = "r1", label = "A", status = BedStatus.OCCUPIED, createdAt = 0L, updatedAt = 0L)
        val bedC = Bed(id = "b3", roomId = "r1", label = "C", status = BedStatus.VACANT, createdAt = 0L, updatedAt = 0L)

        every { hostelPreferences.selectedHostelId } returns flowOf("h1")
        every { floorRepository.getByHostelId("h1") } returns flowOf(listOf(floor1))
        every { roomRepository.getByFloorId("f1") } returns flowOf(listOf(room1))
        every { bedRepository.getByRoomId("r1") } returns flowOf(listOf(bedB, bedA, bedC))

        val state = viewModel().uiState.value

        assertFalse(state.loading)
        assertTrue(state.hasActiveHostel)
        assertEquals(listOf("A", "B", "C"), state.items.map { it.bedLabel })
        assertEquals(listOf("B", "C"), state.filteredItems.map { it.bedLabel })
        assertEquals("101", state.items.first().roomNumber)
        assertEquals("Ground", state.items.first().floorLabel)
    }

    @Test
    fun `the status filter switches the same list between vacant, occupied and all`() {
        val floor1 = Floor(id = "f1", hostelId = "h1", label = "Ground", sortOrder = 0, createdAt = 0L, updatedAt = 0L)
        val room1 = Room(id = "r1", floorId = "f1", roomNumber = "101", type = RoomType.DOUBLE, pricePerBed = 3000.0, amenities = "", createdAt = 0L, updatedAt = 0L)
        every { hostelPreferences.selectedHostelId } returns flowOf("h1")
        every { floorRepository.getByHostelId("h1") } returns flowOf(listOf(floor1))
        every { roomRepository.getByFloorId("f1") } returns flowOf(listOf(room1))
        every { bedRepository.getByRoomId("r1") } returns flowOf(
            listOf(
                Bed(id = "b1", roomId = "r1", label = "A", status = BedStatus.OCCUPIED, createdAt = 0L, updatedAt = 0L),
                Bed(id = "b2", roomId = "r1", label = "B", status = BedStatus.VACANT, createdAt = 0L, updatedAt = 0L),
            ),
        )

        val viewModel = viewModel()

        assertEquals(listOf("B"), viewModel.uiState.value.filteredItems.map { it.bedLabel })

        viewModel.onStatusFilterChange(BedFilter.OCCUPIED)
        assertEquals(listOf("A"), viewModel.uiState.value.filteredItems.map { it.bedLabel })

        viewModel.onStatusFilterChange(BedFilter.ALL)
        assertEquals(listOf("A", "B"), viewModel.uiState.value.filteredItems.map { it.bedLabel })
    }

    @Test
    fun `a hostel with floors but no rooms stops loading with an empty list`() {
        val floor1 = Floor(id = "f1", hostelId = "h1", label = "Ground", sortOrder = 0, createdAt = 0L, updatedAt = 0L)
        every { hostelPreferences.selectedHostelId } returns flowOf("h1")
        every { floorRepository.getByHostelId("h1") } returns flowOf(listOf(floor1))
        every { roomRepository.getByFloorId("f1") } returns flowOf(emptyList())

        val state = viewModel().uiState.value

        assertFalse(state.loading)
        assertTrue(state.hasActiveHostel)
        assertTrue(state.items.isEmpty())
    }

    @Test
    fun `onFloorFilterChange narrows filteredItems to the selected floor`() {
        every { hostelPreferences.selectedHostelId } returns flowOf(null)
        val viewModel = viewModel()

        val itemsField = VacantViewUiState(
            items = listOf(
                VacantBedItem("f1", "Ground", "101", RoomType.SINGLE, "A", BedStatus.VACANT),
                VacantBedItem("f2", "First", "201", RoomType.SINGLE, "A", BedStatus.VACANT),
            ),
        )
        assertEquals(2, itemsField.filteredItems.size)
        assertEquals(1, itemsField.copy(selectedFloorId = "f1").filteredItems.size)

        viewModel.onFloorFilterChange("f1")
        assertEquals("f1", viewModel.uiState.value.selectedFloorId)
    }
}
