package com.lodgy.app.ui.dashboard

import com.lodgy.app.data.entity.AgreementStatus
import com.lodgy.app.data.entity.Bed
import com.lodgy.app.data.entity.BedStatus
import com.lodgy.app.data.entity.Expense
import com.lodgy.app.data.entity.ExpenseCategory
import com.lodgy.app.data.entity.Floor
import com.lodgy.app.data.entity.Hostel
import com.lodgy.app.data.entity.Invoice
import com.lodgy.app.data.entity.InvoiceStatus
import com.lodgy.app.data.entity.Payment
import com.lodgy.app.data.entity.PaymentMode
import com.lodgy.app.data.entity.Room
import com.lodgy.app.data.entity.RoomType
import com.lodgy.app.data.entity.TenancyAgreement
import com.lodgy.app.data.prefs.HostelPreferences
import com.lodgy.app.data.entity.Credit
import com.lodgy.app.data.entity.ReconciliationMark
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.CreditRepository
import com.lodgy.app.data.repository.ReconciliationRepository
import com.lodgy.app.data.repository.ExpenseRepository
import com.lodgy.app.data.repository.FloorRepository
import com.lodgy.app.data.repository.HostelRepository
import com.lodgy.app.data.repository.InvoiceRepository
import com.lodgy.app.data.repository.PaymentRepository
import com.lodgy.app.data.repository.RoomRepository
import com.lodgy.app.data.repository.TenancyAgreementRepository
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
import java.util.Calendar

class MonthlyReportViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val hostelPreferences: HostelPreferences = mockk()
    private val hostelRepository: HostelRepository = mockk()
    private val floorRepository: FloorRepository = mockk()
    private val roomRepository: RoomRepository = mockk()
    private val bedRepository: BedRepository = mockk()
    private val tenancyAgreementRepository: TenancyAgreementRepository = mockk()
    private val invoiceRepository: InvoiceRepository = mockk()
    private val paymentRepository: PaymentRepository = mockk()
    private val expenseRepository: ExpenseRepository = mockk()
    private val creditRepository: CreditRepository = mockk()
    private val reconciliationRepository: ReconciliationRepository = mockk()

    private fun periodMillis(year: Int, month: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 15, 0, 0, 0)
        return cal.timeInMillis
    }

    private fun viewModel(
        credits: List<Credit> = emptyList(),
        reconciled: Boolean = false,
    ): MonthlyReportViewModel {
        coEvery { creditRepository.getAllOnce() } returns credits
        coEvery { reconciliationRepository.getForPeriod(any(), any(), any()) } returns
            if (reconciled) {
                ReconciliationMark(id = "m1", hostelId = "h1", periodMonth = 1, periodYear = 2026, note = null, createdAt = 0L, updatedAt = 0L)
            } else {
                null
            }
        coEvery { reconciliationRepository.mark(any(), any(), any(), any()) } returns
            ReconciliationMark(id = "m1", hostelId = "h1", periodMonth = 1, periodYear = 2026, note = null, createdAt = 0L, updatedAt = 0L)
        coEvery { reconciliationRepository.unmark(any(), any(), any()) } returns Unit
        return MonthlyReportViewModel(
            hostelPreferences, hostelRepository, floorRepository, roomRepository, bedRepository,
            tenancyAgreementRepository, invoiceRepository, paymentRepository, expenseRepository,
            creditRepository, reconciliationRepository,
        )
    }

    @Test
    fun `no active hostel stops loading with defaults`() {
        every { hostelPreferences.selectedHostelId } returns flowOf(null)

        val state = viewModel().uiState.value

        assertFalse(state.loading)
        assertFalse(state.hasActiveHostel)
    }

    @Test
    fun `computes occupancy, collections, dues and expense for the current month`() {
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        every { hostelPreferences.selectedHostelId } returns flowOf("h1")
        coEvery { hostelRepository.getById("h1") } returns Hostel(id = "h1", wardenId = "w1", name = "Sunrise", address = "", contactPhone = "", createdAt = 0L, updatedAt = 0L)

        val floor = Floor(id = "f1", hostelId = "h1", label = "G", sortOrder = 0, createdAt = 0L, updatedAt = 0L)
        every { floorRepository.getByHostelId("h1") } returns flowOf(listOf(floor))
        val room = Room(id = "r1", floorId = "f1", roomNumber = "101", type = RoomType.DOUBLE, pricePerBed = 3000.0, amenities = "", createdAt = 0L, updatedAt = 0L)
        every { roomRepository.getByFloorId("f1") } returns flowOf(listOf(room))
        val occupiedBed = Bed(id = "b1", roomId = "r1", label = "A", status = BedStatus.OCCUPIED, createdAt = 0L, updatedAt = 0L)
        val vacantBed = Bed(id = "b2", roomId = "r1", label = "B", status = BedStatus.VACANT, createdAt = 0L, updatedAt = 0L)
        every { bedRepository.getByRoomId("r1") } returns flowOf(listOf(occupiedBed, vacantBed))

        val agreement = TenancyAgreement(id = "a1", tenantId = "t1", bedId = "b1", agreedRent = 5000.0, advanceDeposit = 0.0, billingCycleDay = 1, moveInDate = 0L, moveOutDate = null, depositRefundAmount = null, status = AgreementStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)
        val unrelatedAgreement = TenancyAgreement(id = "a2", tenantId = "t2", bedId = "other-bed", agreedRent = 1000.0, advanceDeposit = 0.0, billingCycleDay = 1, moveInDate = 0L, moveOutDate = null, depositRefundAmount = null, status = AgreementStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)
        coEvery { tenancyAgreementRepository.getAll() } returns listOf(agreement, unrelatedAgreement)

        val invoiceThisPeriod = Invoice(id = "i1", tenancyAgreementId = "a1", periodMonth = currentMonth, periodYear = currentYear, amountDue = 5000.0, dueDate = 0L, status = InvoiceStatus.PARTIAL, createdAt = 0L, updatedAt = 0L)
        val invoiceOtherPeriod = Invoice(id = "i2", tenancyAgreementId = "a1", periodMonth = currentMonth - 1, periodYear = currentYear, amountDue = 5000.0, dueDate = 0L, status = InvoiceStatus.UNPAID, createdAt = 0L, updatedAt = 0L)
        every { invoiceRepository.getAll() } returns flowOf(listOf(invoiceThisPeriod, invoiceOtherPeriod))

        val payment = Payment(id = "p1", invoiceId = "i1", amount = 2000.0, paymentMode = PaymentMode.CASH, paidOn = 0L, note = null, createdAt = 0L, updatedAt = 0L)
        val unrelatedPayment = Payment(id = "p2", invoiceId = "i2", amount = 500.0, paymentMode = PaymentMode.CASH, paidOn = 0L, note = null, createdAt = 0L, updatedAt = 0L)
        coEvery { paymentRepository.getAll() } returns listOf(payment, unrelatedPayment)

        val expenseThisMonth = Expense(id = "e1", hostelId = "h1", category = ExpenseCategory.WIFI, amount = 800.0, isRecurring = true, incurredOn = periodMillis(currentYear, currentMonth), note = null, createdAt = 0L, updatedAt = 0L)
        val expenseOtherMonth = Expense(id = "e2", hostelId = "h1", category = ExpenseCategory.WATER, amount = 300.0, isRecurring = false, incurredOn = periodMillis(currentYear, currentMonth - 1), note = null, createdAt = 0L, updatedAt = 0L)
        every { expenseRepository.getByHostelId("h1") } returns flowOf(listOf(expenseThisMonth, expenseOtherMonth))

        val state = viewModel().uiState.value

        assertFalse(state.loading)
        assertEquals(50, state.occupancyPercent)
        assertEquals(2000.0, state.totalCollected, 0.0001)
        assertEquals(3000.0, state.totalDues, 0.0001)
        assertEquals(800.0, state.totalExpense, 0.0001)
        assertEquals(1200.0, state.netIncome, 0.0001)
    }

    @Test
    fun `onMonthChange and onYearChange update the period and re-run the refresh`() {
        every { hostelPreferences.selectedHostelId } returns flowOf("h1")
        coEvery { hostelRepository.getById("h1") } returns null
        every { floorRepository.getByHostelId("h1") } returns flowOf(emptyList())
        coEvery { tenancyAgreementRepository.getAll() } returns emptyList()
        every { invoiceRepository.getAll() } returns flowOf(emptyList())
        coEvery { paymentRepository.getAll() } returns emptyList()
        every { expenseRepository.getByHostelId("h1") } returns flowOf(emptyList())

        val viewModel = viewModel()
        viewModel.onMonthChange(3)
        assertEquals(3, viewModel.uiState.value.month)

        viewModel.onYearChange(2027)
        assertEquals(2027, viewModel.uiState.value.year)
    }

    @Test
    fun `an empty hostel reports zero occupancy instead of dividing by zero`() {
        every { hostelPreferences.selectedHostelId } returns flowOf("h1")
        coEvery { hostelRepository.getById("h1") } returns null
        every { floorRepository.getByHostelId("h1") } returns flowOf(emptyList())
        coEvery { tenancyAgreementRepository.getAll() } returns emptyList()
        every { invoiceRepository.getAll() } returns flowOf(emptyList())
        coEvery { paymentRepository.getAll() } returns emptyList()
        every { expenseRepository.getByHostelId("h1") } returns flowOf(emptyList())

        val state = viewModel().uiState.value

        assertEquals(0, state.occupancyPercent)
        assertFalse(state.loading)
    }

    @Test
    fun `a checked-out tenant's invoice and payment still count for the period they occurred in`() {
        every { hostelPreferences.selectedHostelId } returns flowOf("h1")
        coEvery { hostelRepository.getById("h1") } returns Hostel(id = "h1", wardenId = "w1", name = "Sunrise", address = "", contactPhone = "", createdAt = 0L, updatedAt = 0L)
        val floor = Floor(id = "f1", hostelId = "h1", label = "G", sortOrder = 0, createdAt = 0L, updatedAt = 0L)
        every { floorRepository.getByHostelId("h1") } returns flowOf(listOf(floor))
        val room = Room(id = "r1", floorId = "f1", roomNumber = "101", type = RoomType.SINGLE, pricePerBed = 3000.0, amenities = "", createdAt = 0L, updatedAt = 0L)
        every { roomRepository.getByFloorId("f1") } returns flowOf(listOf(room))
        val bed = Bed(id = "b1", roomId = "r1", label = "A", status = BedStatus.VACANT, createdAt = 0L, updatedAt = 0L)
        every { bedRepository.getByRoomId("r1") } returns flowOf(listOf(bed))

        val closedAgreement = TenancyAgreement(id = "a1", tenantId = "t1", bedId = "b1", agreedRent = 5000.0, advanceDeposit = 0.0, billingCycleDay = 1, moveInDate = 0L, moveOutDate = 0L, depositRefundAmount = 0.0, status = AgreementStatus.CLOSED, createdAt = 0L, updatedAt = 0L)
        coEvery { tenancyAgreementRepository.getAll() } returns listOf(closedAgreement)

        val invoice = Invoice(id = "i1", tenancyAgreementId = "a1", periodMonth = 7, periodYear = 2026, amountDue = 5000.0, dueDate = 0L, status = InvoiceStatus.PAID, createdAt = 0L, updatedAt = 0L)
        every { invoiceRepository.getAll() } returns flowOf(listOf(invoice))

        val payment = Payment(id = "p1", invoiceId = "i1", amount = 5000.0, paymentMode = PaymentMode.CASH, paidOn = 0L, note = null, createdAt = 0L, updatedAt = 0L)
        coEvery { paymentRepository.getAll() } returns listOf(payment)
        every { expenseRepository.getByHostelId("h1") } returns flowOf(emptyList())

        val viewModel = viewModel()
        viewModel.onMonthChange(7)
        viewModel.onYearChange(2026)

        assertEquals(5000.0, viewModel.uiState.value.totalCollected, 0.0001)
        assertEquals(0.0, viewModel.uiState.value.totalDues, 0.0001)
    }

    @Test
    fun `dues and the credits total are summed from the credit rows, not a cached figure`() {
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        every { hostelPreferences.selectedHostelId } returns flowOf("h1")
        coEvery { hostelRepository.getById("h1") } returns Hostel(id = "h1", wardenId = "w1", name = "Sunrise", address = "", contactPhone = "", createdAt = 0L, updatedAt = 0L)
        val floor = Floor(id = "f1", hostelId = "h1", label = "G", sortOrder = 0, createdAt = 0L, updatedAt = 0L)
        every { floorRepository.getByHostelId("h1") } returns flowOf(listOf(floor))
        val room = Room(id = "r1", floorId = "f1", roomNumber = "101", type = RoomType.SINGLE, pricePerBed = 5000.0, amenities = "", createdAt = 0L, updatedAt = 0L)
        every { roomRepository.getByFloorId("f1") } returns flowOf(listOf(room))
        every { bedRepository.getByRoomId("r1") } returns flowOf(
            listOf(Bed(id = "b1", roomId = "r1", label = "A", status = BedStatus.OCCUPIED, createdAt = 0L, updatedAt = 0L)),
        )
        coEvery { tenancyAgreementRepository.getAll() } returns listOf(
            TenancyAgreement(id = "a1", tenantId = "t1", bedId = "b1", agreedRent = 5000.0, advanceDeposit = 0.0, billingCycleDay = 1, moveInDate = 0L, moveOutDate = null, depositRefundAmount = null, status = AgreementStatus.ACTIVE, createdAt = 0L, updatedAt = 0L),
        )
        every { invoiceRepository.getAll() } returns flowOf(
            listOf(Invoice(id = "i1", tenancyAgreementId = "a1", periodMonth = currentMonth, periodYear = currentYear, amountDue = 5000.0, dueDate = 0L, status = InvoiceStatus.UNPAID, createdAt = 0L, updatedAt = 0L)),
        )
        coEvery { paymentRepository.getAll() } returns emptyList()
        every { expenseRepository.getByHostelId("h1") } returns flowOf(emptyList())

        val state = viewModel(
            credits = listOf(
                Credit(id = "c1", tenantId = "t1", invoiceId = "i1", amount = 1200.0, reason = "Plumbing", createdAt = 0L, updatedAt = 0L),
                Credit(id = "c2", tenantId = "t1", invoiceId = "not-this-period", amount = 900.0, reason = "Other", createdAt = 0L, updatedAt = 0L),
            ),
        ).uiState.value

        assertEquals(1200.0, state.totalCredits, 0.0001)
        assertEquals(3800.0, state.totalDues, 0.0001)
    }

    @Test
    fun `occupancy is flagged as a current-state figure only for a period that has closed`() {
        val now = Calendar.getInstance()
        val thisMonth = MonthlyReportUiState(month = now.get(Calendar.MONTH) + 1, year = now.get(Calendar.YEAR))

        assertFalse(thisMonth.occupancyIsCurrentStateOnly)
        assertTrue(thisMonth.copy(year = now.get(Calendar.YEAR) - 1).occupancyIsCurrentStateOnly)
        assertTrue(MonthlyReportUiState(month = 1, year = now.get(Calendar.YEAR) - 1).occupancyIsCurrentStateOnly)
        assertFalse(thisMonth.copy(year = now.get(Calendar.YEAR) + 1).occupancyIsCurrentStateOnly)
    }

    @Test
    fun `a period the warden has attested to reads as reconciled`() {
        every { hostelPreferences.selectedHostelId } returns flowOf("h1")
        coEvery { hostelRepository.getById("h1") } returns Hostel(id = "h1", wardenId = "w1", name = "Sunrise", address = "", contactPhone = "", createdAt = 0L, updatedAt = 0L)
        every { floorRepository.getByHostelId("h1") } returns flowOf(emptyList())
        coEvery { tenancyAgreementRepository.getAll() } returns emptyList()
        every { invoiceRepository.getAll() } returns flowOf(emptyList())
        coEvery { paymentRepository.getAll() } returns emptyList()
        every { expenseRepository.getByHostelId("h1") } returns flowOf(emptyList())

        assertTrue(viewModel(reconciled = true).uiState.value.reconciled)
        assertFalse(viewModel(reconciled = false).uiState.value.reconciled)
    }

    @Test
    fun `marking and unmarking a period writes and removes the attestation only`() {
        every { hostelPreferences.selectedHostelId } returns flowOf("h1")
        coEvery { hostelRepository.getById("h1") } returns Hostel(id = "h1", wardenId = "w1", name = "Sunrise", address = "", contactPhone = "", createdAt = 0L, updatedAt = 0L)
        every { floorRepository.getByHostelId("h1") } returns flowOf(emptyList())
        coEvery { tenancyAgreementRepository.getAll() } returns emptyList()
        every { invoiceRepository.getAll() } returns flowOf(emptyList())
        coEvery { paymentRepository.getAll() } returns emptyList()
        every { expenseRepository.getByHostelId("h1") } returns flowOf(emptyList())

        val viewModel = viewModel()
        val month = viewModel.uiState.value.month
        val year = viewModel.uiState.value.year

        viewModel.onReconciledChange(true)
        coVerify { reconciliationRepository.mark("h1", month, year, null) }
        assertTrue(viewModel.uiState.value.reconciled)

        viewModel.onReconciledChange(false)
        coVerify { reconciliationRepository.unmark("h1", month, year) }
        assertFalse(viewModel.uiState.value.reconciled)
    }
}
