package com.lodgy.app.data.repository

import com.lodgy.app.data.dao.InvoiceDao
import com.lodgy.app.data.entity.Invoice
import com.lodgy.app.data.entity.InvoiceStatus
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InvoiceRepositoryTest {

    private val invoiceDao: InvoiceDao = mockk()
    private val repository = InvoiceRepository(invoiceDao)

    @Test
    fun `create persists a new invoice as UNPAID`() = runTest {
        val inserted = slot<Invoice>()
        coEvery { invoiceDao.insert(capture(inserted)) } returns Unit

        val created = repository.create("agreement-1", 9, 2026, 5000.0, 123L)

        assertEquals(InvoiceStatus.UNPAID, created.status)
        assertEquals("agreement-1", created.tenancyAgreementId)
        assertEquals(created, inserted.captured)
    }

    @Test
    fun `existsForPeriod is true when the dao finds a matching invoice`() = runTest {
        coEvery { invoiceDao.getForPeriod("a1", 9, 2026) } returns mockk()
        assertTrue(repository.existsForPeriod("a1", 9, 2026))
    }

    @Test
    fun `existsForPeriod is false when the dao finds nothing`() = runTest {
        coEvery { invoiceDao.getForPeriod("a1", 9, 2026) } returns null
        assertFalse(repository.existsForPeriod("a1", 9, 2026))
    }

    @Test
    fun `updateStatus copies the invoice with the new status`() = runTest {
        val invoice = Invoice(id = "i1", tenancyAgreementId = "a1", periodMonth = 9, periodYear = 2026, amountDue = 100.0, dueDate = 0L, status = InvoiceStatus.UNPAID, createdAt = 0L, updatedAt = 0L)
        val updated = slot<Invoice>()
        coEvery { invoiceDao.update(capture(updated)) } returns Unit

        repository.updateStatus(invoice, InvoiceStatus.PAID)

        assertEquals(InvoiceStatus.PAID, updated.captured.status)
        assertEquals("i1", updated.captured.id)
    }
}
