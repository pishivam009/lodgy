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
import androidx.lifecycle.SavedStateHandle
import com.lodgy.app.data.repository.HostelRepository
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
import org.junit.Assert.assertNull
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
    private val hostelRepository: HostelRepository = mockk()

    private fun viewModel(args: Map<String, Any?> = emptyMap()): InvoiceListViewModel {
        coEvery { creditRepository.getByInvoiceId(any()) } returns emptyList()
        every { reconciliationRepository.observeAll() } returns flowOf(emptyList())
        coEvery { bedRepository.getHostelId(any()) } returns "h1"
        coEvery { hostelRepository.getById(any()) } returns null
        return InvoiceListViewModel(
            invoiceRepository, tenancyAgreementRepository, tenantRepository, paymentRepository,
            bedRepository, creditRepository, reconciliationRepository, hostelRepository,
            SavedStateHandle(args),
        )
    }

    /** Everything enrich() reaches, so a test can focus on which rows survive the filter. */
    private fun stubEnrichment() {
        val agreement = TenancyAgreement(id = "a1", tenantId = "t1", bedId = "b1", agreedRent = 5000.0, advanceDeposit = 0.0, billingCycleDay = 1, moveInDate = 0L, moveOutDate = null, depositRefundAmount = null, status = AgreementStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)
        coEvery { tenancyAgreementRepository.getById("a1") } returns agreement
        coEvery { tenantRepository.getById("t1") } returns Tenant(id = "t1", name = "Ravi", phone = "1", photoPath = null, idProofPhotoPath = null, emergencyContactName = "", emergencyContactPhone = "", status = TenantStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)
        every { paymentRepository.getByInvoiceId(any()) } returns flowOf(emptyList())
        coEvery { bedRepository.getLocation("b1") } returns BedLocation("204", "B")
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

    /**
     * LODGY-89. The Home tile counts "past due and not settled". Filtering to UNPAID would have
     * been the obvious shortcut and it is wrong twice over: it drops a part-paid invoice that is
     * still late, and it picks up one that is not due until next month. If the list disagrees with
     * the number the warden tapped, both stop being trusted.
     */
    @Test
    fun `overdue means past due and unsettled, not merely unpaid`() {
        val yesterday = System.currentTimeMillis() - 48 * 60 * 60 * 1000
        val nextMonth = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000
        val latePartial = Invoice(id = "i1", tenancyAgreementId = "a1", periodMonth = 8, periodYear = 2026, amountDue = 5000.0, dueDate = yesterday, status = InvoiceStatus.PARTIAL, createdAt = 300L, updatedAt = 0L)
        val lateUnpaid = Invoice(id = "i2", tenancyAgreementId = "a1", periodMonth = 8, periodYear = 2026, amountDue = 5000.0, dueDate = yesterday, status = InvoiceStatus.UNPAID, createdAt = 200L, updatedAt = 0L)
        val notYetDue = Invoice(id = "i3", tenancyAgreementId = "a1", periodMonth = 10, periodYear = 2026, amountDue = 5000.0, dueDate = nextMonth, status = InvoiceStatus.UNPAID, createdAt = 100L, updatedAt = 0L)
        val latePaid = Invoice(id = "i4", tenancyAgreementId = "a1", periodMonth = 7, periodYear = 2026, amountDue = 5000.0, dueDate = yesterday, status = InvoiceStatus.PAID, createdAt = 50L, updatedAt = 0L)
        every { invoiceRepository.getAll() } returns flowOf(listOf(latePartial, lateUnpaid, notYetDue, latePaid))
        stubEnrichment()

        val viewModel = viewModel(mapOf("filter" to "OVERDUE"))

        assertEquals(
            setOf("i1", "i2"),
            viewModel.uiState.value.filteredItems.map { it.invoice.id }.toSet(),
        )
    }

    @Test
    fun `opening from Home with a hostel filter shows only that property`() {
        val mine = Invoice(id = "i1", tenancyAgreementId = "a1", periodMonth = 8, periodYear = 2026, amountDue = 5000.0, dueDate = 0L, status = InvoiceStatus.UNPAID, createdAt = 200L, updatedAt = 0L)
        every { invoiceRepository.getAll() } returns flowOf(listOf(mine))
        stubEnrichment()

        assertEquals(1, viewModel(mapOf("hostelId" to "h1")).uiState.value.filteredItems.size)
        assertEquals(0, viewModel(mapOf("hostelId" to "other")).uiState.value.filteredItems.size)
    }

    /** The Payments tab passes nothing and must keep showing everything. */
    @Test
    fun `with no arguments the list is unfiltered and unscoped`() {
        val paid = Invoice(id = "i1", tenancyAgreementId = "a1", periodMonth = 8, periodYear = 2026, amountDue = 5000.0, dueDate = 0L, status = InvoiceStatus.PAID, createdAt = 200L, updatedAt = 0L)
        every { invoiceRepository.getAll() } returns flowOf(listOf(paid))
        stubEnrichment()

        val state = viewModel().uiState.value
        assertEquals(InvoiceFilter.ALL, state.filter)
        assertNull(state.hostelId)
        assertEquals(1, state.filteredItems.size)
    }

    /** A route carrying nonsense must not crash the Payments screen. */
    @Test
    fun `an unrecognised filter argument falls back to showing everything`() {
        val any = Invoice(id = "i1", tenancyAgreementId = "a1", periodMonth = 8, periodYear = 2026, amountDue = 5000.0, dueDate = 0L, status = InvoiceStatus.UNPAID, createdAt = 200L, updatedAt = 0L)
        every { invoiceRepository.getAll() } returns flowOf(listOf(any))
        stubEnrichment()

        assertEquals(InvoiceFilter.ALL, viewModel(mapOf("filter" to "NONSENSE")).uiState.value.filter)
    }
}
