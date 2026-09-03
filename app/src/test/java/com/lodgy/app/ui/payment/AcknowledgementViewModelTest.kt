package com.lodgy.app.ui.payment

import androidx.lifecycle.SavedStateHandle
import com.lodgy.app.data.dao.BedLocation
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AcknowledgementViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val invoiceRepository: InvoiceRepository = mockk()
    private val paymentRepository: PaymentRepository = mockk()
    private val creditRepository: CreditRepository = mockk()
    private val agreementRepository: TenancyAgreementRepository = mockk()
    private val tenantRepository: TenantRepository = mockk()
    private val bedRepository: BedRepository = mockk()
    private val roomRepository: RoomRepository = mockk()
    private val floorRepository: FloorRepository = mockk()
    private val hostelRepository: HostelRepository = mockk()

    private val invoice = Invoice(id = "inv-1", tenancyAgreementId = "a1", periodMonth = 9, periodYear = 2026, amountDue = 5000.0, dueDate = 0L, status = InvoiceStatus.PARTIAL, createdAt = 0L, updatedAt = 0L)

    private fun viewModel(
        found: Boolean = true,
        payments: List<Payment> = emptyList(),
        credits: List<Credit> = emptyList(),
    ): AcknowledgementViewModel {
        coEvery { invoiceRepository.getById("inv-1") } returns if (found) invoice else null
        coEvery { agreementRepository.getById("a1") } returns TenancyAgreement(id = "a1", tenantId = "t1", bedId = "b1", agreedRent = 5000.0, advanceDeposit = 0.0, billingCycleDay = 1, moveInDate = 0L, moveOutDate = null, depositRefundAmount = null, status = AgreementStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)
        coEvery { tenantRepository.getById("t1") } returns Tenant(id = "t1", name = "Ravi", phone = "1", photoPath = null, idProofPhotoPath = null, emergencyContactName = "", emergencyContactPhone = "", status = TenantStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)
        coEvery { bedRepository.getById("b1") } returns Bed(id = "b1", roomId = "r1", label = "B", status = BedStatus.OCCUPIED, createdAt = 0L, updatedAt = 0L)
        coEvery { bedRepository.getLocation("b1") } returns BedLocation("204", "B")
        coEvery { roomRepository.getById("r1") } returns Room(id = "r1", floorId = "f1", roomNumber = "204", type = RoomType.DOUBLE, pricePerBed = 5000.0, amenities = "", createdAt = 0L, updatedAt = 0L)
        coEvery { floorRepository.getById("f1") } returns Floor(id = "f1", hostelId = "h1", label = "Second", sortOrder = 2, createdAt = 0L, updatedAt = 0L)
        coEvery { hostelRepository.getById("h1") } returns Hostel(id = "h1", wardenId = "w1", name = "Sunrise", address = "", contactPhone = "", createdAt = 0L, updatedAt = 0L)
        every { paymentRepository.getByInvoiceId("inv-1") } returns flowOf(payments)
        coEvery { creditRepository.getByInvoiceId("inv-1") } returns credits
        return AcknowledgementViewModel(
            invoiceRepository, paymentRepository, creditRepository, agreementRepository, tenantRepository,
            bedRepository, roomRepository, floorRepository, hostelRepository,
            SavedStateHandle(mapOf("invoiceId" to "inv-1")),
        )
    }

    private fun payment(id: String, amount: Double, paidOn: Long) =
        Payment(id = id, invoiceId = "inv-1", amount = amount, paymentMode = PaymentMode.CASH, paidOn = paidOn, note = null, createdAt = 0L, updatedAt = 0L)

    @Test
    fun `gathers tenant, room, hostel and period for the receipt`() {
        val state = viewModel().uiState.value

        assertTrue(state.found)
        assertEquals("Ravi", state.tenantName)
        assertEquals("Sunrise", state.hostelName)
        assertEquals(BedLocation("204", "B"), state.location)
        assertEquals(9, state.periodMonth)
        assertEquals(2026, state.periodYear)
    }

    @Test
    fun `payments are listed oldest first and totalled`() {
        val state = viewModel(payments = listOf(payment("p2", 1000.0, 200L), payment("p1", 2000.0, 100L))).uiState.value

        assertEquals(listOf("p1", "p2"), state.payments.map { it.id })
        assertEquals(3000.0, state.totalPaid, 0.0001)
        assertEquals(2000.0, state.balance, 0.0001)
    }

    @Test
    fun `a credit reduces the amount due and therefore the balance`() {
        val credits = listOf(Credit(id = "c1", tenantId = "t1", invoiceId = "inv-1", amount = 1500.0, reason = "Plumbing", createdAt = 0L, updatedAt = 0L))

        val state = viewModel(payments = listOf(payment("p1", 2000.0, 100L)), credits = credits).uiState.value

        assertEquals(3500.0, state.amountDue, 0.0001)
        assertEquals(1500.0, state.balance, 0.0001)
    }

    @Test
    fun `overpaying reads as settled rather than a negative balance`() {
        val state = viewModel(payments = listOf(payment("p1", 9000.0, 100L))).uiState.value

        assertEquals(0.0, state.balance, 0.0001)
    }

    @Test
    fun `a deleted invoice reports not found instead of rendering blanks`() {
        val state = viewModel(found = false).uiState.value

        assertFalse(state.loading)
        assertFalse(state.found)
    }
}
