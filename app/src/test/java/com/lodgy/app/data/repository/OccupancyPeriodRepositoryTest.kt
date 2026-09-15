package com.lodgy.app.data.repository

import com.lodgy.app.data.dao.OccupancyPeriodDao
import com.lodgy.app.data.dao.TenantStayRow
import com.lodgy.app.data.entity.OccupancyPeriod
import com.lodgy.app.data.entity.PropertyType
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

class OccupancyPeriodRepositoryTest {

    private val dao: OccupancyPeriodDao = mockk()
    private val repository = OccupancyPeriodRepository(dao)

    private fun period(id: String, tenancyAgreementId: String?, endDate: Long?) = OccupancyPeriod(
        id = id, tenantId = "t1", bedId = "b1", tenancyAgreementId = tenancyAgreementId,
        startDate = 100L, endDate = endDate, createdAt = 100L, updatedAt = 100L,
    )

    @Test
    fun `open persists a new period with no end date`() = runTest {
        val inserted = slot<OccupancyPeriod>()
        coEvery { dao.insert(capture(inserted)) } returns Unit

        val opened = repository.open("t1", "b1", "a1", 500L)

        assertEquals("t1", opened.tenantId)
        assertEquals("b1", opened.bedId)
        assertEquals("a1", opened.tenancyAgreementId)
        assertEquals(500L, opened.startDate)
        assertNull(opened.endDate)
        assertEquals(opened, inserted.captured)
    }

    @Test
    fun `open allows a null tenancy for a backfilled stay`() = runTest {
        coEvery { dao.insert(any()) } returns Unit

        val opened = repository.open("t1", "b1", null, 500L)

        assertNull(opened.tenancyAgreementId)
    }

    @Test
    fun `close ends the agreement's open period at the given date`() = runTest {
        val open = period("p1", tenancyAgreementId = "a1", endDate = null)
        coEvery { dao.getOpenByTenancyAgreementId("a1") } returns open
        val updated = slot<OccupancyPeriod>()
        coEvery { dao.update(capture(updated)) } returns Unit

        repository.close("a1", 900L)

        assertEquals(900L, updated.captured.endDate)
        assertEquals("p1", updated.captured.id)
    }

    @Test
    fun `close is a no-op when the agreement has no open period`() = runTest {
        coEvery { dao.getOpenByTenancyAgreementId("a1") } returns null

        repository.close("a1", 900L)

        coVerify(exactly = 0) { dao.update(any()) }
    }

    @Test
    fun `getByBedId returns the DAO's rows for that bed`() = runTest {
        val rows = listOf(period("p1", "a1", null), period("p2", "a2", 200L))
        coEvery { dao.getByBedId("b1") } returns rows

        assertEquals(rows, repository.getByBedId("b1"))
    }

    @Test
    fun `getByTenantId returns the DAO's rows for that tenant`() = runTest {
        val rows = listOf(period("p1", "a1", null))
        coEvery { dao.getByTenantId("t1") } returns rows

        assertEquals(rows, repository.getByTenantId("t1"))
    }

    @Test
    fun `backfill saves a period with no tenancy and marks it backfilled`() = runTest {
        coEvery { dao.getByBedId("b1") } returns emptyList()
        val inserted = slot<OccupancyPeriod>()
        coEvery { dao.insert(capture(inserted)) } returns Unit

        val outcome = repository.backfill("t1", "b1", 100L, 200L)

        val saved = (outcome as BackfillOutcome.Saved).period
        assertNull(saved.tenancyAgreementId)
        assertEquals(true, saved.backfilled)
        assertEquals(100L, saved.startDate)
        assertEquals(200L, saved.endDate)
        assertEquals(saved, inserted.captured)
    }

    @Test
    fun `backfill is refused when it would overlap an existing period on the bed`() = runTest {
        coEvery { dao.getByBedId("b1") } returns listOf(period("p1", null, endDate = null).copy(startDate = 150L))

        val outcome = repository.backfill("t1", "b1", 100L, 200L)

        assertEquals(BackfillOutcome.Overlaps, outcome)
        coVerify(exactly = 0) { dao.insert(any()) }
    }

    @Test
    fun `backfill allows two periods on the same bed that do not overlap`() = runTest {
        coEvery { dao.getByBedId("b1") } returns listOf(period("p1", null, endDate = 200L).copy(startDate = 100L))
        coEvery { dao.insert(any()) } returns Unit

        val outcome = repository.backfill("t2", "b1", 300L, 400L)

        assertEquals(true, outcome is BackfillOutcome.Saved)
    }

    @Test
    fun `backfill against an open-ended existing period overlaps any later start`() = runTest {
        coEvery { dao.getByBedId("b1") } returns listOf(period("p1", null, endDate = null).copy(startDate = 100L))

        val outcome = repository.backfill("t2", "b1", 500L, 600L)

        assertEquals(BackfillOutcome.Overlaps, outcome)
    }

    @Test
    fun `editBackfilled excludes the period being edited from its own overlap check`() = runTest {
        val existing = period("p1", null, endDate = 200L).copy(startDate = 100L, backfilled = true)
        coEvery { dao.getByBedId("b1") } returns listOf(existing)
        val updated = slot<OccupancyPeriod>()
        coEvery { dao.update(capture(updated)) } returns Unit

        val outcome = repository.editBackfilled(existing, "b1", 110L, 210L)

        assertEquals(true, outcome is BackfillOutcome.Saved)
        assertEquals(110L, updated.captured.startDate)
        assertEquals(210L, updated.captured.endDate)
    }

    @Test
    fun `editBackfilled is refused when the new dates collide with a different period`() = runTest {
        val existing = period("p1", null, endDate = 200L).copy(startDate = 100L, backfilled = true)
        val other = period("p2", null, endDate = 500L).copy(startDate = 400L)
        coEvery { dao.getByBedId("b1") } returns listOf(existing, other)

        val outcome = repository.editBackfilled(existing, "b1", 450L, 480L)

        assertEquals(BackfillOutcome.Overlaps, outcome)
        coVerify(exactly = 0) { dao.update(any()) }
    }

    @Test
    fun `delete removes the period via the DAO`() = runTest {
        val existing = period("p1", null, endDate = 200L)
        coEvery { dao.delete(existing) } returns Unit

        repository.delete(existing)

        coVerify { dao.delete(existing) }
    }

    @Test
    fun `observeStaysByTenantId passes through the DAO's live joined rows`() = runTest {
        val row = TenantStayRow(
            periodId = "p1", tenancyAgreementId = "a1", bedLabel = "A", roomNumber = "101",
            hostelName = "Sunrise Hostel", propertyType = PropertyType.HOSTEL, startDate = 100L, endDate = null,
        )
        every { dao.observeStaysByTenantId("t1") } returns flowOf(listOf(row))

        val result = repository.observeStaysByTenantId("t1")

        assertEquals(listOf(row), result.first())
    }

    @Test
    fun `getBackfilledStays returns the DAO's joined rows`() = runTest {
        coEvery { dao.getBackfilledStays() } returns emptyList()

        assertEquals(emptyList<Any>(), repository.getBackfilledStays())
    }
}
