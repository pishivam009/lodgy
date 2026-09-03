package com.lodgy.app.ui.property

import androidx.lifecycle.SavedStateHandle
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

    private fun viewModel(): RoomListViewModel {
        coEvery { floorRepository.getById("f1") } returns Floor(id = "f1", hostelId = "h1", label = "Ground", sortOrder = 0, createdAt = 0L, updatedAt = 0L)
        every { roomRepository.getByFloorId("f1") } returns flowOf(listOf(room))
        return RoomListViewModel(roomRepository, bedRepository, floorRepository, SavedStateHandle(mapOf("floorId" to "f1")))
    }

    @Test
    fun `loads the floor label and its rooms`() {
        val state = viewModel().uiState.value
        assertEquals("Ground", state.floorLabel)
        assertEquals(listOf(room), state.rooms)
    }

    @Test
    fun `requestDelete deletes a room with no occupied beds`() {
        coEvery { bedRepository.hasOccupiedBed("r1") } returns false
        coEvery { roomRepository.delete(room) } returns Unit

        val viewModel = viewModel()
        viewModel.requestDelete(room)

        coVerify { roomRepository.delete(room) }
        assertNull(viewModel.uiState.value.blockedDeleteRoom)
    }

    @Test
    fun `requestDelete blocks deletion when the room still has an occupied bed`() {
        coEvery { bedRepository.hasOccupiedBed("r1") } returns true

        val viewModel = viewModel()
        viewModel.requestDelete(room)

        coVerify(exactly = 0) { roomRepository.delete(any()) }
        assertEquals(room, viewModel.uiState.value.blockedDeleteRoom)
    }

    @Test
    fun `dismissBlockedDelete clears the blocked room`() {
        coEvery { bedRepository.hasOccupiedBed("r1") } returns true
        val viewModel = viewModel()
        viewModel.requestDelete(room)

        viewModel.dismissBlockedDelete()

        assertNull(viewModel.uiState.value.blockedDeleteRoom)
    }
}
