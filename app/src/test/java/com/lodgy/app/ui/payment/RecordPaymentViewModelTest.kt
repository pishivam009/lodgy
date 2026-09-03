package com.lodgy.app.ui.payment

import androidx.lifecycle.SavedStateHandle
import com.lodgy.app.data.entity.Invoice
import com.lodgy.app.data.entity.InvoiceStatus
import com.lodgy.app.data.entity.PaymentMode
import com.lodgy.app.data.entity.Tenant
import com.lodgy.app.data.entity.TenancyAgreement
import com.lodgy.app.data.entity.AgreementStatus
import com.lodgy.app.data.entity.TenantStatus
import com.lodgy.app.data.repository.InvoiceRepository
import com.lodgy.app.data.repository.PaymentRepository
import com.lodgy.app.data.repository.TenancyAgreementRepository
import com.lodgy.app.data.repository.TenantRepository
import com.lodgy.app.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RecordPaymentViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val invoiceRepository: InvoiceRepository = mockk()
    private val paymentRepository: PaymentRepository = mockk()
    private val tenancyAgreementRepository: TenancyAgreementRepository = mockk()
    private val tenantRepository: TenantRepository = mockk()

    private val invoice = Invoice(id = "inv-1", tenancyAgreementId = "a1", periodMonth = 9, periodYear = 2026, amountDue = 5000.0, dueDate = 0L, status = InvoiceStatus.UNPAID, createdAt = 0L, updatedAt = 0L)
    private val agreement = TenancyAgreement(id = "a1", tenantId = "t1", bedId = "b1", agreedRent = 5000.0, advanceDeposit = 0.0, billingCycleDay = 1, moveInDate = 0L, moveOutDate = null, depositRefundAmount = null, status = AgreementStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)
    private val tenant = Tenant(id = "t1", name = "Ravi", phone = "1", photoPath = null, idProofPhotoPath = null, emergencyContactName = "e", emergencyContactPhone = "2", status = TenantStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)

    private fun viewModel(alreadyPaid: Double = 0.0): RecordPaymentViewModel {
        coEvery { invoiceRepository.getById("inv-1") } returns invoice
        coEvery { tenancyAgreementRepository.getById("a1") } returns agreement
        coEvery { tenantRepository.getById("t1") } returns tenant
        coEvery { paymentRepository.getTotalPaid("inv-1") } returns alreadyPaid
        return RecordPaymentViewModel(
            invoiceRepository, paymentRepository, tenancyAgreementRepository, tenantRepository,
            SavedStateHandle(mapOf("invoiceId" to "inv-1")),
        )
    }

    @Test
    fun `loads tenant name and prefills the remaining amount due`() {
        val state = viewModel(alreadyPaid = 1000.0).uiState.value

        assertFalse(state.loading)
        assertEquals("Ravi", state.tenantName)
        assertEquals(5000.0, state.amountDue, 0.0001)
        assertEquals(1000.0, state.alreadyPaid, 0.0001)
        assertEquals("4000.0", state.amount)
    }

    @Test
    fun `prefilled amount is blank once the invoice is already fully paid`() {
        val state = viewModel(alreadyPaid = 5000.0).uiState.value
        assertEquals("", state.amount)
    }

    @Test
    fun `canSave requires a positive numeric amount`() {
        val viewModel = viewModel()
        viewModel.onAmountChange("")
        assertFalse(viewModel.uiState.value.canSave)
        viewModel.onAmountChange("abc")
        assertFalse(viewModel.uiState.value.canSave)
        viewModel.onAmountChange("0")
        assertFalse(viewModel.uiState.value.canSave)
        viewModel.onAmountChange("500")
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun `save marks the invoice PAID once the total collected meets the amount due`() {
        val viewModel = viewModel(alreadyPaid = 0.0)
        coEvery { paymentRepository.create(any(), any(), any(), any(), any()) } returns mockk()
        coEvery { paymentRepository.getTotalPaid("inv-1") } returns 5000.0
        val statusSlot = slot<InvoiceStatus>()
        coEvery { invoiceRepository.updateStatus(invoice, capture(statusSlot)) } returns Unit

        viewModel.onAmountChange("5000")
        viewModel.save()

        assertEquals(InvoiceStatus.PAID, statusSlot.captured)
        assertTrue(viewModel.uiState.value.saved)
    }

    @Test
    fun `save marks the invoice PARTIAL when something but not everything has been collected`() {
        val viewModel = viewModel(alreadyPaid = 0.0)
        coEvery { paymentRepository.create(any(), any(), any(), any(), any()) } returns mockk()
        coEvery { paymentRepository.getTotalPaid("inv-1") } returns 2000.0
        val statusSlot = slot<InvoiceStatus>()
        coEvery { invoiceRepository.updateStatus(invoice, capture(statusSlot)) } returns Unit

        viewModel.onAmountChange("2000")
        viewModel.save()

        assertEquals(InvoiceStatus.PARTIAL, statusSlot.captured)
    }

    @Test
    fun `save records the note as null when it is left blank`() {
        val viewModel = viewModel(alreadyPaid = 0.0)
        coEvery { paymentRepository.create("inv-1", any(), PaymentMode.CASH, any(), isNull()) } returns mockk()
        coEvery { paymentRepository.getTotalPaid("inv-1") } returns 500.0
        coEvery { invoiceRepository.updateStatus(any(), any()) } returns Unit

        viewModel.onAmountChange("500")
        viewModel.onNoteChange("   ")
        viewModel.save()

        coVerify { paymentRepository.create("inv-1", 500.0, PaymentMode.CASH, any(), isNull()) }
    }

    @Test
    fun `save does nothing when the amount is not a valid number`() {
        val viewModel = viewModel()
        viewModel.onAmountChange("not a number")

        viewModel.save()

        coVerify(exactly = 0) { paymentRepository.create(any(), any(), any(), any(), any()) }
    }
}
