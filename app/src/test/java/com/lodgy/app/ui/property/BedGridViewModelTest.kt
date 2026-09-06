package com.lodgy.app.ui.property

import androidx.lifecycle.SavedStateHandle
import com.lodgy.app.data.entity.Bed
import com.lodgy.app.data.entity.BedStatus
import com.lodgy.app.data.dao.RoomProperty
import com.lodgy.app.data.entity.PropertyType
import com.lodgy.app.data.entity.Room
import com.lodgy.app.data.entity.RoomType
import com.lodgy.app.data.entity.Warden
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.RoomRepository
import com.lodgy.app.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import com.lodgy.app.data.entity.AgreementStatus
import com.lodgy.app.data.entity.TenancyAgreement
import com.lodgy.app.data.entity.Tenant
import com.lodgy.app.data.entity.TenantStatus
import com.lodgy.app.data.repository.TenancyAgreementRepository
import com.lodgy.app.data.repository.TenantRepository
import com.lodgy.app.data.repository.WardenRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BedGridViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val bedRepository: BedRepository = mockk()
    private val roomRepository: RoomRepository = mockk()
    private val agreementRepository: TenancyAgreementRepository = mockk()
    private val tenantRepository: TenantRepository = mockk()
    private val wardenRepository: WardenRepository = mockk()

    private fun agreement(bedId: String, tenantId: String) = TenancyAgreement(
        id = "a-$bedId", tenantId = tenantId, bedId = bedId, agreedRent = 0.0, advanceDeposit = 0.0,
        billingCycleDay = 1, moveInDate = 0L, moveOutDate = null, depositRefundAmount = null,
        status = AgreementStatus.ACTIVE, createdAt = 0L, updatedAt = 0L,
    )

    private fun tenant(id: String, name: String) = Tenant(
        id = id, name = name, phone = "1", photoPath = null, idProofPhotoPath = null,
        emergencyContactName = "", emergencyContactPhone = "", status = TenantStatus.ACTIVE,
        createdAt = 0L, updatedAt = 0L,
    )

    @Test
    fun `loads the room number and type, and beds sorted by label`() {
        coEvery { roomRepository.getPropertyForRoom("r1") } returns RoomProperty("Sunrise PG", PropertyType.HOSTEL)
        coEvery { roomRepository.getById("r1") } returns Room(id = "r1", floorId = "f1", roomNumber = "101", type = RoomType.DOUBLE, pricePerBed = 3000.0, amenities = "", createdAt = 0L, updatedAt = 0L)
        val bedB = Bed(id = "b2", roomId = "r1", label = "B", status = BedStatus.VACANT, createdAt = 0L, updatedAt = 0L)
        val bedA = Bed(id = "b1", roomId = "r1", label = "A", status = BedStatus.OCCUPIED, createdAt = 0L, updatedAt = 0L)
        every { bedRepository.getByRoomId("r1") } returns flowOf(listOf(bedB, bedA))

        val viewModel = BedGridViewModel(bedRepository, roomRepository, agreementRepository, tenantRepository, wardenRepository, SavedStateHandle(mapOf("roomId" to "r1")))
        val state = viewModel.uiState.value

        assertEquals("101", state.roomNumber)
        assertEquals(RoomType.DOUBLE, state.roomType)
        assertEquals(listOf("A", "B"), state.beds.map { it.label })
    }

    /** LODGY-69: beds were not clickable at all, so onboarding could only start from the Tenants
     *  tab even when the warden was looking straight at the bed. */
    @Test
    fun `tapping an occupied bed resolves the tenant on it`() = runTest {
        coEvery { roomRepository.getPropertyForRoom("r1") } returns RoomProperty("Sunrise PG", PropertyType.HOSTEL)
        coEvery { roomRepository.getById("r1") } returns Room(id = "r1", floorId = "f1", roomNumber = "101", type = RoomType.DOUBLE, pricePerBed = 3000.0, amenities = "", createdAt = 0L, updatedAt = 0L)
        val occupied = Bed(id = "b1", roomId = "r1", label = "A", status = BedStatus.OCCUPIED, createdAt = 0L, updatedAt = 0L)
        every { bedRepository.getByRoomId("r1") } returns flowOf(listOf(occupied))
        coEvery { agreementRepository.getActiveByBedId("b1") } returns agreement("b1", "t1")
        coEvery { tenantRepository.getById("t1") } returns tenant("t1", "Priya")

        val viewModel = viewModel()
        viewModel.onBedSelected(occupied)

        assertEquals("t1", viewModel.uiState.value.selectedBed?.tenantId)
        assertEquals("Priya", viewModel.uiState.value.selectedBed?.tenantName)
    }

    @Test
    fun `tapping a vacant bed offers no tenant, so the sheet shows assign instead`() = runTest {
        coEvery { roomRepository.getPropertyForRoom("r1") } returns RoomProperty("Sunrise PG", PropertyType.HOSTEL)
        coEvery { roomRepository.getById("r1") } returns Room(id = "r1", floorId = "f1", roomNumber = "101", type = RoomType.DOUBLE, pricePerBed = 3000.0, amenities = "", createdAt = 0L, updatedAt = 0L)
        val vacant = Bed(id = "b2", roomId = "r1", label = "B", status = BedStatus.VACANT, createdAt = 0L, updatedAt = 0L)
        every { bedRepository.getByRoomId("r1") } returns flowOf(listOf(vacant))
        coEvery { agreementRepository.getActiveByBedId("b2") } returns null

        val viewModel = viewModel()
        viewModel.onBedSelected(vacant)

        assertNotNull(viewModel.uiState.value.selectedBed)
        assertNull(viewModel.uiState.value.selectedBed?.tenantId)
    }

    /** An OCCUPIED bed whose tenancy has gone missing must not dead-end on a profile that is not
     *  there - it falls back to the vacant behaviour. */
    @Test
    fun `an occupied bed with no agreement falls back rather than dead-ending`() = runTest {
        coEvery { roomRepository.getPropertyForRoom("r1") } returns RoomProperty("Sunrise PG", PropertyType.HOSTEL)
        coEvery { roomRepository.getById("r1") } returns Room(id = "r1", floorId = "f1", roomNumber = "101", type = RoomType.DOUBLE, pricePerBed = 3000.0, amenities = "", createdAt = 0L, updatedAt = 0L)
        val orphaned = Bed(id = "b3", roomId = "r1", label = "C", status = BedStatus.OCCUPIED, createdAt = 0L, updatedAt = 0L)
        every { bedRepository.getByRoomId("r1") } returns flowOf(listOf(orphaned))
        coEvery { agreementRepository.getActiveByBedId("b3") } returns null

        val viewModel = viewModel()
        viewModel.onBedSelected(orphaned)

        assertNull(viewModel.uiState.value.selectedBed?.tenantId)
    }

    @Test
    fun `dismissing clears the sheet`() = runTest {
        coEvery { roomRepository.getPropertyForRoom("r1") } returns RoomProperty("Sunrise PG", PropertyType.HOSTEL)
        coEvery { roomRepository.getById("r1") } returns Room(id = "r1", floorId = "f1", roomNumber = "101", type = RoomType.DOUBLE, pricePerBed = 3000.0, amenities = "", createdAt = 0L, updatedAt = 0L)
        val vacant = Bed(id = "b2", roomId = "r1", label = "B", status = BedStatus.VACANT, createdAt = 0L, updatedAt = 0L)
        every { bedRepository.getByRoomId("r1") } returns flowOf(listOf(vacant))
        coEvery { agreementRepository.getActiveByBedId("b2") } returns null

        val viewModel = viewModel()
        viewModel.onBedSelected(vacant)
        viewModel.onBedSheetDismissed()

        assertNull(viewModel.uiState.value.selectedBed)
    }

    /** What the ViewModel reads on init. Unstubbed, MockK throws inside that coroutine and the
     *  failure surfaces on whichever test happens to run next. */
    private fun stubRoom() {
        coEvery { roomRepository.getPropertyForRoom("r1") } returns RoomProperty("Sunrise PG", PropertyType.HOSTEL)
        coEvery { roomRepository.getById("r1") } returns Room(id = "r1", floorId = "f1", roomNumber = "101", type = RoomType.DOUBLE, pricePerBed = 3000.0, amenities = "", createdAt = 0L, updatedAt = 0L)
        every { bedRepository.getByRoomId("r1") } returns flowOf(emptyList())
    }

    private fun viewModel() = BedGridViewModel(
        bedRepository, roomRepository, agreementRepository, tenantRepository, wardenRepository,
        SavedStateHandle(mapOf("roomId" to "r1")),
    )

    /**
     * LODGY-87. Reaching LODGY-82's switch used to mean typing yourself in as a tenant, with a
     * phone number, because the switch lives on the last screen of the onboarding chain.
     */
    @Test
    fun `marking a room as the warden's own offers their name, ready to accept`() {
        stubRoom()
        coEvery { wardenRepository.getWarden() } returns
            Warden(id = "w1", pinHash = "x", name = "Ramesh", createdAt = 0L, updatedAt = 0L)
        val viewModel = viewModel()

        viewModel.onMarkOwnRoomRequested()

        assertEquals("Ramesh", viewModel.uiState.value.markingOwnRoom)
    }

    @Test
    fun `confirming writes a normal tenancy carrying the non-revenue flag`() {
        val bed = Bed(id = "b1", roomId = "r1", label = "A", status = BedStatus.VACANT, createdAt = 0L, updatedAt = 0L)
        val tenant = Tenant(id = "t9", name = "Ramesh", phone = "", photoPath = null, idProofPhotoPath = null, emergencyContactName = "", emergencyContactPhone = "", status = TenantStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)
        stubRoom()
        coEvery { wardenRepository.getWarden() } returns
            Warden(id = "w1", pinHash = "x", name = "Ramesh", createdAt = 0L, updatedAt = 0L)
        coEvery { agreementRepository.getActiveByBedId("b1") } returns null
        coEvery { tenantRepository.create(any(), any(), any(), any(), any(), any()) } returns tenant
        coEvery { agreementRepository.create(any(), any(), any(), any(), any(), any(), any()) } returns mockk()
        coEvery { bedRepository.setOccupied("b1") } returns Unit

        val viewModel = viewModel()
        viewModel.onBedSelected(bed)
        viewModel.onMarkOwnRoomRequested()
        viewModel.onMarkOwnRoomConfirmed()

        // Rent and deposit zero, non-revenue true: identical to doing it the long way, so nothing
        // downstream has to know this shortcut exists.
        coVerify { agreementRepository.create("t9", "b1", 0.0, 0.0, 1, any(), true) }
        coVerify { bedRepository.setOccupied("b1") }
        assertNull(viewModel.uiState.value.markingOwnRoom)
        assertNull(viewModel.uiState.value.selectedBed)
    }

    @Test
    fun `a caretaker's name replaces the warden's`() {
        val bed = Bed(id = "b1", roomId = "r1", label = "A", status = BedStatus.VACANT, createdAt = 0L, updatedAt = 0L)
        stubRoom()
        coEvery { wardenRepository.getWarden() } returns
            Warden(id = "w1", pinHash = "x", name = "Ramesh", createdAt = 0L, updatedAt = 0L)
        coEvery { agreementRepository.getActiveByBedId("b1") } returns null
        coEvery { tenantRepository.create(any(), any(), any(), any(), any(), any()) } returns mockk(relaxed = true)
        coEvery { agreementRepository.create(any(), any(), any(), any(), any(), any(), any()) } returns mockk()
        coEvery { bedRepository.setOccupied(any()) } returns Unit

        val viewModel = viewModel()
        viewModel.onBedSelected(bed)
        viewModel.onMarkOwnRoomRequested()
        viewModel.onMarkOwnRoomNameChange("Suresh")
        viewModel.onMarkOwnRoomConfirmed()

        coVerify { tenantRepository.create("Suresh", "", null, null, "", "") }
    }

    @Test
    fun `a blank name writes nothing`() {
        stubRoom()
        val bed = Bed(id = "b1", roomId = "r1", label = "A", status = BedStatus.VACANT, createdAt = 0L, updatedAt = 0L)
        coEvery { wardenRepository.getWarden() } returns
            Warden(id = "w1", pinHash = "x", name = "", createdAt = 0L, updatedAt = 0L)
        coEvery { agreementRepository.getActiveByBedId("b1") } returns null

        val viewModel = viewModel()
        viewModel.onBedSelected(bed)
        viewModel.onMarkOwnRoomRequested()
        viewModel.onMarkOwnRoomNameChange("   ")
        viewModel.onMarkOwnRoomConfirmed()

        coVerify(exactly = 0) { tenantRepository.create(any(), any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { agreementRepository.create(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `dismissing the name prompt leaves the bed alone`() {
        stubRoom()
        coEvery { wardenRepository.getWarden() } returns
            Warden(id = "w1", pinHash = "x", name = "Ramesh", createdAt = 0L, updatedAt = 0L)
        val viewModel = viewModel()

        viewModel.onMarkOwnRoomRequested()
        viewModel.onMarkOwnRoomDismissed()

        assertNull(viewModel.uiState.value.markingOwnRoom)
        coVerify(exactly = 0) { tenantRepository.create(any(), any(), any(), any(), any(), any()) }
    }
}
