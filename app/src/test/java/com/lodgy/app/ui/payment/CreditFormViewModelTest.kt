package com.lodgy.app.ui.payment

import androidx.lifecycle.SavedStateHandle
import com.lodgy.app.data.entity.AgreementStatus
import com.lodgy.app.data.entity.Invoice
import com.lodgy.app.data.entity.InvoiceStatus
import com.lodgy.app.data.entity.TenancyAgreement
import com.lodgy.app.data.entity.Tenant
import com.lodgy.app.data.entity.TenantStatus
import com.lodgy.app.data.repository.CreditRepository
import com.lodgy.app.data.repository.InvoiceRepository
import com.lodgy.app.data.repository.TenancyAgreementRepository
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

class CreditFormViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val creditRepository: CreditRepository = mockk()
    private val tenantRepository: TenantRepository = mockk()
    private val agreementRepository: TenancyAgreementRepository = mockk()
    private val invoiceRepository: InvoiceRepository = mockk()

    private val agreement = TenancyAgreement(id = "a1", tenantId = "t1", bedId = "b1", agreedRent = 5000.0, advanceDeposit = 0.0, billingCycleDay = 1, moveInDate = 0L, moveOutDate = null, depositRefundAmount = null, status = AgreementStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)

    private fun invoice(id: String, status: InvoiceStatus, dueDate: Long) =
        Invoice(id = id, tenancyAgreementId = "a1", periodMonth = 9, periodYear = 2026, amountDue = 5000.0, dueDate = dueDate, status = status, createdAt = 0L, updatedAt = 0L)

    private fun viewModel(invoices: List<Invoice> = emptyList()): CreditFormViewModel {
        coEvery { tenantRepository.getById("t1") } returns Tenant(id = "t1", name = "Ravi", phone = "1", photoPath = null, idProofPhotoPath = null, emergencyContactName = "", emergencyContactPhone = "", status = TenantStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)
        coEvery { agreementRepository.getLatestByTenantId("t1") } returns agreement
        every { invoiceRepository.getByTenancyAgreementId("a1") } returns flowOf(invoices)
        return CreditFormViewModel(creditRepository, tenantRepository, agreementRepository, invoiceRepository, SavedStateHandle(mapOf("tenantId" to "t1")))
    }

    @Test
    fun `only unsettled invoices are offered as targets, newest due first`() {
        val state = viewModel(
            listOf(
                invoice("paid", InvoiceStatus.PAID, dueDate = 300L),
                invoice("older", InvoiceStatus.UNPAID, dueDate = 100L),
                invoice("newer", InvoiceStatus.PARTIAL, dueDate = 200L),
            ),
        ).uiState.value

        assertEquals(listOf("newer", "older"), state.openInvoices.map { it.id })
    }

    @Test
    fun `a credit needs both an amount and a reason before it can be saved`() {
        val viewModel = viewModel()

        assertFalse(viewModel.uiState.value.canSave)

        viewModel.onAmountChange("500")
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.onReasonChange("Plumbing")
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun `zero and negative amounts are refused`() {
        val viewModel = viewModel()
        viewModel.onReasonChange("Plumbing")

        viewModel.onAmountChange("0")
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.onAmountChange("-100")
        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `saving with no invoice chosen leaves the credit pending for the next one`() {
        coEvery { creditRepository.create(any(), any(), any(), any()) } returns mockk()
        val viewModel = viewModel()

        viewModel.onAmountChange("500")
        viewModel.onReasonChange("  Plumbing  ")
        viewModel.save()

        coVerify { creditRepository.create("t1", null, 500.0, "Plumbing") }
        assertTrue(viewModel.uiState.value.saved)
    }

    @Test
    fun `saving against a chosen invoice attaches it there`() {
        coEvery { creditRepository.create(any(), any(), any(), any()) } returns mockk()
        val viewModel = viewModel(listOf(invoice("inv-1", InvoiceStatus.UNPAID, dueDate = 100L)))

        viewModel.onAmountChange("750")
        viewModel.onReasonChange("Geyser repair")
        viewModel.onInvoiceSelected("inv-1")
        viewModel.save()

        coVerify { creditRepository.create("t1", "inv-1", 750.0, "Geyser repair") }
    }
}
