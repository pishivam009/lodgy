package com.lodgy.app.ui.property

import androidx.lifecycle.SavedStateHandle
import com.lodgy.app.data.dao.RoomOccupancy
import com.lodgy.app.data.dao.RoomWithFloor
import com.lodgy.app.data.entity.Hostel
import com.lodgy.app.data.entity.RoomType
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.HostelRepository
import com.lodgy.app.data.repository.RoomRepository
import com.lodgy.app.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AllRoomsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val roomRepository: RoomRepository = mockk()
    private val bedRepository: BedRepository = mockk()
    private val hostelRepository: HostelRepository = mockk()

    private fun room(id: String, number: String, floor: String) =
        RoomWithFloor(roomId = id, roomNumber = number, type = RoomType.DOUBLE, pricePerBed = 3000.0, floorId = "f-$floor", floorLabel = floor)

    private fun viewModel(
        rooms: List<RoomWithFloor>,
        occupancy: List<RoomOccupancy> = emptyList(),
    ): AllRoomsViewModel {
        coEvery { hostelRepository.getById("h1") } returns Hostel(id = "h1", wardenId = "w1", name = "Sunrise", address = "", contactPhone = "", createdAt = 0L, updatedAt = 0L)
        every { roomRepository.getByHostelIdWithFloor("h1") } returns flowOf(rooms)
        every { bedRepository.observeRoomOccupancyByHostel("h1") } returns flowOf(occupancy)
        return AllRoomsViewModel(roomRepository, bedRepository, hostelRepository, SavedStateHandle(mapOf("hostelId" to "h1")))
    }

    @Test
    fun `lists every room across floors, each labelled with its floor`() {
        val state = viewModel(listOf(room("r1", "101", "Ground"), room("r2", "201", "First"))).uiState.value

        assertEquals("Sunrise", state.hostelName)
        assertEquals(listOf("Ground", "First"), state.items.map { it.room.floorLabel })
        assertEquals(listOf("101", "201"), state.items.map { it.room.roomNumber })
    }

    @Test
    fun `bed counts attach to their room, and a room with no beds reports zero`() {
        val state = viewModel(
            rooms = listOf(room("r1", "101", "Ground"), room("r2", "102", "Ground")),
            occupancy = listOf(RoomOccupancy("r1", totalBeds = 3, occupiedBeds = 1)),
        ).uiState.value

        assertEquals(2, state.items.first().vacantBeds)
        assertEquals(0, state.items.last().totalBeds)
    }
}
