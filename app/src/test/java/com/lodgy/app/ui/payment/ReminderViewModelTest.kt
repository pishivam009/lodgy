package com.lodgy.app.ui.payment

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.lodgy.app.contact.ReminderChannel
import com.lodgy.app.contact.ReminderLanguage
import com.lodgy.app.data.entity.AgreementStatus
import com.lodgy.app.data.entity.Bed
import com.lodgy.app.data.entity.BedStatus
import com.lodgy.app.data.entity.Floor
import com.lodgy.app.data.entity.Hostel
import com.lodgy.app.data.entity.Invoice
import com.lodgy.app.data.entity.InvoiceStatus
import com.lodgy.app.data.entity.Room
import com.lodgy.app.data.entity.RoomType
import com.lodgy.app.data.entity.Tenant
import com.lodgy.app.data.entity.TenancyAgreement
import com.lodgy.app.data.entity.TenantStatus
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.FloorRepository
import com.lodgy.app.data.repository.HostelRepository
import com.lodgy.app.data.repository.InvoiceRepository
import com.lodgy.app.data.repository.RoomRepository
import com.lodgy.app.data.repository.TenancyAgreementRepository
import com.lodgy.app.data.repository.TenantRepository
import com.lodgy.app.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class ReminderViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context: Context = mockk()
    private val invoiceRepository: InvoiceRepository = mockk()
    private val tenancyAgreementRepository: TenancyAgreementRepository = mockk()
    private val tenantRepository: TenantRepository = mockk()
    private val bedRepository: BedRepository = mockk()
    private val roomRepository: RoomRepository = mockk()
    private val floorRepository: FloorRepository = mockk()
    private val hostelRepository: HostelRepository = mockk()

    private fun viewModel(): ReminderViewModel {
        every { context.getString(any(), *anyVararg()) } returns "built message"

        val invoice = Invoice(id = "inv-1", tenancyAgreementId = "a1", periodMonth = 9, periodYear = 2026, amountDue = 5000.0, dueDate = 100L, status = InvoiceStatus.UNPAID, createdAt = 0L, updatedAt = 0L)
        val agreement = TenancyAgreement(id = "a1", tenantId = "t1", bedId = "b1", agreedRent = 5000.0, advanceDeposit = 0.0, billingCycleDay = 1, moveInDate = 0L, moveOutDate = null, depositRefundAmount = null, status = AgreementStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)
        val tenant = Tenant(id = "t1", name = "Ravi", phone = "999", photoPath = null, idProofPhotoPath = null, emergencyContactName = "", emergencyContactPhone = "", status = TenantStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)
        val bed = Bed(id = "b1", roomId = "r1", label = "A", status = BedStatus.OCCUPIED, createdAt = 0L, updatedAt = 0L)
        val room = Room(id = "r1", floorId = "f1", roomNumber = "101", type = RoomType.SINGLE, pricePerBed = 5000.0, amenities = "", createdAt = 0L, updatedAt = 0L)
        val floor = Floor(id = "f1", hostelId = "h1", label = "Ground", sortOrder = 0, createdAt = 0L, updatedAt = 0L)
        val hostel = Hostel(id = "h1", wardenId = "w1", name = "Sunrise", address = "", contactPhone = "", createdAt = 0L, updatedAt = 0L)

        coEvery { invoiceRepository.getById("inv-1") } returns invoice
        coEvery { tenancyAgreementRepository.getById("a1") } returns agreement
        coEvery { tenantRepository.getById("t1") } returns tenant
        coEvery { bedRepository.getById("b1") } returns bed
        coEvery { roomRepository.getById("r1") } returns room
        coEvery { floorRepository.getById("f1") } returns floor
        coEvery { hostelRepository.getById("h1") } returns hostel

        return ReminderViewModel(
            context, invoiceRepository, tenancyAgreementRepository, tenantRepository, bedRepository,
            roomRepository, floorRepository, hostelRepository, SavedStateHandle(mapOf("invoiceId" to "inv-1")),
        )
    }

    @Test
    fun `loads the tenant's phone and builds the default reminder message`() {
        val state = viewModel().uiState.value

        assertFalse(state.loading)
        assertEquals("999", state.tenantPhone)
        assertEquals("built message", state.message)
        assertEquals(ReminderChannel.WHATSAPP, state.channel)
        assertEquals(ReminderLanguage.HINDI, state.language)
    }

    @Test
    fun `onChannelChange only swaps the channel, leaving the message untouched`() {
        val viewModel = viewModel()
        viewModel.onChannelChange(ReminderChannel.SMS)
        assertEquals(ReminderChannel.SMS, viewModel.uiState.value.channel)
    }

    @Test
    fun `onLanguageChange swaps the language and rebuilds the message`() {
        val viewModel = viewModel()
        viewModel.onLanguageChange(ReminderLanguage.ENGLISH)

        assertEquals(ReminderLanguage.ENGLISH, viewModel.uiState.value.language)
        assertEquals("built message", viewModel.uiState.value.message)
    }
}
