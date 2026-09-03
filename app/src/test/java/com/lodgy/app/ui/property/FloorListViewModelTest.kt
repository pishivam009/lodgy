package com.lodgy.app.ui.property

import androidx.lifecycle.SavedStateHandle
import com.lodgy.app.data.entity.Floor
import com.lodgy.app.data.entity.Hostel
import com.lodgy.app.data.repository.FloorRepository
import com.lodgy.app.data.repository.HostelRepository
import com.lodgy.app.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FloorListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val floorRepository: FloorRepository = mockk()
    private val hostelRepository: HostelRepository = mockk()

    private fun viewModel(floors: List<Floor>): FloorListViewModel {
        coEvery { hostelRepository.getById("h1") } returns Hostel(id = "h1", wardenId = "w1", name = "Sunrise", address = "", contactPhone = "", createdAt = 0L, updatedAt = 0L)
        every { floorRepository.getByHostelId("h1") } returns flowOf(floors)
        return FloorListViewModel(floorRepository, hostelRepository, SavedStateHandle(mapOf("hostelId" to "h1")))
    }

    @Test
    fun `loads the hostel name and floors sorted by sort order`() {
        val f1 = Floor(id = "f1", hostelId = "h1", label = "First", sortOrder = 1, createdAt = 0L, updatedAt = 0L)
        val f0 = Floor(id = "f0", hostelId = "h1", label = "Ground", sortOrder = 0, createdAt = 0L, updatedAt = 0L)

        val state = viewModel(listOf(f1, f0)).uiState.value

        assertEquals("Sunrise", state.hostelName)
        assertEquals(listOf("Ground", "First"), state.floors.map { it.label })
    }

    @Test
    fun `moveUp, moveDown and delete delegate to the repository`() {
        val f0 = Floor(id = "f0", hostelId = "h1", label = "Ground", sortOrder = 0, createdAt = 0L, updatedAt = 0L)
        coEvery { floorRepository.moveUp(f0, "h1") } returns Unit
        coEvery { floorRepository.moveDown(f0, "h1") } returns Unit
        coEvery { floorRepository.delete(f0) } returns Unit

        val viewModel = viewModel(listOf(f0))
        viewModel.moveUp(f0)
        viewModel.moveDown(f0)
        viewModel.delete(f0)

        coVerify { floorRepository.moveUp(f0, "h1") }
        coVerify { floorRepository.moveDown(f0, "h1") }
        coVerify { floorRepository.delete(f0) }
    }
}
