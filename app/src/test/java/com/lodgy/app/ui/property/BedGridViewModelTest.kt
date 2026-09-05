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
import com.lodgy.app.data.entity.AgreementStatus
import com.lodgy.app.data.entity.TenancyAgreement
import com.lodgy.app.data.entity.Tenant
import com.lodgy.app.data.entity.TenantStatus
import com.lodgy.app.data.repository.TenancyAgreementRepository
import com.lodgy.app.data.repository.TenantRepository
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
        coEvery { roomRepository.getById("r1") } returns Room(id = "r1", floorId = "f1", roomNumber = "101", type = RoomType.DOUBLE, pricePerBed = 3000.0, amenities = "", createdAt = 0L, updatedAt = 0L)
        val bedB = Bed(id = "b2", roomId = "r1", label = "B", status = BedStatus.VACANT, createdAt = 0L, updatedAt = 0L)
        val bedA = Bed(id = "b1", roomId = "r1", label = "A", status = BedStatus.OCCUPIED, createdAt = 0L, updatedAt = 0L)
        every { bedRepository.getByRoomId("r1") } returns flowOf(listOf(bedB, bedA))

        val viewModel = BedGridViewModel(bedRepository, roomRepository, agreementRepository, tenantRepository, SavedStateHandle(mapOf("roomId" to "r1")))
        val state = viewModel.uiState.value

        assertEquals("101", state.roomNumber)
        assertEquals("DOUBLE", state.roomType)
        assertEquals(listOf("A", "B"), state.beds.map { it.label })
    }

    /** LODGY-69: beds were not clickable at all, so onboarding could only start from the Tenants
     *  tab even when the warden was looking straight at the bed. */
    @Test
    fun `tapping an occupied bed resolves the tenant on it`() = runTest {
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
        coEvery { roomRepository.getById("r1") } returns Room(id = "r1", floorId = "f1", roomNumber = "101", type = RoomType.DOUBLE, pricePerBed = 3000.0, amenities = "", createdAt = 0L, updatedAt = 0L)
        val vacant = Bed(id = "b2", roomId = "r1", label = "B", status = BedStatus.VACANT, createdAt = 0L, updatedAt = 0L)
        every { bedRepository.getByRoomId("r1") } returns flowOf(listOf(vacant))
        coEvery { agreementRepository.getActiveByBedId("b2") } returns null

        val viewModel = viewModel()
        viewModel.onBedSelected(vacant)
        viewModel.onBedSheetDismissed()

        assertNull(viewModel.uiState.value.selectedBed)
    }

    private fun viewModel() = BedGridViewModel(
        bedRepository, roomRepository, agreementRepository, tenantRepository,
        SavedStateHandle(mapOf("roomId" to "r1")),
    )
}
