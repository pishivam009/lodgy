package com.lodgy.app.ui.tenant

import androidx.lifecycle.SavedStateHandle
import com.lodgy.app.data.repository.BedRepository
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

    private fun viewModel() = AgreementFormViewModel(
        tenancyAgreementRepository, bedRepository, SavedStateHandle(mapOf("tenantId" to "t1", "bedId" to "b1")),
    )

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
        coEvery { tenancyAgreementRepository.create("t1", "b1", 5000.0, 2000.0, 5, any()) } returns mockk()
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
}
