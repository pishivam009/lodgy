package com.lodgy.app.ui.property

import androidx.lifecycle.SavedStateHandle
import com.lodgy.app.data.entity.Floor
import com.lodgy.app.data.entity.PropertyType
import com.lodgy.app.data.entity.ReconciliationMark
import com.lodgy.app.data.entity.Room
import com.lodgy.app.data.entity.RoomType
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.ExpenseRepository
import com.lodgy.app.data.repository.FloorRepository
import com.lodgy.app.data.repository.RoomRepository
import com.lodgy.app.data.repository.TenancyAgreementRepository
import com.lodgy.app.data.entity.Hostel
import com.lodgy.app.data.entity.Warden
import com.lodgy.app.data.repository.HostelRepository
import com.lodgy.app.data.repository.ReconciliationRepository
import com.lodgy.app.data.repository.WardenRepository
import com.lodgy.app.testutil.MainDispatcherRule
import com.lodgy.app.ui.common.UpdateChange
import com.lodgy.app.data.entity.Expense
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
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
    private val expenseRepository: ExpenseRepository = mockk()
    private val tenancyAgreementRepository: TenancyAgreementRepository = mockk()
    // No reconciled month unless a test says so, so a rename is free by default (LODGY-65).
    private val reconciliationRepository: ReconciliationRepository =
        mockk { every { getByHostelId(any()) } returns flowOf(emptyList()) }

    private fun viewModel(hostelId: String? = null) = HostelFormViewModel(
        hostelRepository, wardenRepository, floorRepository, roomRepository, bedRepository,
        expenseRepository, tenancyAgreementRepository, reconciliationRepository,
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
        viewModel.confirmChanges()

        coVerify { roomRepository.update(room, "Main Street Shop", RoomType.SINGLE, 21000.0, "") }
    }

    /** LODGY-65. A rename is free and stays unconfirmed - until the warden has attested a month
     *  against the old name, which their paper register and their exported PDFs still carry. */
    @Test
    fun `renaming a property with no reconciled month saves without a prompt`() {
        val hostel = Hostel(id = "h1", wardenId = "w1", name = "Old", address = "A", contactPhone = "1", createdAt = 0L, updatedAt = 0L)
        coEvery { hostelRepository.getById("h1") } returns hostel
        coEvery { hostelRepository.update(hostel, "New", "A", "1") } returns Unit

        val viewModel = viewModel("h1")
        viewModel.onNameChange("New")
        viewModel.save()

        assertTrue(viewModel.uiState.value.pendingChanges.isEmpty())
        coVerify { hostelRepository.update(hostel, "New", "A", "1") }
    }

    @Test
    fun `renaming a property with reconciled months asks first and counts them`() {
        val hostel = Hostel(id = "h1", wardenId = "w1", name = "Old", address = "A", contactPhone = "1", createdAt = 0L, updatedAt = 0L)
        coEvery { hostelRepository.getById("h1") } returns hostel
        every { reconciliationRepository.getByHostelId("h1") } returns flowOf(
            listOf(
                ReconciliationMark(id = "m1", hostelId = "h1", periodMonth = 7, periodYear = 2026, note = null, createdAt = 0L, updatedAt = 0L),
                ReconciliationMark(id = "m2", hostelId = "h1", periodMonth = 8, periodYear = 2026, note = null, createdAt = 0L, updatedAt = 0L),
            ),
        )

        val viewModel = viewModel("h1")
        viewModel.onNameChange("New")
        viewModel.save()

        assertEquals(listOf(UpdateChange.HostelRename("New", 2)), viewModel.uiState.value.pendingChanges)
        coVerify(exactly = 0) { hostelRepository.update(any(), any(), any(), any()) }
    }

    /** Editing the address or the phone number changes nothing but itself, so it must stay
     *  unconfirmed however many months are reconciled (LODGY-57 AC5, upheld under LODGY-65). */
    @Test
    fun `editing the address of a reconciled property saves without a prompt`() {
        val hostel = Hostel(id = "h1", wardenId = "w1", name = "Old", address = "A", contactPhone = "1", createdAt = 0L, updatedAt = 0L)
        coEvery { hostelRepository.getById("h1") } returns hostel
        coEvery { hostelRepository.update(hostel, "Old", "B", "1") } returns Unit
        every { reconciliationRepository.getByHostelId("h1") } returns flowOf(
            listOf(ReconciliationMark(id = "m1", hostelId = "h1", periodMonth = 7, periodYear = 2026, note = null, createdAt = 0L, updatedAt = 0L)),
        )

        val viewModel = viewModel("h1")
        viewModel.onAddressChange("B")
        viewModel.save()

        assertTrue(viewModel.uiState.value.pendingChanges.isEmpty())
        coVerify { hostelRepository.update(hostel, "Old", "B", "1") }
    }

    @Test
    fun `re-renting a single-unit property asks first and states both figures`() {
        val hostel = Hostel(id = "h9", wardenId = "w1", name = "Corner Shop", address = "", contactPhone = "", propertyType = PropertyType.SHOP, createdAt = 0L, updatedAt = 0L)
        val room = Room(id = "r9", floorId = "f9", roomNumber = "Corner Shop", type = RoomType.SINGLE, pricePerBed = 18000.0, amenities = "", createdAt = 0L, updatedAt = 0L)
        coEvery { hostelRepository.getById("h9") } returns hostel
        coEvery { roomRepository.getFirstRoomIdByHostel("h9") } returns "r9"
        coEvery { roomRepository.getById("r9") } returns room

        val viewModel = viewModel("h9")
        viewModel.onMonthlyRentChange("21000")
        viewModel.save()

        assertEquals(listOf(UpdateChange.UnitRent(18000.0, 21000.0)), viewModel.uiState.value.pendingChanges)
        coVerify(exactly = 0) { roomRepository.update(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `dismissing abandons a single-unit rent change`() {
        val hostel = Hostel(id = "h9", wardenId = "w1", name = "Corner Shop", address = "", contactPhone = "", propertyType = PropertyType.SHOP, createdAt = 0L, updatedAt = 0L)
        val room = Room(id = "r9", floorId = "f9", roomNumber = "Corner Shop", type = RoomType.SINGLE, pricePerBed = 18000.0, amenities = "", createdAt = 0L, updatedAt = 0L)
        coEvery { hostelRepository.getById("h9") } returns hostel
        coEvery { roomRepository.getFirstRoomIdByHostel("h9") } returns "r9"
        coEvery { roomRepository.getById("r9") } returns room

        val viewModel = viewModel("h9")
        viewModel.onMonthlyRentChange("21000")
        viewModel.save()
        viewModel.dismissChanges()

        assertTrue(viewModel.uiState.value.pendingChanges.isEmpty())
        assertFalse(viewModel.uiState.value.saved)
        coVerify(exactly = 0) { hostelRepository.update(any(), any(), any(), any()) }
    }

    @Test
    fun `an empty property deletes, cascading its floors and beds`() {
        val hostel = Hostel(id = "h1", wardenId = "w1", name = "Sunrise", address = "", contactPhone = "", createdAt = 0L, updatedAt = 0L)
        coEvery { hostelRepository.getById("h1") } returns hostel
        every { floorRepository.getByHostelId("h1") } returns flowOf(emptyList())
        every { expenseRepository.getByHostelId("h1") } returns flowOf(emptyList())
        coEvery { tenancyAgreementRepository.getAll() } returns emptyList()
        coEvery { hostelRepository.delete(hostel) } returns Unit

        val viewModel = viewModel("h1")
        viewModel.requestDelete()
        assertTrue(viewModel.uiState.value.pendingDelete)

        viewModel.confirmDelete()
        assertTrue(viewModel.uiState.value.deleted)
        coVerify { hostelRepository.delete(hostel) }
    }

    @Test
    fun `a property with an expense is blocked from deletion`() {
        val hostel = Hostel(id = "h1", wardenId = "w1", name = "Sunrise", address = "", contactPhone = "", createdAt = 0L, updatedAt = 0L)
        coEvery { hostelRepository.getById("h1") } returns hostel
        every { floorRepository.getByHostelId("h1") } returns flowOf(emptyList())
        every { expenseRepository.getByHostelId("h1") } returns flowOf(listOf(mockk<Expense>()))
        coEvery { tenancyAgreementRepository.getAll() } returns emptyList()

        val viewModel = viewModel("h1")
        viewModel.requestDelete()

        assertTrue(viewModel.uiState.value.blockedDelete)
        assertFalse(viewModel.uiState.value.pendingDelete)
        coVerify(exactly = 0) { hostelRepository.delete(any()) }
    }
}
