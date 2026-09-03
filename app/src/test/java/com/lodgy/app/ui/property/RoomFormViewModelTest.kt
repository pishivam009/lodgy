package com.lodgy.app.ui.property

import androidx.lifecycle.SavedStateHandle
import com.lodgy.app.data.entity.Room
import com.lodgy.app.data.entity.RoomType
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.RoomRepository
import com.lodgy.app.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RoomFormViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val roomRepository: RoomRepository = mockk()
    private val bedRepository: BedRepository = mockk()

    private fun viewModel(roomId: String? = null) = RoomFormViewModel(
        roomRepository, bedRepository,
        SavedStateHandle(mapOf<String, Any?>("floorId" to "floor-1", "roomId" to roomId).filterValues { it != null }),
    )

    @Test
    fun `creating a new room starts blank`() {
        val state = viewModel().uiState.value
        assertFalse(state.isEditing)
        assertEquals("", state.roomNumber)
    }

    @Test
    fun `editing an existing room preloads its fields`() {
        val room = Room(id = "r1", floorId = "floor-1", roomNumber = "101", type = RoomType.DOUBLE, pricePerBed = 3500.0, amenities = "AC", createdAt = 0L, updatedAt = 0L)
        coEvery { roomRepository.getById("r1") } returns room

        val state = viewModel("r1").uiState.value

        assertTrue(state.isEditing)
        assertEquals("101", state.roomNumber)
        assertEquals(RoomType.DOUBLE, state.type)
        assertEquals("3500.0", state.pricePerBed)
        assertEquals("AC", state.amenities)
    }

    @Test
    fun `canSave requires a room number and a numeric price`() {
        val viewModel = viewModel()
        assertFalse(viewModel.uiState.value.canSave)
        viewModel.onRoomNumberChange("101")
        assertFalse(viewModel.uiState.value.canSave)
        viewModel.onPriceChange("abc")
        assertFalse(viewModel.uiState.value.canSave)
        viewModel.onPriceChange("3000")
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun `saving a new SINGLE room creates it and generates exactly one bed`() {
        val created = Room(id = "r1", floorId = "floor-1", roomNumber = "101", type = RoomType.SINGLE, pricePerBed = 3000.0, amenities = "", createdAt = 0L, updatedAt = 0L)
        coEvery { roomRepository.create("floor-1", "101", RoomType.SINGLE, 3000.0, "") } returns created
        coEvery { bedRepository.generateForRoom("r1", 1) } returns Unit

        val viewModel = viewModel()
        viewModel.onRoomNumberChange("101")
        viewModel.onPriceChange("3000")
        viewModel.save()

        coVerify { bedRepository.generateForRoom("r1", 1) }
        assertTrue(viewModel.uiState.value.saved)
    }

    @Test
    fun `saving a new TRIPLE room generates three beds`() {
        val created = Room(id = "r2", floorId = "floor-1", roomNumber = "102", type = RoomType.TRIPLE, pricePerBed = 1500.0, amenities = "", createdAt = 0L, updatedAt = 0L)
        coEvery { roomRepository.create("floor-1", "102", RoomType.TRIPLE, 1500.0, "") } returns created
        coEvery { bedRepository.generateForRoom("r2", 3) } returns Unit

        val viewModel = viewModel()
        viewModel.onRoomNumberChange("102")
        viewModel.onTypeChange(RoomType.TRIPLE)
        viewModel.onPriceChange("1500")
        viewModel.save()

        coVerify { bedRepository.generateForRoom("r2", 3) }
    }

    @Test
    fun `saving an edit updates the room and does not touch beds`() {
        val room = Room(id = "r1", floorId = "floor-1", roomNumber = "101", type = RoomType.DOUBLE, pricePerBed = 3500.0, amenities = "AC", createdAt = 0L, updatedAt = 0L)
        coEvery { roomRepository.getById("r1") } returns room
        coEvery { roomRepository.update(room, "101", RoomType.DOUBLE, 4000.0, "AC") } returns Unit

        val viewModel = viewModel("r1")
        viewModel.onPriceChange("4000")
        viewModel.save()

        coVerify { roomRepository.update(room, "101", RoomType.DOUBLE, 4000.0, "AC") }
        coVerify(exactly = 0) { bedRepository.generateForRoom(any(), any()) }
    }

    @Test
    fun `changing the type of a room with an occupied bed asks for confirmation first`() {
        val room = Room(id = "r1", floorId = "floor-1", roomNumber = "101", type = RoomType.DOUBLE, pricePerBed = 3500.0, amenities = "AC", createdAt = 0L, updatedAt = 0L)
        coEvery { roomRepository.getById("r1") } returns room
        coEvery { bedRepository.hasOccupiedBed("r1") } returns true

        val viewModel = viewModel("r1")
        viewModel.onTypeChange(RoomType.TRIPLE)
        viewModel.save()

        assertTrue(viewModel.uiState.value.showTypeChangeConfirm)
        assertFalse(viewModel.uiState.value.saved)
        coVerify(exactly = 0) { roomRepository.update(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `confirmTypeChange applies the pending type change`() {
        val room = Room(id = "r1", floorId = "floor-1", roomNumber = "101", type = RoomType.DOUBLE, pricePerBed = 3500.0, amenities = "AC", createdAt = 0L, updatedAt = 0L)
        coEvery { roomRepository.getById("r1") } returns room
        coEvery { bedRepository.hasOccupiedBed("r1") } returns true
        coEvery { roomRepository.update(room, "101", RoomType.TRIPLE, 3500.0, "AC") } returns Unit

        val viewModel = viewModel("r1")
        viewModel.onTypeChange(RoomType.TRIPLE)
        viewModel.save()
        viewModel.confirmTypeChange()

        coVerify { roomRepository.update(room, "101", RoomType.TRIPLE, 3500.0, "AC") }
        assertFalse(viewModel.uiState.value.showTypeChangeConfirm)
        assertTrue(viewModel.uiState.value.saved)
    }

    @Test
    fun `dismissTypeChangeConfirm abandons the save`() {
        val room = Room(id = "r1", floorId = "floor-1", roomNumber = "101", type = RoomType.DOUBLE, pricePerBed = 3500.0, amenities = "AC", createdAt = 0L, updatedAt = 0L)
        coEvery { roomRepository.getById("r1") } returns room
        coEvery { bedRepository.hasOccupiedBed("r1") } returns true

        val viewModel = viewModel("r1")
        viewModel.onTypeChange(RoomType.TRIPLE)
        viewModel.save()
        viewModel.dismissTypeChangeConfirm()

        assertFalse(viewModel.uiState.value.showTypeChangeConfirm)
        assertFalse(viewModel.uiState.value.saved)
        coVerify(exactly = 0) { roomRepository.update(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `changing the type of a room with no occupied bed saves without a prompt`() {
        val room = Room(id = "r1", floorId = "floor-1", roomNumber = "101", type = RoomType.DOUBLE, pricePerBed = 3500.0, amenities = "AC", createdAt = 0L, updatedAt = 0L)
        coEvery { roomRepository.getById("r1") } returns room
        coEvery { bedRepository.hasOccupiedBed("r1") } returns false
        coEvery { roomRepository.update(room, "101", RoomType.SINGLE, 3500.0, "AC") } returns Unit

        val viewModel = viewModel("r1")
        viewModel.onTypeChange(RoomType.SINGLE)
        viewModel.save()

        assertFalse(viewModel.uiState.value.showTypeChangeConfirm)
        coVerify { roomRepository.update(room, "101", RoomType.SINGLE, 3500.0, "AC") }
    }

    @Test
    fun `save is a no-op when the room number is blank`() {
        val viewModel = viewModel()
        viewModel.onPriceChange("3000")

        viewModel.save()

        coVerify(exactly = 0) { roomRepository.create(any(), any(), any(), any(), any()) }
    }
}
