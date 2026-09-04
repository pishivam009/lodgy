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
        every { hostelRepository.getAll() } returns flowOf(emptyList())

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
        every { hostelRepository.getAll() } returns flowOf(listOf(Hostel(id = "h1", wardenId = "w1", name = "Sunrise", address = "", contactPhone = "", createdAt = 0L, updatedAt = 0L)))

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
        assertEquals(listOf("Sunrise"), state.hostels.map { it.name })
        assertEquals(1, state.vacantBedCount)
        assertEquals(1, state.overdueInvoiceCount)
        assertEquals(5000.0, state.todaysCollections, 0.0001)
        assertEquals(listOf("Soon Tenant", "Later Tenant"), state.upcomingMoveOuts.map { it.tenantName })
    }

    @Test
    fun `a checked-out tenant's invoices and payments still count toward the metrics`() {
        val now = System.currentTimeMillis()
        val yesterday = now - TimeUnit.DAYS.toMillis(1)
        every { hostelRepository.getAll() } returns flowOf(listOf(Hostel(id = "h1", wardenId = "w1", name = "Sunrise", address = "", contactPhone = "", createdAt = 0L, updatedAt = 0L)))
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
        every { hostelRepository.getAll() } returns flowOf(listOf(Hostel(id = "h1", wardenId = "w1", name = "Sunrise", address = "", contactPhone = "", createdAt = 0L, updatedAt = 0L)))
        every { floorRepository.getByHostelId("h1") } returns flowOf(emptyList())
        coEvery { tenancyAgreementRepository.getAll() } returns emptyList()
        every { invoiceRepository.getAll() } returns flowOf(emptyList())
        coEvery { paymentRepository.getAll() } returns emptyList()

        val viewModel = viewModel()
        viewModel.refresh()

        coVerify(atLeast = 1) { tenancyAgreementRepository.getAll() }
    }

    /** LODGY-81: the dashboard used to follow the selected-hostel preference, so a warden with
     *  three properties had no single view of their business. It now aggregates by default. */
    @Test
    fun `figures cover every hostel by default, and the filter narrows to one`() {
        val h1 = Hostel(id = "h1", wardenId = "w1", name = "Sunrise", address = "", contactPhone = "", createdAt = 0L, updatedAt = 0L)
        val h2 = Hostel(id = "h2", wardenId = "w1", name = "Moonlight", address = "", contactPhone = "", createdAt = 0L, updatedAt = 0L)
        every { hostelRepository.getAll() } returns flowOf(listOf(h1, h2))

        every { floorRepository.getByHostelId("h1") } returns flowOf(listOf(
            Floor(id = "f1", hostelId = "h1", label = "G", sortOrder = 0, createdAt = 0L, updatedAt = 0L)))
        every { floorRepository.getByHostelId("h2") } returns flowOf(listOf(
            Floor(id = "f2", hostelId = "h2", label = "G", sortOrder = 0, createdAt = 0L, updatedAt = 0L)))
        every { roomRepository.getByFloorId("f1") } returns flowOf(listOf(
            Room(id = "r1", floorId = "f1", roomNumber = "101", type = RoomType.SINGLE, pricePerBed = 1.0, amenities = "", createdAt = 0L, updatedAt = 0L)))
        every { roomRepository.getByFloorId("f2") } returns flowOf(listOf(
            Room(id = "r2", floorId = "f2", roomNumber = "201", type = RoomType.SINGLE, pricePerBed = 1.0, amenities = "", createdAt = 0L, updatedAt = 0L)))
        every { bedRepository.getByRoomId("r1") } returns flowOf(listOf(
            Bed(id = "b1", roomId = "r1", label = "A", status = BedStatus.VACANT, createdAt = 0L, updatedAt = 0L)))
        every { bedRepository.getByRoomId("r2") } returns flowOf(listOf(
            Bed(id = "b2", roomId = "r2", label = "A", status = BedStatus.VACANT, createdAt = 0L, updatedAt = 0L)))

        coEvery { tenancyAgreementRepository.getAll() } returns emptyList()
        every { invoiceRepository.getAll() } returns flowOf(emptyList())
        coEvery { paymentRepository.getAll() } returns emptyList()

        val viewModel = viewModel()

        // Default: both properties' vacant beds, and both offered as filter options.
        assertEquals(2, viewModel.uiState.value.vacantBedCount)
        assertEquals(listOf("Sunrise", "Moonlight"), viewModel.uiState.value.hostels.map { it.name })
        assertEquals(null, viewModel.uiState.value.filterHostelId)

        // Narrowed: only that property's bed, and the scope is nameable so the UI can label it.
        viewModel.onHostelFilterChange("h2")
        assertEquals(1, viewModel.uiState.value.vacantBedCount)
        assertEquals("Moonlight", viewModel.uiState.value.filterHostelName)

        // And back.
        viewModel.onHostelFilterChange(null)
        assertEquals(2, viewModel.uiState.value.vacantBedCount)
        assertEquals(null, viewModel.uiState.value.filterHostelName)
    }
}
