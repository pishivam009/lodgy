package com.lodgy.app.ui.payment

import androidx.lifecycle.SavedStateHandle
import com.lodgy.app.data.dao.BedLocation
import com.lodgy.app.data.entity.AgreementStatus
import com.lodgy.app.data.entity.Credit
import com.lodgy.app.data.entity.Invoice
import com.lodgy.app.data.entity.InvoiceStatus
import com.lodgy.app.data.entity.Payment
import com.lodgy.app.data.entity.PaymentMode
import com.lodgy.app.data.entity.ReconciliationMark
import com.lodgy.app.data.entity.TenancyAgreement
import com.lodgy.app.data.entity.Tenant
import com.lodgy.app.data.entity.TenantStatus
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.CreditRepository
import com.lodgy.app.data.repository.InvoiceRepository
import com.lodgy.app.data.repository.PaymentRepository
import com.lodgy.app.data.repository.ReconciliationRepository
import com.lodgy.app.data.repository.TenancyAgreementRepository
import com.lodgy.app.data.repository.TenantRepository
import com.lodgy.app.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class InvoiceDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val invoiceRepository: InvoiceRepository = mockk()
    private val tenancyAgreementRepository: TenancyAgreementRepository = mockk()
    private val tenantRepository: TenantRepository = mockk()
    private val paymentRepository: PaymentRepository = mockk()
    private val bedRepository: BedRepository = mockk()
    private val creditRepository: CreditRepository = mockk()
    private val reconciliationRepository: ReconciliationRepository = mockk()

    private val agreement = TenancyAgreement(id = "a1", tenantId = "t1", bedId = "b1", agreedRent = 5000.0, advanceDeposit = 0.0, billingCycleDay = 1, moveInDate = 0L, moveOutDate = null, depositRefundAmount = null, status = AgreementStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)
    private val tenant = Tenant(id = "t1", name = "Ravi", phone = "1", photoPath = null, idProofPhotoPath = null, emergencyContactName = "e", emergencyContactPhone = "2", status = TenantStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)

    private fun viewModel(
        invoice: Invoice?,
        payments: List<Payment> = emptyList(),
        credits: List<Credit> = emptyList(),
        reconciled: Boolean = false,
    ): InvoiceDetailViewModel {
        coEvery { invoiceRepository.getById("inv-1") } returns invoice
        coEvery { tenancyAgreementRepository.getById("a1") } returns agreement
        coEvery { tenantRepository.getById("t1") } returns tenant
        coEvery { bedRepository.getHostelId("b1") } returns "h1"
        coEvery { bedRepository.getLocation("b1") } returns BedLocation("204", "B")
        every { paymentRepository.getByInvoiceId("inv-1") } returns flowOf(payments)
        coEvery { creditRepository.getByInvoiceId("inv-1") } returns credits
        coEvery { reconciliationRepository.getForPeriod("h1", any(), any()) } returns
            if (reconciled) ReconciliationMark(id = "m1", hostelId = "h1", periodMonth = 9, periodYear = 2026, note = null, createdAt = 0L, updatedAt = 0L) else null
        return InvoiceDetailViewModel(
            invoiceRepository, tenancyAgreementRepository, tenantRepository, paymentRepository,
            bedRepository, creditRepository, reconciliationRepository,
            SavedStateHandle(mapOf("invoiceId" to "inv-1")),
        )
    }

    @Test
    fun `loads the invoice's tenant, location, due date and figures`() {
        val invoice = Invoice(id = "inv-1", tenancyAgreementId = "a1", periodMonth = 9, periodYear = 2026, amountDue = 5000.0, dueDate = 123L, status = InvoiceStatus.PARTIAL, createdAt = 0L, updatedAt = 0L)
        val payment = Payment(id = "p1", invoiceId = "inv-1", amount = 2000.0, paymentMode = PaymentMode.CASH, paidOn = 0L, note = null, createdAt = 0L, updatedAt = 0L)

        val state = viewModel(invoice, payments = listOf(payment)).uiState.value

        assertFalse(state.loading)
        assertTrue(state.found)
        assertEquals("Ravi", state.tenantName)
        assertEquals(BedLocation("204", "B"), state.location)
        assertEquals(9, state.periodMonth)
        assertEquals(2026, state.periodYear)
        assertEquals(123L, state.dueDateMillis)
        assertEquals(InvoiceStatus.PARTIAL, state.status)
        assertEquals(2000.0, state.totalPaid, 0.0001)
        assertEquals(5000.0, state.effectiveDue, 0.0001)
    }

    @Test
    fun `an invoice past its due date and not fully paid reads as overdue`() {
        val invoice = Invoice(id = "inv-1", tenancyAgreementId = "a1", periodMonth = 1, periodYear = 2000, amountDue = 5000.0, dueDate = 0L, status = InvoiceStatus.UNPAID, createdAt = 0L, updatedAt = 0L)

        assertTrue(viewModel(invoice).uiState.value.isOverdue)
    }

    @Test
    fun `a paid invoice never reads as overdue even past its due date`() {
        val invoice = Invoice(id = "inv-1", tenancyAgreementId = "a1", periodMonth = 1, periodYear = 2000, amountDue = 5000.0, dueDate = 0L, status = InvoiceStatus.PAID, createdAt = 0L, updatedAt = 0L)

        assertFalse(viewModel(invoice).uiState.value.isOverdue)
    }

    @Test
    fun `credits reduce the effective due but not the raw invoice amount`() {
        val invoice = Invoice(id = "inv-1", tenancyAgreementId = "a1", periodMonth = 9, periodYear = 2026, amountDue = 5000.0, dueDate = 0L, status = InvoiceStatus.UNPAID, createdAt = 0L, updatedAt = 0L)
        val credit = Credit(id = "c1", tenantId = "t1", invoiceId = "inv-1", amount = 1200.0, reason = "Plumbing", createdAt = 0L, updatedAt = 0L)

        val state = viewModel(invoice, credits = listOf(credit)).uiState.value

        assertEquals(5000.0, state.amountDue, 0.0001)
        assertEquals(1200.0, state.creditTotal, 0.0001)
        assertEquals(3800.0, state.effectiveDue, 0.0001)
    }

    @Test
    fun `a period the warden already reconciled is flagged`() {
        val invoice = Invoice(id = "inv-1", tenancyAgreementId = "a1", periodMonth = 9, periodYear = 2026, amountDue = 5000.0, dueDate = 0L, status = InvoiceStatus.UNPAID, createdAt = 0L, updatedAt = 0L)

        assertTrue(viewModel(invoice, reconciled = true).uiState.value.periodReconciled)
        assertFalse(viewModel(invoice, reconciled = false).uiState.value.periodReconciled)
    }

    @Test
    fun `a deleted invoice is reported as not found rather than crashing`() {
        val state = viewModel(invoice = null).uiState.value

        assertFalse(state.loading)
        assertFalse(state.found)
    }
}
