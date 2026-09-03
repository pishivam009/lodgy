package com.lodgy.app.ui.payment

import com.lodgy.app.data.entity.Invoice
import com.lodgy.app.data.entity.InvoiceStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class InvoiceListUiStateTest {

    private fun item(status: InvoiceStatus) = InvoiceListItem(
        invoice = Invoice(
            tenancyAgreementId = "a1",
            periodMonth = 9,
            periodYear = 2026,
            amountDue = 5000.0,
            dueDate = 0L,
            status = status,
            createdAt = 0L,
            updatedAt = 0L,
        ),
        tenantName = "Tenant",
        totalPaid = 0.0,
    )

    private val allItems = listOf(
        item(InvoiceStatus.UNPAID),
        item(InvoiceStatus.PARTIAL),
        item(InvoiceStatus.PAID),
        item(InvoiceStatus.PAID),
    )

    @Test
    fun `ALL filter returns every item unfiltered`() {
        val state = InvoiceListUiState(items = allItems, filter = InvoiceFilter.ALL)
        assertEquals(4, state.filteredItems.size)
    }

    @Test
    fun `UNPAID filter returns only unpaid invoices`() {
        val state = InvoiceListUiState(items = allItems, filter = InvoiceFilter.UNPAID)
        assertEquals(1, state.filteredItems.size)
        assertEquals(InvoiceStatus.UNPAID, state.filteredItems.single().invoice.status)
    }

    @Test
    fun `PAID filter returns every paid invoice`() {
        val state = InvoiceListUiState(items = allItems, filter = InvoiceFilter.PAID)
        assertEquals(2, state.filteredItems.size)
        assertEquals(true, state.filteredItems.all { it.invoice.status == InvoiceStatus.PAID })
    }
}
