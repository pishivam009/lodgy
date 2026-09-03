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
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BulkRoomFormViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val roomRepository: RoomRepository = mockk()
    private val bedRepository: BedRepository = mockk()

    private fun viewModel() = BulkRoomFormViewModel(
        roomRepository,
        bedRepository,
        SavedStateHandle(mapOf("floorId" to "floor-1")),
    )

    @Test
    fun `creating 3 rooms numbers them sequentially from the starting number`() = runTest {
        val createdRoomNumbers = mutableListOf<String>()
        coEvery { roomRepository.create("floor-1", capture(createdRoomNumbers), any(), any(), any()) } answers {
            Room(floorId = "floor-1", roomNumber = createdRoomNumbers.last(), type = RoomType.SINGLE, pricePerBed = 3000.0, amenities = "", createdAt = 0L, updatedAt = 0L)
        }
        coEvery { bedRepository.generateForRoom(any(), any()) } returns Unit

        val viewModel = viewModel()
        viewModel.onStartNumberChange("101")
        viewModel.onCountChange("3")
        viewModel.onPriceChange("3000")
        viewModel.save()

        assertEquals(listOf("101", "102", "103"), createdRoomNumbers)
    }

    @Test
    fun `each generated room gets the bed count matching its type`() = runTest {
        val bedCounts = mutableListOf<Int>()
        var roomIndex = 0
        coEvery { roomRepository.create(any(), any(), any(), any(), any()) } answers {
            Room(floorId = "floor-1", roomNumber = "10${roomIndex++}", type = RoomType.TRIPLE, pricePerBed = 1000.0, amenities = "", createdAt = 0L, updatedAt = 0L)
        }
        coEvery { bedRepository.generateForRoom(any(), capture(bedCounts)) } returns Unit

        val viewModel = viewModel()
        viewModel.onStartNumberChange("200")
        viewModel.onCountChange("2")
        viewModel.onTypeChange(RoomType.TRIPLE)
        viewModel.onPriceChange("1000")
        viewModel.save()

        assertEquals(listOf(3, 3), bedCounts)
        coVerify(exactly = 2) { roomRepository.create(any(), any(), RoomType.TRIPLE, any(), any()) }
    }

    @Test
    fun `cannot save without a valid starting number`() {
        val state = BulkRoomFormUiState(startNumber = "", count = "3", pricePerBed = "1000")
        assertFalse(state.canSave)
    }

    @Test
    fun `cannot save with a zero or negative count`() {
        assertFalse(BulkRoomFormUiState(startNumber = "101", count = "0", pricePerBed = "1000").canSave)
        assertFalse(BulkRoomFormUiState(startNumber = "101", count = "-2", pricePerBed = "1000").canSave)
    }

    @Test
    fun `cannot save without a valid price`() {
        assertFalse(BulkRoomFormUiState(startNumber = "101", count = "3", pricePerBed = "").canSave)
    }

    @Test
    fun `can save once every field is valid`() {
        assertTrue(BulkRoomFormUiState(startNumber = "101", count = "3", pricePerBed = "1500").canSave)
    }
}
