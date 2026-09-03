package com.lodgy.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class InvoiceTotalsTest {

    @Test
    fun `a credit reduces the amount due by its value`() {
        assertEquals(4500.0, effectiveAmountDue(5000.0, 500.0), 0.0001)
    }

    @Test
    fun `no credit leaves the invoice untouched`() {
        assertEquals(5000.0, effectiveAmountDue(5000.0, 0.0), 0.0001)
    }

    @Test
    fun `a credit larger than the invoice clears it rather than going negative`() {
        assertEquals(0.0, effectiveAmountDue(5000.0, 8000.0), 0.0001)
    }
}
