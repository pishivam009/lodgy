package com.lodgy.app.ui.property

import com.lodgy.app.data.entity.Hostel
import com.lodgy.app.data.entity.Warden
import com.lodgy.app.data.prefs.HostelPreferences
import com.lodgy.app.data.repository.HostelRepository
import com.lodgy.app.data.repository.WardenRepository
import com.lodgy.app.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HostelListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val wardenRepository: WardenRepository = mockk()
    private val hostelRepository: HostelRepository = mockk()
    private val hostelPreferences: HostelPreferences = mockk()

    private fun hostel(id: String) = Hostel(id = id, wardenId = "w1", name = id, address = "", contactPhone = "", createdAt = 0L, updatedAt = 0L)

    @Test
    fun `no warden yet leaves the list empty`() {
        coEvery { wardenRepository.getWarden() } returns null

        val state = HostelListViewModel(wardenRepository, hostelRepository, hostelPreferences).uiState.value

        assertTrue(state.hostels.isEmpty())
    }

    @Test
    fun `defaults the selection to the first hostel when none is explicitly selected`() {
        coEvery { wardenRepository.getWarden() } returns Warden(id = "w1", pinHash = "x", name = "Warden", createdAt = 0L, updatedAt = 0L)
        every { hostelRepository.getByWardenId("w1") } returns flowOf(listOf(hostel("h1"), hostel("h2")))
        every { hostelPreferences.selectedHostelId } returns flowOf(null)

        val state = HostelListViewModel(wardenRepository, hostelRepository, hostelPreferences).uiState.value

        assertEquals("h1", state.selectedHostelId)
        assertEquals(2, state.hostels.size)
    }

    @Test
    fun `an explicit selection is preferred over the default`() {
        coEvery { wardenRepository.getWarden() } returns Warden(id = "w1", pinHash = "x", name = "Warden", createdAt = 0L, updatedAt = 0L)
        every { hostelRepository.getByWardenId("w1") } returns flowOf(listOf(hostel("h1"), hostel("h2")))
        every { hostelPreferences.selectedHostelId } returns flowOf("h2")

        val state = HostelListViewModel(wardenRepository, hostelRepository, hostelPreferences).uiState.value

        assertEquals("h2", state.selectedHostelId)
    }

    @Test
    fun `selectHostel persists the choice`() {
        coEvery { wardenRepository.getWarden() } returns null
        coEvery { hostelPreferences.setSelectedHostelId("h2") } returns Unit

        HostelListViewModel(wardenRepository, hostelRepository, hostelPreferences).selectHostel("h2")

        coVerify { hostelPreferences.setSelectedHostelId("h2") }
    }
}
