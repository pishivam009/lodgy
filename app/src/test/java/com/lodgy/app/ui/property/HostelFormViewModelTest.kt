package com.lodgy.app.ui.property

import androidx.lifecycle.SavedStateHandle
import com.lodgy.app.data.entity.Floor
import com.lodgy.app.data.entity.PropertyType
import com.lodgy.app.data.entity.Room
import com.lodgy.app.data.entity.RoomType
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.FloorRepository
import com.lodgy.app.data.repository.RoomRepository
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
    private val floorRepository: FloorRepository = mockk()
    private val roomRepository: RoomRepository = mockk()
    private val bedRepository: BedRepository = mockk()

    private fun viewModel(hostelId: String? = null) = HostelFormViewModel(
        hostelRepository, wardenRepository, floorRepository, roomRepository, bedRepository,
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
        coEvery { hostelRepository.create("w1", "Sunrise", "", "", PropertyType.HOSTEL) } returns mockk()

        val viewModel = viewModel()
        viewModel.onNameChange("Sunrise")
        viewModel.save()

        coVerify { hostelRepository.create("w1", "Sunrise", "", "", PropertyType.HOSTEL) }
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

    /** LODGY-79. A warden letting a shop had to invent a floor and a bed to rent it at all. The
     *  hierarchy still exists underneath - as REAL rows, so every query, rollup and export is
     *  untouched - but the warden never has to build it. */
    @Test
    fun `creating a single-unit property builds its implicit floor, room and bed`() {
        val warden = Warden(id = "w1", pinHash = "x", name = "Warden", createdAt = 0L, updatedAt = 0L)
        val hostel = Hostel(id = "h9", wardenId = "w1", name = "Corner Shop", address = "", contactPhone = "", propertyType = PropertyType.SHOP, createdAt = 0L, updatedAt = 0L)
        val floor = Floor(id = "f9", hostelId = "h9", label = "-", sortOrder = 0, createdAt = 0L, updatedAt = 0L)
        val room = Room(id = "r9", floorId = "f9", roomNumber = "Corner Shop", type = RoomType.SINGLE, pricePerBed = 18000.0, amenities = "", createdAt = 0L, updatedAt = 0L)
        coEvery { wardenRepository.getWarden() } returns warden
        coEvery { hostelRepository.create("w1", "Corner Shop", "", "", PropertyType.SHOP) } returns hostel
        coEvery { floorRepository.create("h9", any()) } returns floor
        coEvery { roomRepository.create("f9", "Corner Shop", RoomType.SINGLE, 18000.0, "") } returns room
        coEvery { bedRepository.generateForRoom("r9", 1) } returns Unit

        val viewModel = viewModel()
        viewModel.onPropertyTypeChange(PropertyType.SHOP)
        viewModel.onNameChange("Corner Shop")
        viewModel.onMonthlyRentChange("18000")
        viewModel.save()

        coVerify { floorRepository.create("h9", any()) }
        coVerify { roomRepository.create("f9", "Corner Shop", RoomType.SINGLE, 18000.0, "") }
        // Exactly one bed: the property is let as a whole, so there is one thing to let.
        coVerify { bedRepository.generateForRoom("r9", 1) }
    }

    /** A hostel is still set up floor by floor afterwards, so nothing implicit is built for it -
     *  existing properties must behave exactly as they did. */
    @Test
    fun `creating a hostel builds no implicit floor, room or bed`() {
        val warden = Warden(id = "w1", pinHash = "x", name = "Warden", createdAt = 0L, updatedAt = 0L)
        coEvery { wardenRepository.getWarden() } returns warden
        coEvery { hostelRepository.create("w1", "Sunrise", "", "", PropertyType.HOSTEL) } returns mockk()

        val viewModel = viewModel()
        viewModel.onNameChange("Sunrise")
        viewModel.save()

        coVerify(exactly = 0) { floorRepository.create(any(), any()) }
        coVerify(exactly = 0) { roomRepository.create(any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { bedRepository.generateForRoom(any(), any()) }
    }

    /** The rent is the whole point of a single-unit property, and there is no room form to put it
     *  on, so saving without it would create something unlettable. */
    @Test
    fun `a single-unit property cannot be saved without a rent, a hostel can`() {
        val viewModel = viewModel()
        viewModel.onNameChange("Corner Shop")
        assertTrue(viewModel.uiState.value.canSave)

        viewModel.onPropertyTypeChange(PropertyType.SHOP)
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.onMonthlyRentChange("18000")
        assertTrue(viewModel.uiState.value.canSave)
    }

    /** The warden edits a single-unit property here and can never reach the room form, so the
     *  implicit room has to follow - otherwise the all-rooms tiles and exports keep the old name
     *  and the old rent. */
    @Test
    fun `editing a single-unit property carries the new name and rent onto its implicit room`() {
        val hostel = Hostel(id = "h9", wardenId = "w1", name = "Corner Shop", address = "", contactPhone = "", propertyType = PropertyType.SHOP, createdAt = 0L, updatedAt = 0L)
        val room = Room(id = "r9", floorId = "f9", roomNumber = "Corner Shop", type = RoomType.SINGLE, pricePerBed = 18000.0, amenities = "", createdAt = 0L, updatedAt = 0L)
        coEvery { hostelRepository.getById("h9") } returns hostel
        coEvery { roomRepository.getFirstRoomIdByHostel("h9") } returns "r9"
        coEvery { roomRepository.getById("r9") } returns room
        coEvery { hostelRepository.update(hostel, "Main Street Shop", "", "") } returns Unit
        coEvery { roomRepository.update(room, "Main Street Shop", RoomType.SINGLE, 21000.0, "") } returns Unit

        val viewModel = viewModel("h9")
        viewModel.onNameChange("Main Street Shop")
        viewModel.onMonthlyRentChange("21000")
        viewModel.save()

        coVerify { roomRepository.update(room, "Main Street Shop", RoomType.SINGLE, 21000.0, "") }
    }
}
