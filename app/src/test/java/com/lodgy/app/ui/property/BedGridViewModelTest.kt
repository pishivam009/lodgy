package com.lodgy.app.ui.property

import androidx.lifecycle.SavedStateHandle
import com.lodgy.app.data.entity.Bed
import com.lodgy.app.data.entity.BedStatus
import com.lodgy.app.data.entity.Room
import com.lodgy.app.data.entity.RoomType
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.RoomRepository
import com.lodgy.app.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BedGridViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val bedRepository: BedRepository = mockk()
    private val roomRepository: RoomRepository = mockk()

    @Test
    fun `loads the room number and type, and beds sorted by label`() {
        coEvery { roomRepository.getById("r1") } returns Room(id = "r1", floorId = "f1", roomNumber = "101", type = RoomType.DOUBLE, pricePerBed = 3000.0, amenities = "", createdAt = 0L, updatedAt = 0L)
        val bedB = Bed(id = "b2", roomId = "r1", label = "B", status = BedStatus.VACANT, createdAt = 0L, updatedAt = 0L)
        val bedA = Bed(id = "b1", roomId = "r1", label = "A", status = BedStatus.OCCUPIED, createdAt = 0L, updatedAt = 0L)
        every { bedRepository.getByRoomId("r1") } returns flowOf(listOf(bedB, bedA))

        val viewModel = BedGridViewModel(bedRepository, roomRepository, SavedStateHandle(mapOf("roomId" to "r1")))
        val state = viewModel.uiState.value

        assertEquals("101", state.roomNumber)
        assertEquals("DOUBLE", state.roomType)
        assertEquals(listOf("A", "B"), state.beds.map { it.label })
    }
}
