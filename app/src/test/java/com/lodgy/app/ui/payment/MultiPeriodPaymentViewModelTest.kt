package com.lodgy.app.ui.payment

import androidx.lifecycle.SavedStateHandle
import com.lodgy.app.data.entity.AgreementStatus
import com.lodgy.app.data.entity.Invoice
import com.lodgy.app.data.entity.InvoiceStatus
import com.lodgy.app.data.entity.PaymentMode
import com.lodgy.app.data.entity.TenancyAgreement
import com.lodgy.app.data.entity.Tenant
import com.lodgy.app.data.entity.TenantStatus
import com.lodgy.app.data.repository.CreditRepository
import com.lodgy.app.data.repository.InvoiceRepository
import com.lodgy.app.data.repository.PaymentRepository
import com.lodgy.app.data.repository.TenancyAgreementRepository
import com.lodgy.app.data.repository.TenantRepository
import com.lodgy.app.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MultiPeriodPaymentViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val invoiceRepository: InvoiceRepository = mockk()
    private val paymentRepository: PaymentRepository = mockk()
    private val creditRepository: CreditRepository = mockk()
    private val agreementRepository: TenancyAgreementRepository = mockk()
    private val tenantRepository: TenantRepository = mockk()

    private val agreement = TenancyAgreement(id = "a1", tenantId = "t1", bedId = "b1", agreedRent = 5000.0, advanceDeposit = 0.0, billingCycleDay = 5, moveInDate = 0L, moveOutDate = null, depositRefundAmount = null, status = AgreementStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)

    private fun invoice(id: String, month: Int, status: InvoiceStatus = InvoiceStatus.UNPAID, amountDue: Double = 5000.0) =
        Invoice(id = id, tenancyAgreementId = "a1", periodMonth = month, periodYear = 2026, amountDue = amountDue, dueDate = 0L, status = status, createdAt = 0L, updatedAt = 0L)

    @Before
    fun setUp() {
        coEvery { agreementRepository.getLatestByTenantId("t1") } returns agreement
        coEvery { tenantRepository.getById("t1") } returns Tenant(id = "t1", name = "Ravi", phone = "1", photoPath = null, idProofPhotoPath = null, emergencyContactName = "", emergencyContactPhone = "", status = TenantStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)
        coEvery { creditRepository.getByInvoiceId(any()) } returns emptyList()
        coEvery { paymentRepository.getTotalPaid(any()) } returns 0.0
        coEvery { paymentRepository.create(any(), any(), any(), any(), any(), any()) } returns mockk()
        coEvery { invoiceRepository.updateStatus(any(), any()) } returns Unit
    }

    private fun viewModel(invoices: List<Invoice>): MultiPeriodPaymentViewModel {
        every { invoiceRepository.getByTenancyAgreementId("a1") } returns flowOf(invoices)
        return MultiPeriodPaymentViewModel(
            invoiceRepository, paymentRepository, creditRepository, agreementRepository, tenantRepository,
            SavedStateHandle(mapOf("tenantId" to "t1")),
        )
    }

    @Test
    fun `open months are listed oldest first with the lump sum prefilled to clear them all`() {
        val state = viewModel(listOf(invoice("sep", 9), invoice("aug", 8))).uiState.value

        assertEquals(listOf("aug", "sep"), state.openInvoices.map { it.invoice.id })
        assertEquals(10000.0, state.totalOutstanding, 0.0001)
        assertEquals("10000.0", state.amount)
        assertTrue(state.canSave)
    }

    @Test
    fun `settled months are left out entirely`() {
        val state = viewModel(
            listOf(invoice("aug", 8), invoice("jul", 7, status = InvoiceStatus.PAID)),
        ).uiState.value

        assertEquals(listOf("aug"), state.openInvoices.map { it.invoice.id })
    }

    @Test
    fun `a single open month is refused - that is an ordinary payment`() {
        assertFalse(viewModel(listOf(invoice("aug", 8))).uiState.value.canSave)
    }

    @Test
    fun `each invoice is written its own share under one shared group id`() {
        val viewModel = viewModel(listOf(invoice("aug", 8), invoice("sep", 9)))
        viewModel.onAmountChange("7000")
        val groups = mutableListOf<String?>()
        coEvery { paymentRepository.create(any(), any(), any(), any(), any(), captureNullable(groups)) } returns mockk()

        viewModel.save()

        coVerify { paymentRepository.create("aug", 5000.0, PaymentMode.CASH, any(), null, any()) }
        coVerify { paymentRepository.create("sep", 2000.0, PaymentMode.CASH, any(), null, any()) }
        assertEquals(1, groups.filterNotNull().distinct().size)
    }

    @Test
    fun `statuses follow each invoice's own share, not the lump sum`() {
        val viewModel = viewModel(listOf(invoice("aug", 8), invoice("sep", 9)))
        viewModel.onAmountChange("7000")
        coEvery { paymentRepository.getTotalPaid("aug") } returns 5000.0
        coEvery { paymentRepository.getTotalPaid("sep") } returns 2000.0

        viewModel.save()

        coVerify { invoiceRepository.updateStatus(match { it.id == "aug" }, InvoiceStatus.PAID) }
        coVerify { invoiceRepository.updateStatus(match { it.id == "sep" }, InvoiceStatus.PARTIAL) }
        assertTrue(viewModel.uiState.value.saved)
    }

    @Test
    fun `a credit on one month reduces what that month needs from the lump sum`() {
        coEvery { creditRepository.getByInvoiceId("aug") } returns listOf(
            com.lodgy.app.data.entity.Credit(id = "c1", tenantId = "t1", invoiceId = "aug", amount = 2000.0, reason = "Plumbing", createdAt = 0L, updatedAt = 0L),
        )

        val state = viewModel(listOf(invoice("aug", 8), invoice("sep", 9))).uiState.value

        assertEquals(3000.0, state.openInvoices.first().outstanding, 0.0001)
        assertEquals(8000.0, state.totalOutstanding, 0.0001)
    }

    @Test
    fun `zero cannot be recorded`() {
        val viewModel = viewModel(listOf(invoice("aug", 8), invoice("sep", 9)))
        viewModel.onAmountChange("0")

        assertFalse(viewModel.uiState.value.canSave)

        viewModel.save()
        coVerify(exactly = 0) { paymentRepository.create(any(), any(), any(), any(), any(), any()) }
    }
}
