package com.lodgy.app.ui.property

import androidx.lifecycle.SavedStateHandle
import com.lodgy.app.data.dao.RoomOccupancy
import com.lodgy.app.data.entity.Floor
import com.lodgy.app.data.entity.Room
import com.lodgy.app.data.entity.RoomType
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.FloorRepository
import com.lodgy.app.data.repository.RoomRepository
import com.lodgy.app.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class RoomListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val roomRepository: RoomRepository = mockk()
    private val bedRepository: BedRepository = mockk()
    private val floorRepository: FloorRepository = mockk()

    private val room = Room(id = "r1", floorId = "f1", roomNumber = "101", type = RoomType.SINGLE, pricePerBed = 3000.0, amenities = "", createdAt = 0L, updatedAt = 0L)

    private fun viewModel(
        rooms: List<Room> = listOf(room),
        occupancy: List<RoomOccupancy> = listOf(RoomOccupancy("r1", totalBeds = 1, occupiedBeds = 0)),
    ): RoomListViewModel {
        coEvery { floorRepository.getById("f1") } returns Floor(id = "f1", hostelId = "h1", label = "Ground", sortOrder = 0, createdAt = 0L, updatedAt = 0L)
        every { roomRepository.getByFloorId("f1") } returns flowOf(rooms)
        every { bedRepository.observeOccupancyByFloor("f1") } returns flowOf(occupancy)
        return RoomListViewModel(roomRepository, bedRepository, floorRepository, SavedStateHandle(mapOf("floorId" to "f1")))
    }

    @Test
    fun `loads the floor label and its rooms with their bed counts`() {
        val state = viewModel().uiState.value
        assertEquals("Ground", state.floorLabel)
        assertEquals(listOf(room), state.items.map { it.room })
        assertEquals(1, state.items.single().vacantBeds)
    }

    @Test
    fun `a room with no beds yet still lists, reporting zero of zero`() {
        val state = viewModel(occupancy = emptyList()).uiState.value

        assertEquals(0, state.items.single().totalBeds)
        assertEquals(false, state.items.single().isFull)
    }

    @Test
    fun `the filter splits rooms into those with space and those that are full`() {
        val full = Room(id = "r2", floorId = "f1", roomNumber = "102", type = RoomType.SINGLE, pricePerBed = 3000.0, amenities = "", createdAt = 0L, updatedAt = 0L)
        val viewModel = viewModel(
            rooms = listOf(room, full),
            occupancy = listOf(
                RoomOccupancy("r1", totalBeds = 2, occupiedBeds = 1),
                RoomOccupancy("r2", totalBeds = 2, occupiedBeds = 2),
            ),
        )

        assertEquals(2, viewModel.uiState.value.filteredItems.size)

        viewModel.onFilterChange(RoomFilter.HAS_SPACE)
        assertEquals(listOf("101"), viewModel.uiState.value.filteredItems.map { it.room.roomNumber })

        viewModel.onFilterChange(RoomFilter.FULL)
        assertEquals(listOf("102"), viewModel.uiState.value.filteredItems.map { it.room.roomNumber })
    }

    @Test
    fun `requestDelete asks for confirmation instead of deleting a room outright`() {
        coEvery { bedRepository.activeTenantNamesInRoom("r1") } returns emptyList()

        val viewModel = viewModel()
        viewModel.requestDelete(room)

        coVerify(exactly = 0) { roomRepository.delete(any()) }
        assertEquals(room, viewModel.uiState.value.pendingDeleteRoom)
    }

    @Test
    fun `confirmDelete deletes the pending room and clears the prompt`() {
        coEvery { bedRepository.activeTenantNamesInRoom("r1") } returns emptyList()
        coEvery { roomRepository.delete(room) } returns Unit

        val viewModel = viewModel()
        viewModel.requestDelete(room)
        viewModel.confirmDelete()

        coVerify { roomRepository.delete(room) }
        assertNull(viewModel.uiState.value.pendingDeleteRoom)
    }

    @Test
    fun `dismissPendingDelete leaves the room untouched`() {
        coEvery { bedRepository.activeTenantNamesInRoom("r1") } returns emptyList()

        val viewModel = viewModel()
        viewModel.requestDelete(room)
        viewModel.dismissPendingDelete()

        coVerify(exactly = 0) { roomRepository.delete(any()) }
        assertNull(viewModel.uiState.value.pendingDeleteRoom)
    }

    @Test
    fun `confirmDelete does nothing when no delete is pending`() {
        val viewModel = viewModel()

        viewModel.confirmDelete()

        coVerify(exactly = 0) { roomRepository.delete(any()) }
    }

    @Test
    fun `requestDelete blocks deletion when the room still has an occupied bed`() {
        coEvery { bedRepository.activeTenantNamesInRoom("r1") } returns listOf("Ramesh Kumar")

        val viewModel = viewModel()
        viewModel.requestDelete(room)

        coVerify(exactly = 0) { roomRepository.delete(any()) }
        assertEquals(room, viewModel.uiState.value.blockedDeleteRoom?.room)
        assertEquals(listOf("Ramesh Kumar"), viewModel.uiState.value.blockedDeleteRoom?.tenantNames)
    }

    @Test
    fun `dismissBlockedDelete clears the blocked room`() {
        coEvery { bedRepository.activeTenantNamesInRoom("r1") } returns listOf("Ramesh Kumar")
        val viewModel = viewModel()
        viewModel.requestDelete(room)

        viewModel.dismissBlockedDelete()

        assertNull(viewModel.uiState.value.blockedDeleteRoom)
    }
}
