package com.lodgy.app.ui.property

import androidx.lifecycle.SavedStateHandle
import com.lodgy.app.data.entity.Floor
import com.lodgy.app.data.repository.FloorRepository
import com.lodgy.app.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FloorFormViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val floorRepository: FloorRepository = mockk()

    private fun viewModel(floorId: String? = null) = FloorFormViewModel(
        floorRepository,
        SavedStateHandle(mapOf<String, Any?>("hostelId" to "h1", "floorId" to floorId).filterValues { it != null }),
    )

    @Test
    fun `editing an existing floor preloads its label`() {
        val floor = Floor(id = "f1", hostelId = "h1", label = "Ground", sortOrder = 0, createdAt = 0L, updatedAt = 0L)
        coEvery { floorRepository.getById("f1") } returns floor

        val state = viewModel("f1").uiState.value

        assertTrue(state.isEditing)
        assertEquals("Ground", state.label)
    }

    @Test
    fun `save creates a new floor under the hostel`() {
        coEvery { floorRepository.create("h1", "First") } returns mockk()

        val viewModel = viewModel()
        viewModel.onLabelChange("First")
        viewModel.save()

        coVerify { floorRepository.create("h1", "First") }
        assertTrue(viewModel.uiState.value.saved)
    }

    @Test
    fun `save renames an existing floor instead of creating one`() {
        val floor = Floor(id = "f1", hostelId = "h1", label = "Old", sortOrder = 0, createdAt = 0L, updatedAt = 0L)
        coEvery { floorRepository.getById("f1") } returns floor
        coEvery { floorRepository.rename(floor, "New") } returns Unit

        val viewModel = viewModel("f1")
        viewModel.onLabelChange("New")
        viewModel.save()

        coVerify { floorRepository.rename(floor, "New") }
        coVerify(exactly = 0) { floorRepository.create(any(), any()) }
    }

    @Test
    fun `save does nothing when the label is blank`() {
        val viewModel = viewModel()

        viewModel.save()

        coVerify(exactly = 0) { floorRepository.create(any(), any()) }
    }
}
