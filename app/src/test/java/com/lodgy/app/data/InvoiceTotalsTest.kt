package com.lodgy.app.data

import com.lodgy.app.data.entity.InvoiceStatus
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

    @Test
    fun `invoice status follows paid against effective due`() {
        assertEquals(InvoiceStatus.UNPAID, invoiceStatusFor(effectiveDue = 5000.0, totalPaid = 0.0))
        assertEquals(InvoiceStatus.PARTIAL, invoiceStatusFor(effectiveDue = 5000.0, totalPaid = 2000.0))
        assertEquals(InvoiceStatus.PAID, invoiceStatusFor(effectiveDue = 5000.0, totalPaid = 5000.0))
        // Overpayment still reads as paid.
        assertEquals(InvoiceStatus.PAID, invoiceStatusFor(effectiveDue = 5000.0, totalPaid = 6000.0))
        // Fully credited with nothing paid is settled, not unpaid.
        assertEquals(InvoiceStatus.PAID, invoiceStatusFor(effectiveDue = 0.0, totalPaid = 0.0))
    }
}
