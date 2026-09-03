package com.lodgy.app.ui.tenant

import androidx.lifecycle.SavedStateHandle
import com.lodgy.app.data.entity.AgreementStatus
import com.lodgy.app.data.entity.Tenant
import com.lodgy.app.data.entity.TenancyAgreement
import com.lodgy.app.data.entity.TenantStatus
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.TenancyAgreementRepository
import com.lodgy.app.data.repository.TenantRepository
import com.lodgy.app.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CheckoutViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val tenancyAgreementRepository: TenancyAgreementRepository = mockk()
    private val tenantRepository: TenantRepository = mockk()
    private val bedRepository: BedRepository = mockk()

    private val tenant = Tenant(id = "t1", name = "Ravi", phone = "1", photoPath = null, idProofPhotoPath = null, emergencyContactName = "", emergencyContactPhone = "", status = TenantStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)
    private val agreement = TenancyAgreement(id = "a1", tenantId = "t1", bedId = "b1", agreedRent = 5000.0, advanceDeposit = 2000.0, billingCycleDay = 1, moveInDate = 0L, moveOutDate = null, depositRefundAmount = null, status = AgreementStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)

    private fun viewModel(hasAgreement: Boolean = true): CheckoutViewModel {
        coEvery { tenantRepository.getById("t1") } returns tenant
        coEvery { tenancyAgreementRepository.getActiveByTenantId("t1") } returns if (hasAgreement) agreement else null
        return CheckoutViewModel(tenancyAgreementRepository, tenantRepository, bedRepository, SavedStateHandle(mapOf("tenantId" to "t1")))
    }

    @Test
    fun `loads the tenant and their advance deposit`() {
        val state = viewModel().uiState.value
        assertFalse(state.loading)
        assertTrue(state.hasActiveAgreement)
        assertEquals("Ravi", state.tenantName)
        assertEquals(2000.0, state.advanceDeposit, 0.0001)
    }

    @Test
    fun `no active agreement means nothing to check out`() {
        val state = viewModel(hasAgreement = false).uiState.value
        assertFalse(state.hasActiveAgreement)
        assertEquals(0.0, state.advanceDeposit, 0.0001)
    }

    @Test
    fun `confirmCheckout closes the agreement with the computed refund, frees the bed and vacates the tenant`() {
        val viewModel = viewModel()
        coEvery { tenancyAgreementRepository.close(agreement, any(), 1500.0) } returns Unit
        coEvery { bedRepository.setVacant("b1") } returns Unit
        coEvery { tenantRepository.setVacated(tenant) } returns Unit

        viewModel.onDamageDeductionChange("500")
        viewModel.confirmCheckout()

        coVerify { tenancyAgreementRepository.close(agreement, any(), 1500.0) }
        coVerify { bedRepository.setVacant("b1") }
        coVerify { tenantRepository.setVacated(tenant) }
        assertTrue(viewModel.uiState.value.saved)
    }

    @Test
    fun `confirmCheckout does nothing without an active agreement`() {
        val viewModel = viewModel(hasAgreement = false)

        viewModel.confirmCheckout()

        coVerify(exactly = 0) { tenancyAgreementRepository.close(any(), any(), any()) }
    }
}
