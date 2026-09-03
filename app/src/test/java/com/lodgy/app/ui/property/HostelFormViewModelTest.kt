package com.lodgy.app.ui.property

import androidx.lifecycle.SavedStateHandle
import com.lodgy.app.data.entity.Hostel
import com.lodgy.app.data.entity.Warden
import com.lodgy.app.data.repository.HostelRepository
import com.lodgy.app.data.repository.WardenRepository
import com.lodgy.app.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HostelFormViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val hostelRepository: HostelRepository = mockk()
    private val wardenRepository: WardenRepository = mockk()

    private fun viewModel(hostelId: String? = null) = HostelFormViewModel(
        hostelRepository, wardenRepository,
        SavedStateHandle(mapOf<String, Any?>("hostelId" to hostelId).filterValues { it != null }),
    )

    @Test
    fun `editing an existing hostel preloads its fields`() {
        val hostel = Hostel(id = "h1", wardenId = "w1", name = "Sunrise", address = "MG Road", contactPhone = "999", createdAt = 0L, updatedAt = 0L)
        coEvery { hostelRepository.getById("h1") } returns hostel

        val state = viewModel("h1").uiState.value

        assertTrue(state.isEditing)
        assertEquals("Sunrise", state.name)
        assertEquals("MG Road", state.address)
    }

    @Test
    fun `save creates a new hostel under the current warden`() {
        val warden = Warden(id = "w1", pinHash = "x", name = "Warden", createdAt = 0L, updatedAt = 0L)
        coEvery { wardenRepository.getWarden() } returns warden
        coEvery { hostelRepository.create("w1", "Sunrise", "", "") } returns mockk()

        val viewModel = viewModel()
        viewModel.onNameChange("Sunrise")
        viewModel.save()

        coVerify { hostelRepository.create("w1", "Sunrise", "", "") }
        assertTrue(viewModel.uiState.value.saved)
    }

    @Test
    fun `save updates an existing hostel instead of creating one`() {
        val hostel = Hostel(id = "h1", wardenId = "w1", name = "Old", address = "A", contactPhone = "1", createdAt = 0L, updatedAt = 0L)
        coEvery { hostelRepository.getById("h1") } returns hostel
        coEvery { hostelRepository.update(hostel, "New", "A", "1") } returns Unit

        val viewModel = viewModel("h1")
        viewModel.onNameChange("New")
        viewModel.save()

        coVerify { hostelRepository.update(hostel, "New", "A", "1") }
        coVerify(exactly = 0) { hostelRepository.create(any(), any(), any(), any()) }
    }

    @Test
    fun `save does nothing when there is no warden yet`() {
        coEvery { wardenRepository.getWarden() } returns null

        val viewModel = viewModel()
        viewModel.onNameChange("Sunrise")
        viewModel.save()

        coVerify(exactly = 0) { hostelRepository.create(any(), any(), any(), any()) }
        assertFalse(viewModel.uiState.value.saved)
    }

    @Test
    fun `save does nothing when the name is blank`() {
        val viewModel = viewModel()

        viewModel.save()

        coVerify(exactly = 0) { hostelRepository.create(any(), any(), any(), any()) }
    }
}
