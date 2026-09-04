package com.lodgy.app.ui.payment

import com.lodgy.app.data.dao.BedLocation
import com.lodgy.app.data.entity.AgreementStatus
import com.lodgy.app.data.entity.Invoice
import com.lodgy.app.data.entity.InvoiceStatus
import com.lodgy.app.data.entity.Payment
import com.lodgy.app.data.entity.PaymentMode
import com.lodgy.app.data.entity.Tenant
import com.lodgy.app.data.entity.TenancyAgreement
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

class InvoiceListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val invoiceRepository: InvoiceRepository = mockk()
    private val tenancyAgreementRepository: TenancyAgreementRepository = mockk()
    private val tenantRepository: TenantRepository = mockk()
    private val paymentRepository: PaymentRepository = mockk()
    private val bedRepository: BedRepository = mockk()
    private val creditRepository: CreditRepository = mockk()
    private val reconciliationRepository: ReconciliationRepository = mockk()

    private fun viewModel(): InvoiceListViewModel {
        coEvery { creditRepository.getByInvoiceId(any()) } returns emptyList()
        every { reconciliationRepository.observeAll() } returns flowOf(emptyList())
        coEvery { bedRepository.getHostelId(any()) } returns "h1"
        return InvoiceListViewModel(invoiceRepository, tenancyAgreementRepository, tenantRepository, paymentRepository, bedRepository, creditRepository, reconciliationRepository)
    }

    @Test
    fun `enriches each invoice with tenant name, room and bed, and total paid, newest first`() {
        val older = Invoice(id = "i1", tenancyAgreementId = "a1", periodMonth = 8, periodYear = 2026, amountDue = 5000.0, dueDate = 0L, status = InvoiceStatus.UNPAID, createdAt = 100L, updatedAt = 0L)
        val newer = Invoice(id = "i2", tenancyAgreementId = "a1", periodMonth = 9, periodYear = 2026, amountDue = 5000.0, dueDate = 0L, status = InvoiceStatus.PAID, createdAt = 200L, updatedAt = 0L)
        every { invoiceRepository.getAll() } returns flowOf(listOf(older, newer))

        val agreement = TenancyAgreement(id = "a1", tenantId = "t1", bedId = "b1", agreedRent = 5000.0, advanceDeposit = 0.0, billingCycleDay = 1, moveInDate = 0L, moveOutDate = null, depositRefundAmount = null, status = AgreementStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)
        coEvery { tenancyAgreementRepository.getById("a1") } returns agreement
        coEvery { tenantRepository.getById("t1") } returns Tenant(id = "t1", name = "Ravi", phone = "1", photoPath = null, idProofPhotoPath = null, emergencyContactName = "", emergencyContactPhone = "", status = TenantStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)
        every { paymentRepository.getByInvoiceId("i1") } returns flowOf(emptyList())
        every { paymentRepository.getByInvoiceId("i2") } returns flowOf(
            listOf(
                Payment(id = "p1", invoiceId = "i2", amount = 5000.0, paymentMode = PaymentMode.CASH, paidOn = 0L, note = null, multiPeriodGroupId = "group-1", createdAt = 0L, updatedAt = 0L),
            ),
        )
        coEvery { bedRepository.getLocation("b1") } returns BedLocation("204", "B")

        val state = viewModel().uiState.value

        assertEquals(listOf("i2", "i1"), state.items.map { it.invoice.id })
        assertEquals("Ravi", state.items.first().tenantName)
        assertEquals(BedLocation("204", "B"), state.items.first().location)
        assertEquals(5000.0, state.items.first().totalPaid, 0.0001)
        assertTrue(state.items.first().partOfMultiPeriodPayment)
        assertFalse(state.items.last().partOfMultiPeriodPayment)
    }

    @Test
    fun `onFilterChange updates the exposed filter`() {
        every { invoiceRepository.getAll() } returns flowOf(emptyList())

        val viewModel = viewModel()
        viewModel.onFilterChange(InvoiceFilter.PAID)

        assertEquals(InvoiceFilter.PAID, viewModel.uiState.value.filter)
    }
}
