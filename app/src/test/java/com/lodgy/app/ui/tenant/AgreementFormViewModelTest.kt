package com.lodgy.app.ui.tenant

import androidx.lifecycle.SavedStateHandle
import java.util.Calendar
import com.lodgy.app.data.entity.AgreementStatus
import com.lodgy.app.data.entity.TenancyAgreement
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.InvoiceRepository
import com.lodgy.app.data.repository.TenancyAgreementRepository
import com.lodgy.app.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AgreementFormViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val tenancyAgreementRepository: TenancyAgreementRepository = mockk()
    private val bedRepository: BedRepository = mockk()
    private val invoiceRepository: InvoiceRepository = mockk()

    private fun viewModel() = AgreementFormViewModel(
        tenancyAgreementRepository, bedRepository, invoiceRepository,
        SavedStateHandle(mapOf("tenantId" to "t1", "bedId" to "b1")),
    )

    private val agreement = TenancyAgreement(id = "a1", tenantId = "t1", bedId = "b1", agreedRent = 5000.0, advanceDeposit = 2000.0, billingCycleDay = 5, moveInDate = 0L, moveOutDate = null, depositRefundAmount = null, status = AgreementStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)

    @Test
    fun `billingCycleDayValid rejects days outside 1 to 28`() {
        val viewModel = viewModel()
        viewModel.onBillingCycleDayChange("0")
        assertFalse(viewModel.uiState.value.billingCycleDayValid)
        viewModel.onBillingCycleDayChange("29")
        assertFalse(viewModel.uiState.value.billingCycleDayValid)
        viewModel.onBillingCycleDayChange("28")
        assertTrue(viewModel.uiState.value.billingCycleDayValid)
    }

    @Test
    fun `canSave requires numeric rent, deposit and a valid billing day`() {
        val viewModel = viewModel()
        viewModel.onAgreedRentChange("5000")
        viewModel.onAdvanceDepositChange("2000")
        assertFalse(viewModel.uiState.value.canSave)
        viewModel.onBillingCycleDayChange("5")
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun `save creates the agreement and marks the bed occupied`() {
        coEvery { tenancyAgreementRepository.create("t1", "b1", 5000.0, 2000.0, 5, any()) } returns agreement
        coEvery { bedRepository.setOccupied("b1") } returns Unit

        val viewModel = viewModel()
        viewModel.onAgreedRentChange("5000")
        viewModel.onAdvanceDepositChange("2000")
        viewModel.onBillingCycleDayChange("5")
        viewModel.save()

        coVerify { tenancyAgreementRepository.create("t1", "b1", 5000.0, 2000.0, 5, any()) }
        coVerify { bedRepository.setOccupied("b1") }
        assertTrue(viewModel.uiState.value.saved)
    }

    @Test
    fun `save does nothing while the form is invalid`() {
        val viewModel = viewModel()

        viewModel.save()

        coVerify(exactly = 0) { tenancyAgreementRepository.create(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `leaving the opening balance blank writes no invoice at all`() {
        coEvery { tenancyAgreementRepository.create("t1", "b1", 5000.0, 2000.0, 5, any()) } returns agreement
        coEvery { bedRepository.setOccupied("b1") } returns Unit

        val viewModel = viewModel()
        viewModel.onAgreedRentChange("5000")
        viewModel.onAdvanceDepositChange("2000")
        viewModel.onBillingCycleDayChange("5")
        viewModel.save()

        coVerify(exactly = 0) { invoiceRepository.create(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `carried-forward dues become one invoice, dated the month before move-in`() {
        coEvery { tenancyAgreementRepository.create("t1", "b1", 5000.0, 2000.0, 5, any()) } returns agreement
        coEvery { bedRepository.setOccupied("b1") } returns Unit
        coEvery { invoiceRepository.create(any(), any(), any(), any(), any()) } returns mockk()

        val moveIn = Calendar.getInstance().apply {
            set(2026, Calendar.MARCH, 10, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val viewModel = viewModel()
        viewModel.onAgreedRentChange("5000")
        viewModel.onAdvanceDepositChange("2000")
        viewModel.onBillingCycleDayChange("5")
        viewModel.onMoveInDateChange(moveIn)
        viewModel.onOpeningBalanceChange("12000")
        viewModel.save()

        coVerify { invoiceRepository.create("a1", 2, 2026, 12000.0, moveIn) }
    }

    @Test
    fun `a January move-in rolls the opening invoice back into the previous year`() {
        coEvery { tenancyAgreementRepository.create("t1", "b1", 5000.0, 2000.0, 5, any()) } returns agreement
        coEvery { bedRepository.setOccupied("b1") } returns Unit
        coEvery { invoiceRepository.create(any(), any(), any(), any(), any()) } returns mockk()

        val moveIn = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 4, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val viewModel = viewModel()
        viewModel.onAgreedRentChange("5000")
        viewModel.onAdvanceDepositChange("2000")
        viewModel.onBillingCycleDayChange("5")
        viewModel.onMoveInDateChange(moveIn)
        viewModel.onOpeningBalanceChange("3000")
        viewModel.save()

        coVerify { invoiceRepository.create("a1", 12, 2025, 3000.0, moveIn) }
    }

    @Test
    fun `a zero or unparseable opening balance writes nothing`() {
        coEvery { tenancyAgreementRepository.create("t1", "b1", 5000.0, 2000.0, 5, any()) } returns agreement
        coEvery { bedRepository.setOccupied("b1") } returns Unit

        val viewModel = viewModel()
        viewModel.onAgreedRentChange("5000")
        viewModel.onAdvanceDepositChange("2000")
        viewModel.onBillingCycleDayChange("5")
        viewModel.onOpeningBalanceChange("0")
        viewModel.save()

        viewModel.onOpeningBalanceChange("abc")
        viewModel.save()

        coVerify(exactly = 0) { invoiceRepository.create(any(), any(), any(), any(), any()) }
    }
}
