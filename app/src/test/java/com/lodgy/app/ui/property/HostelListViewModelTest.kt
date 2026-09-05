package com.lodgy.app.ui.property

import com.lodgy.app.data.entity.PropertyType
import com.lodgy.app.data.repository.RoomRepository
import kotlinx.coroutines.test.runTest
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
    private val roomRepository: RoomRepository = mockk()

    private fun hostel(id: String) = Hostel(id = id, wardenId = "w1", name = id, address = "", contactPhone = "", createdAt = 0L, updatedAt = 0L)

    /** The list query runs in init and is irrelevant to the navigation tests below. */
    private fun stubEmptyList() {
        coEvery { wardenRepository.getWarden() } returns Warden(id = "w1", pinHash = "x", name = "Warden", createdAt = 0L, updatedAt = 0L)
        every { hostelRepository.getByWardenId("w1") } returns flowOf(emptyList())
        every { hostelPreferences.selectedHostelId } returns flowOf(null)
    }

    @Test
    fun `no warden yet leaves the list empty`() {
        coEvery { wardenRepository.getWarden() } returns null

        val state = HostelListViewModel(wardenRepository, hostelRepository, hostelPreferences, roomRepository).uiState.value

        assertTrue(state.hostels.isEmpty())
    }

    @Test
    fun `defaults the selection to the first hostel when none is explicitly selected`() {
        coEvery { wardenRepository.getWarden() } returns Warden(id = "w1", pinHash = "x", name = "Warden", createdAt = 0L, updatedAt = 0L)
        every { hostelRepository.getByWardenId("w1") } returns flowOf(listOf(hostel("h1"), hostel("h2")))
        every { hostelPreferences.selectedHostelId } returns flowOf(null)

        val state = HostelListViewModel(wardenRepository, hostelRepository, hostelPreferences, roomRepository).uiState.value

        assertEquals("h1", state.selectedHostelId)
        assertEquals(2, state.hostels.size)
    }

    @Test
    fun `an explicit selection is preferred over the default`() {
        coEvery { wardenRepository.getWarden() } returns Warden(id = "w1", pinHash = "x", name = "Warden", createdAt = 0L, updatedAt = 0L)
        every { hostelRepository.getByWardenId("w1") } returns flowOf(listOf(hostel("h1"), hostel("h2")))
        every { hostelPreferences.selectedHostelId } returns flowOf("h2")

        val state = HostelListViewModel(wardenRepository, hostelRepository, hostelPreferences, roomRepository).uiState.value

        assertEquals("h2", state.selectedHostelId)
    }

    @Test
    fun `selectHostel persists the choice`() {
        coEvery { wardenRepository.getWarden() } returns null
        coEvery { hostelPreferences.setSelectedHostelId("h2") } returns Unit

        HostelListViewModel(wardenRepository, hostelRepository, hostelPreferences, roomRepository).selectHostel("h2")

        coVerify { hostelPreferences.setSelectedHostelId("h2") }
    }

    /** LODGY-79: a hostel opens its floors, but a shop or flat has none worth showing, so it goes
     *  straight to the unit the warden actually lets. */
    @Test
    fun `a single-unit property opens its unit, a hostel opens its floors`() = runTest {
        stubEmptyList()
        coEvery { roomRepository.getFirstRoomIdByHostel("h9") } returns "r9"
        val viewModel = HostelListViewModel(wardenRepository, hostelRepository, hostelPreferences, roomRepository)

        var destination: PropertyDestination? = null
        viewModel.openProperty(hostel("h9", PropertyType.SHOP)) { destination = it }
        assertEquals(PropertyDestination.Unit("r9"), destination)

        viewModel.openProperty(hostel("h1", PropertyType.HOSTEL)) { destination = it }
        assertEquals(PropertyDestination.Floors("h1"), destination)
    }

    /** A tap must never dead-end, so a single-unit property whose implicit room has somehow gone
     *  missing falls back to the floor list rather than navigating nowhere. */
    @Test
    fun `a single-unit property with no room falls back to the floor list`() = runTest {
        stubEmptyList()
        coEvery { roomRepository.getFirstRoomIdByHostel("h9") } returns null
        val viewModel = HostelListViewModel(wardenRepository, hostelRepository, hostelPreferences, roomRepository)

        var destination: PropertyDestination? = null
        viewModel.openProperty(hostel("h9", PropertyType.SHOP)) { destination = it }

        assertEquals(PropertyDestination.Floors("h9"), destination)
    }

    private fun hostel(id: String, type: PropertyType) = Hostel(
        id = id, wardenId = "w1", name = "P", address = "", contactPhone = "",
        propertyType = type, createdAt = 0L, updatedAt = 0L,
    )
}
