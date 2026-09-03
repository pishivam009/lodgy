package com.lodgy.app.ui.payment

import androidx.lifecycle.SavedStateHandle
import com.lodgy.app.data.entity.AgreementStatus
import com.lodgy.app.data.entity.Tenant
import com.lodgy.app.data.entity.TenancyAgreement
import com.lodgy.app.data.entity.TenantStatus
import com.lodgy.app.data.repository.InvoiceRepository
import com.lodgy.app.data.repository.TenancyAgreementRepository
import com.lodgy.app.data.repository.TenantRepository
import com.lodgy.app.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ManualInvoiceFormViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val invoiceRepository: InvoiceRepository = mockk()
    private val tenancyAgreementRepository: TenancyAgreementRepository = mockk()
    private val tenantRepository: TenantRepository = mockk()

    private val tenant = Tenant(id = "t1", name = "Ravi", phone = "1", photoPath = null, idProofPhotoPath = null, emergencyContactName = "e", emergencyContactPhone = "2", status = TenantStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)
    private val agreement = TenancyAgreement(id = "a1", tenantId = "t1", bedId = "b1", agreedRent = 5000.0, advanceDeposit = 0.0, billingCycleDay = 1, moveInDate = 0L, moveOutDate = null, depositRefundAmount = null, status = AgreementStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)

    private fun viewModel() = ManualInvoiceFormViewModel(
        invoiceRepository, tenancyAgreementRepository, tenantRepository, SavedStateHandle(mapOf("tenantId" to "t1")),
    )

    @Test
    fun `no active agreement disables saving and shows an empty amount`() {
        coEvery { tenantRepository.getById("t1") } returns tenant
        coEvery { tenancyAgreementRepository.getActiveByTenantId("t1") } returns null

        val state = viewModel().uiState.value

        assertFalse(state.loading)
        assertFalse(state.hasActiveAgreement)
        assertEquals("Ravi", state.tenantName)
        assertEquals("", state.amountDue)
    }

    @Test
    fun `an active agreement prefills the agreed rent as the amount due`() {
        coEvery { tenantRepository.getById("t1") } returns tenant
        coEvery { tenancyAgreementRepository.getActiveByTenantId("t1") } returns agreement

        val state = viewModel().uiState.value

        assertTrue(state.hasActiveAgreement)
        assertEquals("5000.0", state.amountDue)
    }

    @Test
    fun `canSave requires a month between 1 and 12, a year, and a numeric amount`() {
        coEvery { tenantRepository.getById("t1") } returns tenant
        coEvery { tenancyAgreementRepository.getActiveByTenantId("t1") } returns agreement
        val viewModel = viewModel()

        viewModel.onPeriodMonthChange("13")
        viewModel.onPeriodYearChange("2026")
        viewModel.onAmountDueChange("5000")
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.onPeriodMonthChange("9")
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun `save flags a duplicate error instead of creating a second invoice for the same period`() {
        coEvery { tenantRepository.getById("t1") } returns tenant
        coEvery { tenancyAgreementRepository.getActiveByTenantId("t1") } returns agreement
        coEvery { invoiceRepository.existsForPeriod("a1", 9, 2026) } returns true

        val viewModel = viewModel()
        viewModel.onPeriodMonthChange("9")
        viewModel.onPeriodYearChange("2026")
        viewModel.onAmountDueChange("5000")
        viewModel.save()

        assertTrue(viewModel.uiState.value.duplicateError)
        coVerify(exactly = 0) { invoiceRepository.create(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `save creates the invoice when the period isn't already billed`() {
        coEvery { tenantRepository.getById("t1") } returns tenant
        coEvery { tenancyAgreementRepository.getActiveByTenantId("t1") } returns agreement
        coEvery { invoiceRepository.existsForPeriod("a1", 9, 2026) } returns false
        coEvery { invoiceRepository.create("a1", 9, 2026, 5000.0, any()) } returns mockk()

        val viewModel = viewModel()
        viewModel.onPeriodMonthChange("9")
        viewModel.onPeriodYearChange("2026")
        viewModel.onAmountDueChange("5000")
        viewModel.save()

        assertTrue(viewModel.uiState.value.saved)
        assertFalse(viewModel.uiState.value.duplicateError)
    }

    @Test
    fun `save does nothing without an active agreement`() {
        coEvery { tenantRepository.getById("t1") } returns tenant
        coEvery { tenancyAgreementRepository.getActiveByTenantId("t1") } returns null

        val viewModel = viewModel()
        viewModel.onPeriodMonthChange("9")
        viewModel.onPeriodYearChange("2026")
        viewModel.onAmountDueChange("5000")
        viewModel.save()

        coVerify(exactly = 0) { invoiceRepository.create(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `changing the period clears any prior duplicate error`() {
        coEvery { tenantRepository.getById("t1") } returns tenant
        coEvery { tenancyAgreementRepository.getActiveByTenantId("t1") } returns agreement
        coEvery { invoiceRepository.existsForPeriod("a1", 9, 2026) } returns true

        val viewModel = viewModel()
        viewModel.onPeriodMonthChange("9")
        viewModel.onPeriodYearChange("2026")
        viewModel.onAmountDueChange("5000")
        viewModel.save()
        assertTrue(viewModel.uiState.value.duplicateError)

        viewModel.onPeriodMonthChange("10")
        assertFalse(viewModel.uiState.value.duplicateError)
    }
}
