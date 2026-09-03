package com.lodgy.app.ui.dashboard

import com.lodgy.app.data.entity.AgreementStatus
import com.lodgy.app.data.entity.Bed
import com.lodgy.app.data.entity.BedStatus
import com.lodgy.app.data.entity.Floor
import com.lodgy.app.data.entity.Hostel
import com.lodgy.app.data.entity.Invoice
import com.lodgy.app.data.entity.InvoiceStatus
import com.lodgy.app.data.entity.Payment
import com.lodgy.app.data.entity.PaymentMode
import com.lodgy.app.data.entity.Room
import com.lodgy.app.data.entity.RoomType
import com.lodgy.app.data.entity.Tenant
import com.lodgy.app.data.entity.TenancyAgreement
import com.lodgy.app.data.entity.TenantStatus
import com.lodgy.app.data.prefs.HostelPreferences
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.FloorRepository
import com.lodgy.app.data.repository.HostelRepository
import com.lodgy.app.data.repository.InvoiceRepository
import com.lodgy.app.data.repository.PaymentRepository
import com.lodgy.app.data.repository.RoomRepository
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
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

class DashboardViewModelTest {

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
    private val tenantRepository: TenantRepository = mockk()

    private fun viewModel() = DashboardViewModel(
        hostelPreferences, hostelRepository, floorRepository, roomRepository, bedRepository,
        tenancyAgreementRepository, invoiceRepository, paymentRepository, tenantRepository,
    )

    @Test
    fun `no active hostel stops loading with defaults`() {
        every { hostelPreferences.selectedHostelId } returns flowOf(null)

        val state = viewModel().uiState.value

        assertFalse(state.loading)
        assertFalse(state.hasActiveHostel)
    }

    @Test
    fun `computes vacant beds, overdue invoices, today's collections and sorted upcoming move-outs`() {
        val now = System.currentTimeMillis()
        val yesterday = now - TimeUnit.DAYS.toMillis(1)
        val tomorrow = now + TimeUnit.DAYS.toMillis(1)
        val nextWeek = now + TimeUnit.DAYS.toMillis(7)

        every { hostelPreferences.selectedHostelId } returns flowOf("h1")
        coEvery { hostelRepository.getById("h1") } returns Hostel(id = "h1", wardenId = "w1", name = "Sunrise", address = "", contactPhone = "", createdAt = 0L, updatedAt = 0L)

        val floor = Floor(id = "f1", hostelId = "h1", label = "G", sortOrder = 0, createdAt = 0L, updatedAt = 0L)
        every { floorRepository.getByHostelId("h1") } returns flowOf(listOf(floor))
        val room = Room(id = "r1", floorId = "f1", roomNumber = "101", type = RoomType.DOUBLE, pricePerBed = 3000.0, amenities = "", createdAt = 0L, updatedAt = 0L)
        every { roomRepository.getByFloorId("f1") } returns flowOf(listOf(room))
        val occupiedBed = Bed(id = "b1", roomId = "r1", label = "A", status = BedStatus.OCCUPIED, createdAt = 0L, updatedAt = 0L)
        val vacantBed = Bed(id = "b2", roomId = "r1", label = "B", status = BedStatus.VACANT, createdAt = 0L, updatedAt = 0L)
        every { bedRepository.getByRoomId("r1") } returns flowOf(listOf(occupiedBed, vacantBed))

        val movingSoon = TenancyAgreement(id = "a1", tenantId = "t1", bedId = "b1", agreedRent = 5000.0, advanceDeposit = 0.0, billingCycleDay = 1, moveInDate = 0L, moveOutDate = nextWeek, depositRefundAmount = null, status = AgreementStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)
        val movingTomorrow = TenancyAgreement(id = "a2", tenantId = "t2", bedId = "b1", agreedRent = 5000.0, advanceDeposit = 0.0, billingCycleDay = 1, moveInDate = 0L, moveOutDate = tomorrow, depositRefundAmount = null, status = AgreementStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)
        val alreadyClosed = TenancyAgreement(id = "a3", tenantId = "t3", bedId = "b1", agreedRent = 5000.0, advanceDeposit = 0.0, billingCycleDay = 1, moveInDate = 0L, moveOutDate = yesterday, depositRefundAmount = null, status = AgreementStatus.CLOSED, createdAt = 0L, updatedAt = 0L)
        coEvery { tenancyAgreementRepository.getAll() } returns listOf(movingSoon, movingTomorrow, alreadyClosed)

        coEvery { tenantRepository.getById("t1") } returns Tenant(id = "t1", name = "Later Tenant", phone = "1", photoPath = null, idProofPhotoPath = null, emergencyContactName = "", emergencyContactPhone = "", status = TenantStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)
        coEvery { tenantRepository.getById("t2") } returns Tenant(id = "t2", name = "Soon Tenant", phone = "2", photoPath = null, idProofPhotoPath = null, emergencyContactName = "", emergencyContactPhone = "", status = TenantStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)

        val overdueInvoice = Invoice(id = "i1", tenancyAgreementId = "a1", periodMonth = 1, periodYear = 2026, amountDue = 5000.0, dueDate = yesterday, status = InvoiceStatus.UNPAID, createdAt = 0L, updatedAt = 0L)
        val paidInvoice = Invoice(id = "i2", tenancyAgreementId = "a1", periodMonth = 1, periodYear = 2026, amountDue = 5000.0, dueDate = yesterday, status = InvoiceStatus.PAID, createdAt = 0L, updatedAt = 0L)
        every { invoiceRepository.getAll() } returns flowOf(listOf(overdueInvoice, paidInvoice))

        val todaysPayment = Payment(id = "p1", invoiceId = "i2", amount = 5000.0, paymentMode = PaymentMode.CASH, paidOn = now, note = null, createdAt = 0L, updatedAt = 0L)
        val oldPayment = Payment(id = "p2", invoiceId = "i1", amount = 1000.0, paymentMode = PaymentMode.CASH, paidOn = yesterday, note = null, createdAt = 0L, updatedAt = 0L)
        coEvery { paymentRepository.getAll() } returns listOf(todaysPayment, oldPayment)

        val state = viewModel().uiState.value

        assertFalse(state.loading)
        assertEquals("Sunrise", state.hostelName)
        assertEquals(1, state.vacantBedCount)
        assertEquals(1, state.overdueInvoiceCount)
        assertEquals(5000.0, state.todaysCollections, 0.0001)
        assertEquals(listOf("Soon Tenant", "Later Tenant"), state.upcomingMoveOuts.map { it.tenantName })
    }

