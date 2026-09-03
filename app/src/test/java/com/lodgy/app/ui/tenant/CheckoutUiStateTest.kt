package com.lodgy.app.ui.tenant

import org.junit.Assert.assertEquals
import org.junit.Test

class CheckoutUiStateTest {

    @Test
    fun `refund is deposit minus damage deduction`() {
        val state = CheckoutUiState(advanceDeposit = 5000.0, damageDeduction = "800")
        assertEquals(4200.0, state.refundAmount, 0.0)
    }

    @Test
    fun `blank damage deduction is treated as zero`() {
        val state = CheckoutUiState(advanceDeposit = 5000.0, damageDeduction = "")
        assertEquals(5000.0, state.refundAmount, 0.0)
    }

    @Test
    fun `non-numeric damage deduction is treated as zero rather than crashing`() {
        val state = CheckoutUiState(advanceDeposit = 5000.0, damageDeduction = "abc")
        assertEquals(5000.0, state.refundAmount, 0.0)
    }

    @Test
    fun `damage deduction larger than the deposit produces a negative refund`() {
        val state = CheckoutUiState(advanceDeposit = 1000.0, damageDeduction = "1500")
        assertEquals(-500.0, state.refundAmount, 0.0)
    }
}
