package com.lodgy.app.ui.payment

import com.lodgy.app.data.entity.Invoice
import com.lodgy.app.data.entity.InvoiceStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class InvoiceListUiStateTest {

    private fun item(
        status: InvoiceStatus,
        month: Int = 9,
        year: Int = 2026,
        amountDue: Double = 5000.0,
        dueDate: Long = 0L,
    ) = InvoiceListItem(
        invoice = Invoice(
            tenancyAgreementId = "a1",
            periodMonth = month,
            periodYear = year,
            amountDue = amountDue,
            dueDate = dueDate,
            status = status,
            createdAt = 0L,
            updatedAt = 0L,
        ),
        tenantName = "Tenant",
        location = null,
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

    @Test
    fun `a blank period filter leaves every period visible`() {
        val state = InvoiceListUiState(
            items = listOf(item(InvoiceStatus.UNPAID, month = 8), item(InvoiceStatus.UNPAID, month = 9)),
        )
        assertEquals(2, state.filteredItems.size)
    }

    @Test
    fun `month and year narrow independently`() {
        val items = listOf(
            item(InvoiceStatus.UNPAID, month = 9, year = 2026),
            item(InvoiceStatus.UNPAID, month = 8, year = 2026),
            item(InvoiceStatus.UNPAID, month = 9, year = 2025),
        )
        assertEquals(2, InvoiceListUiState(items = items, periodMonth = "9").filteredItems.size)
        assertEquals(2, InvoiceListUiState(items = items, periodYear = "2026").filteredItems.size)
        assertEquals(1, InvoiceListUiState(items = items, periodMonth = "9", periodYear = "2026").filteredItems.size)
    }

    @Test
    fun `a status filter and a period filter both apply, neither replaces the other`() {
        val items = listOf(
            item(InvoiceStatus.UNPAID, month = 9),
            item(InvoiceStatus.PAID, month = 9),
            item(InvoiceStatus.UNPAID, month = 8),
        )
        val state = InvoiceListUiState(items = items, filter = InvoiceFilter.UNPAID, periodMonth = "9")

        assertEquals(1, state.filteredItems.size)
        assertEquals(InvoiceStatus.UNPAID, state.filteredItems.single().invoice.status)
        assertEquals(9, state.filteredItems.single().invoice.periodMonth)
    }

    @Test
    fun `sorting switches between newest due date and largest amount`() {
        val items = listOf(
            item(InvoiceStatus.UNPAID, dueDate = 100L, amountDue = 9000.0),
            item(InvoiceStatus.UNPAID, dueDate = 300L, amountDue = 1000.0),
        )
        assertEquals(
            listOf(300L, 100L),
            InvoiceListUiState(items = items, sort = InvoiceSort.DUE_DATE).filteredItems.map { it.invoice.dueDate },
        )
        assertEquals(
            listOf(9000.0, 1000.0),
            InvoiceListUiState(items = items, sort = InvoiceSort.AMOUNT).filteredItems.map { it.invoice.amountDue },
        )
    }
}
