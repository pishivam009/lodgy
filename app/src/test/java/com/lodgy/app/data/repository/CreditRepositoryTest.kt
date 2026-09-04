package com.lodgy.app.data.repository

import com.lodgy.app.data.dao.CreditDao
import com.lodgy.app.data.entity.Credit
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CreditRepositoryTest {

    private val creditDao: CreditDao = mockk()
    private val repository = CreditRepository(creditDao)

    private fun credit(id: String, invoiceId: String?) =
        Credit(id = id, tenantId = "t1", invoiceId = invoiceId, amount = 500.0, reason = "Plumbing", createdAt = 0L, updatedAt = 0L)

    @Test
    fun `create stores the credit and hands it back`() = runTest {
        val inserted = slot<Credit>()
        coEvery { creditDao.insert(capture(inserted)) } returns Unit

        val created = repository.create("t1", "inv-1", 750.0, "Geyser repair")

        assertEquals("t1", inserted.captured.tenantId)
        assertEquals("inv-1", inserted.captured.invoiceId)
        assertEquals(750.0, inserted.captured.amount, 0.0001)
        assertEquals("Geyser repair", inserted.captured.reason)
        assertEquals(inserted.captured.id, created.id)
    }

    @Test
    fun `a credit with no invoice is stored as pending`() = runTest {
        val inserted = slot<Credit>()
        coEvery { creditDao.insert(capture(inserted)) } returns Unit

        repository.create("t1", null, 500.0, "Plumbing")

        assertNull(inserted.captured.invoiceId)
    }

    @Test
    fun `applyPendingTo attaches every unapplied credit to the new invoice`() = runTest {
        coEvery { creditDao.getPendingByTenantId("t1") } returns listOf(credit("c1", null), credit("c2", null))
        val updates = mutableListOf<Credit>()
        coEvery { creditDao.update(capture(updates)) } returns Unit

        repository.applyPendingTo("t1", "inv-9")

        assertEquals(listOf("inv-9", "inv-9"), updates.map { it.invoiceId })
        assertEquals(listOf("c1", "c2"), updates.map { it.id })
    }

    @Test
    fun `applyPendingTo touches nothing when the tenant owes no credits`() = runTest {
        coEvery { creditDao.getPendingByTenantId("t1") } returns emptyList()

        repository.applyPendingTo("t1", "inv-9")

        coVerify(exactly = 0) { creditDao.update(any()) }
    }

    @Test
    fun `reads delegate to the dao`() = runTest {
        val rows = listOf(credit("c1", "inv-1"))
        coEvery { creditDao.getByInvoiceId("inv-1") } returns rows
        every { creditDao.getByTenantId("t1") } returns flowOf(rows)
        every { creditDao.getAll() } returns flowOf(rows)
        coEvery { creditDao.getAllOnce() } returns rows

        assertEquals(rows, repository.getByInvoiceId("inv-1"))
        assertEquals(rows, repository.getByTenantId("t1").first())
        assertEquals(rows, repository.getAll().first())
        assertEquals(rows, repository.getAllOnce())
    }

    @Test
    fun `delete removes the credit`() = runTest {
        val target = credit("c1", "inv-1")
        coEvery { creditDao.delete(target) } returns Unit

        repository.delete(target)

        coVerify { creditDao.delete(target) }
    }
}
