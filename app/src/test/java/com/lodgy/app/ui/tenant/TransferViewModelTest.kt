package com.lodgy.app.ui.tenant

import androidx.lifecycle.SavedStateHandle
import com.lodgy.app.data.dao.BedLocation
import com.lodgy.app.data.dao.VacantBedRow
import com.lodgy.app.data.entity.AgreementStatus
import com.lodgy.app.data.entity.NoteType
import com.lodgy.app.data.entity.TenancyAgreement
import com.lodgy.app.data.entity.Tenant
import com.lodgy.app.data.entity.TenantStatus
import com.lodgy.app.data.prefs.HostelPreferences
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.TenancyAgreementRepository
import com.lodgy.app.data.repository.TenantNoteRepository
import com.lodgy.app.data.repository.TenantRepository
import com.lodgy.app.testutil.MainDispatcherRule
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

class TransferViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val agreementRepository: TenancyAgreementRepository = mockk()
    private val tenantRepository: TenantRepository = mockk()
    private val bedRepository: BedRepository = mockk()
    private val noteRepository: TenantNoteRepository = mockk()
    private val hostelPreferences: HostelPreferences = mockk()

    private val agreement = TenancyAgreement(id = "a1", tenantId = "t1", bedId = "old-bed", agreedRent = 4000.0, advanceDeposit = 0.0, billingCycleDay = 1, moveInDate = 0L, moveOutDate = null, depositRefundAmount = null, status = AgreementStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)
    private val target = VacantBedRow(bedId = "new-bed", bedLabel = "B", roomNumber = "204", pricePerBed = 5500.0, floorLabel = "Second")

    private fun viewModel(
        active: TenancyAgreement? = agreement,
        options: List<VacantBedRow> = listOf(target),
    ): TransferViewModel {
        coEvery { agreementRepository.getActiveByTenantId("t1") } returns active
        coEvery { tenantRepository.getById("t1") } returns Tenant(id = "t1", name = "Ravi", phone = "1", photoPath = null, idProofPhotoPath = null, emergencyContactName = "", emergencyContactPhone = "", status = TenantStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)
        coEvery { bedRepository.getLocation("old-bed") } returns BedLocation("101", "A")
        coEvery { bedRepository.getVacantBedsByHostel("h1") } returns options
        every { hostelPreferences.selectedHostelId } returns flowOf("h1")
        return TransferViewModel(agreementRepository, tenantRepository, bedRepository, noteRepository, hostelPreferences, SavedStateHandle(mapOf("tenantId" to "t1")))
    }

    private fun stubWrites() {
        coEvery { agreementRepository.transferBed(any(), any(), any()) } returns Unit
        coEvery { bedRepository.setOccupied(any()) } returns Unit
        coEvery { bedRepository.setVacant(any()) } returns Unit
        coEvery { noteRepository.create(any(), any(), any(), any(), any()) } returns mockk()
    }

    @Test
    fun `loads the current bed, the vacant options and the existing rent`() {
        val state = viewModel().uiState.value

        assertEquals(BedLocation("101", "A"), state.currentLocation)
        assertEquals(listOf("new-bed"), state.options.map { it.bedId })
        assertEquals("4000.0", state.rent)
        assertFalse(state.canSave)
    }

    @Test
    fun `a tenant with no active tenancy cannot be transferred`() {
        val state = viewModel(active = null).uiState.value

        assertFalse(state.hasActiveAgreement)
        assertFalse(state.canSave)
    }

    @Test
    fun `picking a bed re-prices to that room's rate and the warden can still override it`() {
        val viewModel = viewModel()

        viewModel.onBedSelected("new-bed")
        assertEquals("5500.0", viewModel.uiState.value.rent)
        assertTrue(viewModel.uiState.value.canSave)

        viewModel.onRentChange("5000")
        assertEquals("5000", viewModel.uiState.value.rent)
    }

    @Test
    fun `transfer keeps the same agreement and swaps both bed statuses`() {
        stubWrites()
        val viewModel = viewModel()

        viewModel.onBedSelected("new-bed")
        viewModel.confirmTransfer("Moved from Room 101 · Bed A to Room 204 · Bed B.")

        coVerify { agreementRepository.transferBed(agreement, "new-bed", 5500.0) }
        coVerify { bedRepository.setOccupied("new-bed") }
        coVerify { bedRepository.setVacant("old-bed") }
        coVerify(exactly = 0) { agreementRepository.close(any(), any(), any()) }
        coVerify(exactly = 0) { agreementRepository.create(any(), any(), any(), any(), any(), any()) }
        assertTrue(viewModel.uiState.value.saved)
    }

    @Test
    fun `the move is written to the tenant's timeline naming both beds`() {
        stubWrites()
        val viewModel = viewModel()

        viewModel.onBedSelected("new-bed")
        viewModel.confirmTransfer("Moved from Room 101 · Bed A to Room 204 · Bed B.")

        coVerify {
            noteRepository.create("t1", NoteType.GENERAL, "Moved from Room 101 · Bed A to Room 204 · Bed B.", null, any())
        }
    }

    @Test
    fun `transferring to the bed the tenant already occupies is refused rather than freeing it`() {
        stubWrites()
        val viewModel = viewModel(options = listOf(target.copy(bedId = "old-bed")))

        viewModel.onBedSelected("old-bed")
        viewModel.confirmTransfer("note")

        coVerify(exactly = 0) { bedRepository.setVacant(any()) }
        coVerify(exactly = 0) { agreementRepository.transferBed(any(), any(), any()) }
        assertFalse(viewModel.uiState.value.saved)
    }
}