    @Test
    fun `a checked-out tenant's invoices and payments still count toward the metrics`() {
        val now = System.currentTimeMillis()
        val yesterday = now - TimeUnit.DAYS.toMillis(1)

        every { hostelPreferences.selectedHostelId } returns flowOf("h1")
        coEvery { hostelRepository.getById("h1") } returns Hostel(id = "h1", wardenId = "w1", name = "Sunrise", address = "", contactPhone = "", createdAt = 0L, updatedAt = 0L)
        val floor = Floor(id = "f1", hostelId = "h1", label = "G", sortOrder = 0, createdAt = 0L, updatedAt = 0L)
        every { floorRepository.getByHostelId("h1") } returns flowOf(listOf(floor))
        val room = Room(id = "r1", floorId = "f1", roomNumber = "101", type = RoomType.SINGLE, pricePerBed = 3000.0, amenities = "", createdAt = 0L, updatedAt = 0L)
        every { roomRepository.getByFloorId("f1") } returns flowOf(listOf(room))
        val bed = Bed(id = "b1", roomId = "r1", label = "A", status = BedStatus.VACANT, createdAt = 0L, updatedAt = 0L)
        every { bedRepository.getByRoomId("r1") } returns flowOf(listOf(bed))

        val closedAgreement = TenancyAgreement(id = "a1", tenantId = "t1", bedId = "b1", agreedRent = 5000.0, advanceDeposit = 0.0, billingCycleDay = 1, moveInDate = 0L, moveOutDate = now, depositRefundAmount = 0.0, status = AgreementStatus.CLOSED, createdAt = 0L, updatedAt = 0L)
        coEvery { tenancyAgreementRepository.getAll() } returns listOf(closedAgreement)

        val overdueInvoice = Invoice(id = "i1", tenancyAgreementId = "a1", periodMonth = 7, periodYear = 2026, amountDue = 5000.0, dueDate = yesterday, status = InvoiceStatus.UNPAID, createdAt = 0L, updatedAt = 0L)
        every { invoiceRepository.getAll() } returns flowOf(listOf(overdueInvoice))

        val paidToday = Payment(id = "p1", invoiceId = "i1", amount = 2000.0, paymentMode = PaymentMode.CASH, paidOn = now, note = null, createdAt = 0L, updatedAt = 0L)
        coEvery { paymentRepository.getAll() } returns listOf(paidToday)

        val state = viewModel().uiState.value

        assertEquals(1, state.overdueInvoiceCount)
        assertEquals(2000.0, state.todaysCollections, 0.0001)
    }

    @Test
    fun `refresh re-fetches metrics for the currently selected hostel`() {
        every { hostelPreferences.selectedHostelId } returns flowOf("h1")
        coEvery { hostelRepository.getById("h1") } returns Hostel(id = "h1", wardenId = "w1", name = "Sunrise", address = "", contactPhone = "", createdAt = 0L, updatedAt = 0L)
        every { floorRepository.getByHostelId("h1") } returns flowOf(emptyList())
        coEvery { tenancyAgreementRepository.getAll() } returns emptyList()
        every { invoiceRepository.getAll() } returns flowOf(emptyList())
        coEvery { paymentRepository.getAll() } returns emptyList()

        val viewModel = viewModel()
        viewModel.refresh()

        coVerify(exactly = 2) { hostelRepository.getById("h1") }
    }
}
