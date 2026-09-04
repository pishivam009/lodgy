package com.lodgy.app.ui.property

import androidx.lifecycle.SavedStateHandle
import com.lodgy.app.data.dao.FloorOccupancy
import com.lodgy.app.data.entity.Floor
import com.lodgy.app.data.entity.Hostel
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.FloorRepository
import com.lodgy.app.data.repository.HostelRepository
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

class FloorListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val floorRepository: FloorRepository = mockk()
    private val hostelRepository: HostelRepository = mockk()
    private val bedRepository: BedRepository = mockk()

    private fun viewModel(floors: List<Floor>, occupancy: List<FloorOccupancy> = emptyList()): FloorListViewModel {
        coEvery { hostelRepository.getById("h1") } returns Hostel(id = "h1", wardenId = "w1", name = "Sunrise", address = "", contactPhone = "", createdAt = 0L, updatedAt = 0L)
        every { floorRepository.getByHostelId("h1") } returns flowOf(floors)
        every { bedRepository.observeOccupancyByHostel("h1") } returns flowOf(occupancy)
        return FloorListViewModel(floorRepository, bedRepository, hostelRepository, SavedStateHandle(mapOf("hostelId" to "h1")))
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

        coEvery { bedRepository.activeTenantNamesOnFloor("f0") } returns emptyList()

        val viewModel = viewModel(listOf(f0))
        viewModel.moveUp(f0)
        viewModel.moveDown(f0)
        viewModel.requestDelete(f0)
        viewModel.confirmDelete()

        coVerify { floorRepository.moveUp(f0, "h1") }
        coVerify { floorRepository.moveDown(f0, "h1") }
        coVerify { floorRepository.delete(f0) }
    }

    @Test
    fun `each floor card carries its own bed counts`() {
        val ground = Floor(id = "f0", hostelId = "h1", label = "Ground", sortOrder = 0, createdAt = 0L, updatedAt = 0L)
        val first = Floor(id = "f1", hostelId = "h1", label = "First", sortOrder = 1, createdAt = 0L, updatedAt = 0L)

        val state = viewModel(
            floors = listOf(ground, first),
            occupancy = listOf(FloorOccupancy("f0", totalBeds = 6, occupiedBeds = 4)),
        ).uiState.value

        assertEquals(2, state.items.first().vacantBeds)
        assertEquals(4, state.items.first().occupiedBeds)
    }

    @Test
    fun `a floor with no rooms or beds yet reports zeroes rather than dropping off the list`() {
        val empty = Floor(id = "f9", hostelId = "h1", label = "Terrace", sortOrder = 9, createdAt = 0L, updatedAt = 0L)

        val state = viewModel(listOf(empty)).uiState.value

        assertEquals(listOf("Terrace"), state.floors.map { it.label })
        assertEquals(0, state.items.single().totalBeds)
        assertEquals(0, state.items.single().vacantBeds)
    }

    /** The crash this ticket exists for: deleting a floor whose bed still has an ACTIVE tenancy
     *  used to reach the DAO and die on a FOREIGN KEY constraint. It must not reach the DAO. */
    @Test
    fun `a floor with a tenant on it is blocked, and never reaches the repository`() {
        val f0 = Floor(id = "f0", hostelId = "h1", label = "Ground", sortOrder = 0, createdAt = 0L, updatedAt = 0L)
        coEvery { bedRepository.activeTenantNamesOnFloor("f0") } returns listOf("Ramesh Kumar")

        val viewModel = viewModel(listOf(f0))
        viewModel.requestDelete(f0)

        assertEquals(f0, viewModel.uiState.value.blockedDelete?.floor)
        assertNull(viewModel.uiState.value.pendingDeleteFloor)
        coVerify(exactly = 0) { floorRepository.delete(any()) }
    }

    @Test
    fun `the block names every tenant in the way, so the warden knows who to check out`() {
        val f0 = Floor(id = "f0", hostelId = "h1", label = "Ground", sortOrder = 0, createdAt = 0L, updatedAt = 0L)
        coEvery { bedRepository.activeTenantNamesOnFloor("f0") } returns listOf("Priya", "Suresh")

        val viewModel = viewModel(listOf(f0))
        viewModel.requestDelete(f0)

        assertEquals(listOf("Priya", "Suresh"), viewModel.uiState.value.blockedDelete?.tenantNames)
    }

    @Test
    fun `an empty floor is confirmed rather than blocked`() {
        val f0 = Floor(id = "f0", hostelId = "h1", label = "Ground", sortOrder = 0, createdAt = 0L, updatedAt = 0L)
        coEvery { bedRepository.activeTenantNamesOnFloor("f0") } returns emptyList()

        val viewModel = viewModel(listOf(f0))
        viewModel.requestDelete(f0)

        assertEquals(f0, viewModel.uiState.value.pendingDeleteFloor)
        assertNull(viewModel.uiState.value.blockedDelete)
        coVerify(exactly = 0) { floorRepository.delete(any()) }
    }

    /** Confirming is the only thing that deletes - requesting alone must not. */
    @Test
    fun `nothing is deleted until the warden confirms`() {
        val f0 = Floor(id = "f0", hostelId = "h1", label = "Ground", sortOrder = 0, createdAt = 0L, updatedAt = 0L)
        coEvery { bedRepository.activeTenantNamesOnFloor("f0") } returns emptyList()
        coEvery { floorRepository.delete(f0) } returns Unit

        val viewModel = viewModel(listOf(f0))
        viewModel.requestDelete(f0)
        coVerify(exactly = 0) { floorRepository.delete(f0) }

        viewModel.confirmDelete()
        coVerify(exactly = 1) { floorRepository.delete(f0) }
        assertNull(viewModel.uiState.value.pendingDeleteFloor)
    }

    @Test
    fun `cancelling a pending delete leaves the floor alone`() {
        val f0 = Floor(id = "f0", hostelId = "h1", label = "Ground", sortOrder = 0, createdAt = 0L, updatedAt = 0L)
        coEvery { bedRepository.activeTenantNamesOnFloor("f0") } returns emptyList()

        val viewModel = viewModel(listOf(f0))
        viewModel.requestDelete(f0)
        viewModel.dismissPendingDelete()

        assertNull(viewModel.uiState.value.pendingDeleteFloor)
        coVerify(exactly = 0) { floorRepository.delete(any()) }
    }

    @Test
    fun `dismissing the block clears it`() {
        val f0 = Floor(id = "f0", hostelId = "h1", label = "Ground", sortOrder = 0, createdAt = 0L, updatedAt = 0L)
        coEvery { bedRepository.activeTenantNamesOnFloor("f0") } returns listOf("Ramesh Kumar")

        val viewModel = viewModel(listOf(f0))
        viewModel.requestDelete(f0)
        viewModel.dismissBlockedDelete()

        assertNull(viewModel.uiState.value.blockedDelete)
    }
}
