package com.lodgy.app.data.repository

import com.lodgy.app.data.dao.TenancyAgreementDao
import com.lodgy.app.data.entity.AgreementStatus
import com.lodgy.app.data.entity.TenancyAgreement
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.coVerify
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TenancyAgreementRepositoryTest {

    private val dao: TenancyAgreementDao = mockk()
    private val repository = TenancyAgreementRepository(dao)

    private fun agreement(id: String, status: AgreementStatus) = TenancyAgreement(
        id = id, tenantId = "t1", bedId = "b1", agreedRent = 1000.0, advanceDeposit = 2000.0,
        billingCycleDay = 5, moveInDate = 0L, moveOutDate = null, depositRefundAmount = null,
        status = status, createdAt = 0L, updatedAt = 0L,
    )

    @Test
    fun `getActiveByTenantId picks the active agreement among the tenant's history`() = runTest {
        every { dao.getByTenantId("t1") } returns flowOf(
            listOf(agreement("a1", AgreementStatus.CLOSED), agreement("a2", AgreementStatus.ACTIVE)),
        )
        assertEquals("a2", repository.getActiveByTenantId("t1")?.id)
    }

    @Test
    fun `getActiveByTenantId is null when the tenant has no active agreement`() = runTest {
        every { dao.getByTenantId("t1") } returns flowOf(listOf(agreement("a1", AgreementStatus.CLOSED)))
        assertNull(repository.getActiveByTenantId("t1"))
    }

    @Test
    fun `create persists a new ACTIVE agreement with no move-out yet`() = runTest {
        val inserted = slot<TenancyAgreement>()
        coEvery { dao.insert(capture(inserted)) } returns Unit

        val created = repository.create("t1", "b1", 1500.0, 3000.0, 5, 100L)

        assertEquals(AgreementStatus.ACTIVE, created.status)
        assertNull(created.moveOutDate)
        assertNull(created.depositRefundAmount)
        assertEquals(created, inserted.captured)
    }

    @Test
    fun `close sets status CLOSED with the move-out date and refund amount`() = runTest {
        val existing = agreement("a1", AgreementStatus.ACTIVE)
        val updated = slot<TenancyAgreement>()
        coEvery { dao.update(capture(updated)) } returns Unit

        repository.close(existing, moveOutDate = 999L, depositRefundAmount = 1800.0)

        assertEquals(AgreementStatus.CLOSED, updated.captured.status)
        assertEquals(999L, updated.captured.moveOutDate)
        assertEquals(1800.0, updated.captured.depositRefundAmount!!, 0.0001)
    }

    @Test
    fun `transferBed moves the tenancy without closing it or starting a new one`() = runTest {
        val active = agreement("a1", AgreementStatus.ACTIVE)
        val updated = slot<TenancyAgreement>()
        coEvery { dao.update(capture(updated)) } returns Unit

        repository.transferBed(active, "new-bed", 5500.0)

        assertEquals(active.id, updated.captured.id)
        assertEquals("new-bed", updated.captured.bedId)
        assertEquals(5500.0, updated.captured.agreedRent, 0.0001)
        assertEquals(AgreementStatus.ACTIVE, updated.captured.status)
        assertNull(updated.captured.moveOutDate)
        coVerify(exactly = 0) { dao.insert(any()) }
    }

    @Test
    fun `setPlannedMoveOut records notice while leaving the tenancy active`() = runTest {
        val active = agreement("a1", AgreementStatus.ACTIVE)
        val updated = slot<TenancyAgreement>()
        coEvery { dao.update(capture(updated)) } returns Unit

        repository.setPlannedMoveOut(active, 9_000L)

        assertEquals(9_000L, updated.captured.moveOutDate)
        assertEquals(AgreementStatus.ACTIVE, updated.captured.status)
        assertNull(updated.captured.depositRefundAmount)
    }

    @Test
    fun `setPlannedMoveOut with null withdraws the notice`() = runTest {
        val withNotice = agreement("a1", AgreementStatus.ACTIVE).copy(moveOutDate = 9_000L)
        val updated = slot<TenancyAgreement>()
        coEvery { dao.update(capture(updated)) } returns Unit

        repository.setPlannedMoveOut(withNotice, null)

        assertNull(updated.captured.moveOutDate)
    }
}
