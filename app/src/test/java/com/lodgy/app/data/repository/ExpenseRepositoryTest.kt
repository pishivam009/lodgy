package com.lodgy.app.data.repository

import com.lodgy.app.data.dao.ExpenseDao
import com.lodgy.app.data.entity.Expense
import com.lodgy.app.data.entity.ExpenseCategory
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ExpenseRepositoryTest {

    private val dao: ExpenseDao = mockk()
    private val repository = ExpenseRepository(dao)

    @Test
    fun `create persists an expense with the given fields`() = runTest {
        val inserted = slot<Expense>()
        coEvery { dao.insert(capture(inserted)) } returns Unit

        val created = repository.create("h1", ExpenseCategory.ELECTRICITY, 1200.0, true, 100L, "monthly bill")

        assertEquals(ExpenseCategory.ELECTRICITY, created.category)
        assertEquals(1200.0, created.amount, 0.0001)
        assertEquals(true, created.isRecurring)
        assertEquals("monthly bill", created.note)
        assertEquals(created, inserted.captured)
    }

    @Test
    fun `update replaces the mutable fields`() = runTest {
        val existing = Expense(id = "e1", hostelId = "h1", category = ExpenseCategory.WIFI, amount = 500.0, isRecurring = true, incurredOn = 0L, note = null, createdAt = 0L, updatedAt = 0L)
        val updated = slot<Expense>()
        coEvery { dao.update(capture(updated)) } returns Unit

        repository.update(existing, ExpenseCategory.REPAIR, 999.0, false, 200L, "fixed tap")

        assertEquals(ExpenseCategory.REPAIR, updated.captured.category)
        assertEquals(999.0, updated.captured.amount, 0.0001)
        assertEquals(false, updated.captured.isRecurring)
        assertEquals("fixed tap", updated.captured.note)
    }
}
