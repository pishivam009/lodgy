package com.lodgy.app.ui.backup

import com.lodgy.app.data.entity.AgreementStatus
import com.lodgy.app.data.entity.Bed
import com.lodgy.app.data.entity.BedStatus
import com.lodgy.app.data.entity.Credit
import com.lodgy.app.data.entity.Floor
import com.lodgy.app.data.entity.Hostel
import com.lodgy.app.data.entity.Invoice
import com.lodgy.app.data.entity.InvoiceStatus
import com.lodgy.app.data.entity.Payment
import com.lodgy.app.data.entity.PaymentMode
import com.lodgy.app.data.entity.Room
import com.lodgy.app.data.entity.RoomType
import com.lodgy.app.data.entity.TenancyAgreement
import com.lodgy.app.data.entity.Tenant
import com.lodgy.app.data.entity.TenantStatus
import com.lodgy.app.data.prefs.HostelPreferences
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.CreditRepository
import com.lodgy.app.data.repository.FloorRepository
import com.lodgy.app.data.repository.HostelRepository
import com.lodgy.app.data.repository.InvoiceRepository
import com.lodgy.app.data.repository.PaymentRepository
import com.lodgy.app.data.repository.RoomRepository
import com.lodgy.app.data.repository.TenancyAgreementRepository
import com.lodgy.app.data.repository.TenantRepository
import com.lodgy.app.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DataPacketViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val hostelPreferences: HostelPreferences = mockk()
    private val hostelRepository: HostelRepository = mockk()
    private val floorRepository: FloorRepository = mockk()
    private val roomRepository: RoomRepository = mockk()
    private val bedRepository: BedRepository = mockk()
    private val agreementRepository: TenancyAgreementRepository = mockk()
    private val tenantRepository: TenantRepository = mockk()
    private val invoiceRepository: InvoiceRepository = mockk()
    private val paymentRepository: PaymentRepository = mockk()
    private val creditRepository: CreditRepository = mockk()

    private val sunrise = Hostel(id = "h1", wardenId = "w1", name = "Sunrise", address = "Main Road", contactPhone = "", createdAt = 0L, updatedAt = 0L)
    private val moonlight = Hostel(id = "h2", wardenId = "w1", name = "Moonlight", address = "Second Road", contactPhone = "", createdAt = 0L, updatedAt = 0L)

    private val agreement = TenancyAgreement(id = "a1", tenantId = "t1", bedId = "b1", agreedRent = 5000.0, advanceDeposit = 0.0, billingCycleDay = 1, moveInDate = 1_000L, moveOutDate = null, depositRefundAmount = null, status = AgreementStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)

    @Before
    fun setUp() {
        every { hostelPreferences.selectedHostelId } returns flowOf("h1")
        every { hostelRepository.getAll() } returns flowOf(listOf(sunrise, moonlight))

        every { floorRepository.getByHostelId("h1") } returns flowOf(
            listOf(Floor(id = "f1", hostelId = "h1", label = "Ground", sortOrder = 0, createdAt = 0L, updatedAt = 0L)),
        )
        every { floorRepository.getByHostelId("h2") } returns flowOf(emptyList())
        every { roomRepository.getByFloorId("f1") } returns flowOf(
            listOf(Room(id = "r1", floorId = "f1", roomNumber = "101", type = RoomType.DOUBLE, pricePerBed = 5000.0, amenities = "", createdAt = 0L, updatedAt = 0L)),
        )
        every { bedRepository.getByRoomId("r1") } returns flowOf(
            listOf(
                Bed(id = "b1", roomId = "r1", label = "A", status = BedStatus.OCCUPIED, createdAt = 0L, updatedAt = 0L),
                Bed(id = "b2", roomId = "r1", label = "B", status = BedStatus.VACANT, createdAt = 0L, updatedAt = 0L),
            ),
        )
        coEvery { agreementRepository.getAll() } returns listOf(agreement)
        coEvery { tenantRepository.getById("t1") } returns Tenant(id = "t1", name = "Ravi", phone = "999", photoPath = null, idProofPhotoPath = null, emergencyContactName = "", emergencyContactPhone = "", status = TenantStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)
        every { invoiceRepository.getByTenancyAgreementId("a1") } returns flowOf(
            listOf(
                Invoice(id = "i2", tenancyAgreementId = "a1", periodMonth = 1, periodYear = 2026, amountDue = 5000.0, dueDate = 0L, status = InvoiceStatus.PAID, createdAt = 0L, updatedAt = 0L),
                Invoice(id = "i1", tenancyAgreementId = "a1", periodMonth = 12, periodYear = 2025, amountDue = 5000.0, dueDate = 0L, status = InvoiceStatus.PAID, createdAt = 0L, updatedAt = 0L),
            ),
        )
        coEvery { paymentRepository.getAll() } returns listOf(
            Payment(id = "p1", invoiceId = "i1", amount = 5000.0, paymentMode = PaymentMode.CASH, paidOn = 0L, note = null, createdAt = 0L, updatedAt = 0L),
        )
        coEvery { creditRepository.getAllOnce() } returns listOf(
            Credit(id = "c1", tenantId = "t1", invoiceId = "i2", amount = 1000.0, reason = "Plumbing", createdAt = 0L, updatedAt = 0L),
        )
    }

    private fun viewModel() = DataPacketViewModel(
        hostelPreferences, hostelRepository, floorRepository, roomRepository, bedRepository,
        agreementRepository, tenantRepository, invoiceRepository, paymentRepository, creditRepository,
    )

    @Test
    fun `defaults to the current hostel only`() {
        val state = viewModel().uiState.value

        assertEquals(listOf("Sunrise"), state.hostels.map { it.hostelName })
    }

    @Test
    fun `switching scope covers every hostel, empty ones included`() {
        val viewModel = viewModel()
        viewModel.onScopeChange(PacketScope.ALL_HOSTELS)

        assertEquals(listOf("Sunrise", "Moonlight"), viewModel.uiState.value.hostels.map { it.hostelName })
    }

    @Test
    fun `bed counts come from the beds themselves`() {
        val hostel = viewModel().uiState.value.hostels.single()

        assertEquals(2, hostel.totalBeds)
        assertEquals(1, hostel.occupiedBeds)
    }

    @Test
    fun `invoices are ordered oldest period first with credits already applied`() {
        val tenancy = viewModel().uiState.value.hostels.single().floors.single().tenancies.single()

        assertEquals(listOf(2025 to 12, 2026 to 1), tenancy.invoices.map { it.periodYear to it.periodMonth })
        assertEquals(5000.0, tenancy.invoices.first().amountDue, 0.0001)
        assertEquals(5000.0, tenancy.invoices.first().paid, 0.0001)
        assertEquals(4000.0, tenancy.invoices.last().amountDue, 0.0001)
        assertEquals(0.0, tenancy.invoices.last().paid, 0.0001)
    }

    @Test
    fun `a bed nobody has ever occupied contributes no tenancy row`() {
        val tenancies = viewModel().uiState.value.hostels.single().floors.single().tenancies

        assertEquals(1, tenancies.size)
        assertTrue(tenancies.single().active)
        assertEquals("Ravi", tenancies.single().tenantName)
    }
}
