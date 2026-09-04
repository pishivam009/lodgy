package com.lodgy.app.data.repository

import com.lodgy.app.data.dao.ReconciliationMarkDao
import com.lodgy.app.data.entity.ReconciliationMark
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ReconciliationRepositoryTest {

    private val dao: ReconciliationMarkDao = mockk()
    private val repository = ReconciliationRepository(dao)

    private val existing = ReconciliationMark(id = "m1", hostelId = "h1", periodMonth = 9, periodYear = 2026, note = null, createdAt = 10L, updatedAt = 10L)

    @Test
    fun `marking a fresh period inserts a new attestation`() = runTest {
        coEvery { dao.getForPeriod("h1", 9, 2026) } returns null
        val inserted = slot<ReconciliationMark>()
        coEvery { dao.insert(capture(inserted)) } returns Unit

        repository.mark("h1", 9, 2026, "checked with register")

        assertEquals("h1", inserted.captured.hostelId)
        assertEquals(9, inserted.captured.periodMonth)
        assertEquals(2026, inserted.captured.periodYear)
        assertEquals("checked with register", inserted.captured.note)
    }

    @Test
    fun `marking an already-marked period updates it in place rather than duplicating it`() = runTest {
        coEvery { dao.getForPeriod("h1", 9, 2026) } returns existing
        val inserted = slot<ReconciliationMark>()
        coEvery { dao.insert(capture(inserted)) } returns Unit

        repository.mark("h1", 9, 2026, "re-checked")

        assertEquals("m1", inserted.captured.id)
        assertEquals(10L, inserted.captured.createdAt)
        assertEquals("re-checked", inserted.captured.note)
    }

    @Test
    fun `unmarking deletes the attestation, so a mistake is reversible`() = runTest {
        coEvery { dao.getForPeriod("h1", 9, 2026) } returns existing
        coEvery { dao.delete(existing) } returns Unit

        repository.unmark("h1", 9, 2026)

        coVerify { dao.delete(existing) }
    }

    @Test
    fun `unmarking a period that was never marked does nothing`() = runTest {
        coEvery { dao.getForPeriod("h1", 9, 2026) } returns null

        repository.unmark("h1", 9, 2026)

        coVerify(exactly = 0) { dao.delete(any()) }
    }

    @Test
    fun `reads delegate to the dao`() = runTest {
        coEvery { dao.getForPeriod("h1", 9, 2026) } returns existing
        every { dao.getByHostelId("h1") } returns flowOf(listOf(existing))

        assertEquals(existing, repository.getForPeriod("h1", 9, 2026))
        assertEquals(listOf(existing), repository.getByHostelId("h1").first())
    }
}
