package com.lodgy.app.ui.tenant

import androidx.lifecycle.SavedStateHandle
import com.lodgy.app.data.entity.AgreementStatus
import com.lodgy.app.data.entity.TenancyAgreement
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.TenancyAgreementRepository
import com.lodgy.app.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ForgoneRentViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val agreementRepository: TenancyAgreementRepository = mockk()
    private val bedRepository: BedRepository = mockk()

    private fun agreement(
        nonRevenue: Boolean = true,
        forgoneRentExpense: Boolean = false,
        forgoneRentAmount: Double? = null,
    ) = TenancyAgreement(
        id = "a1",
        tenantId = "t1",
        bedId = "b1",
        agreedRent = 0.0,
        advanceDeposit = 0.0,
        billingCycleDay = 5,
        nonRevenue = nonRevenue,
        forgoneRentExpense = forgoneRentExpense,
        forgoneRentAmount = forgoneRentAmount,
        moveInDate = 0L,
        moveOutDate = null,
        depositRefundAmount = null,
        status = AgreementStatus.ACTIVE,
        createdAt = 0L,
        updatedAt = 0L,
    )

    private fun viewModel(active: TenancyAgreement? = agreement()): ForgoneRentViewModel {
        coEvery { agreementRepository.getActiveByTenantId("t1") } returns active
        coEvery { bedRepository.getRoomPrice("b1") } returns 6000.0
        return ForgoneRentViewModel(
            agreementRepository, bedRepository, SavedStateHandle(mapOf("tenantId" to "t1")),
        )
    }

    @Test
    fun `it is off by default and offers the room's own rate as the amount`() {
        val state = viewModel().uiState.value

        assertTrue(state.hasNonRevenueTenancy)
        assertFalse(state.enabled)
        assertEquals("6000.0", state.amount)
    }

    @Test
    fun `an amount the warden already chose wins over the room rate`() {
        val state = viewModel(agreement(forgoneRentExpense = true, forgoneRentAmount = 4500.0)).uiState.value

        assertTrue(state.enabled)
        assertEquals("4500.0", state.amount)
    }

    /** LODGY-84: a paying tenancy has no rent to forgo, so the screen refuses rather than
     *  offering to book an expense against money that is actually being collected. */
    @Test
    fun `a paying tenancy is not offered this at all`() {
        val state = viewModel(agreement(nonRevenue = false)).uiState.value

        assertFalse(state.hasNonRevenueTenancy)
    }

    @Test
    fun `a tenant with no active tenancy is not offered this either`() {
        val state = viewModel(active = null).uiState.value

        assertFalse(state.hasNonRevenueTenancy)
    }

    @Test
    fun `switching it on saves the switch and the amount`() {
        coEvery { agreementRepository.setForgoneRentExpense(any(), any(), any()) } returns Unit
        val viewModel = viewModel()

        viewModel.onEnabledChange(true)
        viewModel.onAmountChange("4500")
        viewModel.save()

        coVerify { agreementRepository.setForgoneRentExpense(any(), true, 4500.0) }
        assertTrue(viewModel.uiState.value.saved)
    }

    /** Turning it off stops future months. The amount is kept so switching back on later offers
     *  the figure the warden chose, and nothing already recorded is touched. */
    @Test
    fun `switching it off keeps the amount for next time`() {
        coEvery { agreementRepository.setForgoneRentExpense(any(), any(), any()) } returns Unit
        val viewModel = viewModel(agreement(forgoneRentExpense = true, forgoneRentAmount = 4500.0))

        viewModel.onEnabledChange(false)
        viewModel.save()

        coVerify { agreementRepository.setForgoneRentExpense(any(), false, 4500.0) }
    }

    @Test
    fun `it cannot be switched on without an amount`() {
        val viewModel = viewModel()

        viewModel.onEnabledChange(true)
        viewModel.onAmountChange("")
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.onAmountChange("0")
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.onAmountChange("4500")
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun `save is a no-op when it cannot be saved`() {
        val viewModel = viewModel()

        viewModel.onEnabledChange(true)
        viewModel.onAmountChange("")
        viewModel.save()

        coVerify(exactly = 0) { agreementRepository.setForgoneRentExpense(any(), any(), any()) }
        assertFalse(viewModel.uiState.value.saved)
    }
}
