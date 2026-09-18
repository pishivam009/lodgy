package com.lodgy.app.ui.tenant

import androidx.lifecycle.SavedStateHandle
import com.lodgy.app.data.dao.BedLocation
import com.lodgy.app.data.entity.AgreementStatus
import com.lodgy.app.data.entity.TenancyAgreement
import com.lodgy.app.data.entity.Tenant
import com.lodgy.app.data.entity.TenantStatus
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.TenancyAgreementRepository
import com.lodgy.app.data.repository.TenantRepository
import com.lodgy.app.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TenantProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val tenantRepository: TenantRepository = mockk()
    private val agreementRepository: TenancyAgreementRepository = mockk()
    private val bedRepository: BedRepository = mockk()

    private val tenant = Tenant(id = "t1", name = "Ravi", phone = "1", photoPath = null, idProofPhotoPath = null, emergencyContactName = "", emergencyContactPhone = "", status = TenantStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)

    private fun viewModel() = TenantProfileViewModel(
        tenantRepository,
        agreementRepository,
        bedRepository,
        SavedStateHandle(mapOf("tenantId" to "t1")),
    )

    @Test
    fun `observes and exposes the tenant by id`() {
        every { tenantRepository.observeById("t1") } returns flowOf(tenant)
        every { agreementRepository.observeByTenantId("t1") } returns flowOf(emptyList())

        val viewModel = viewModel()

        assertEquals("t1", viewModel.tenantId)
        assertEquals(tenant, viewModel.tenant.value)
    }

    @Test
    fun `resolves room and bed from the latest agreement, including a closed one`() {
        every { tenantRepository.observeById("t1") } returns flowOf(tenant)
        every { agreementRepository.observeByTenantId("t1") } returns flowOf(listOf(TenancyAgreement(tenantId = "t1", bedId = "b1", agreedRent = 0.0, advanceDeposit = 0.0, billingCycleDay = 1, moveInDate = 0L, moveOutDate = 5L, depositRefundAmount = null, status = AgreementStatus.CLOSED, createdAt = 0L, updatedAt = 0L)))
        coEvery { bedRepository.getLocation("b1") } returns BedLocation("101", "A")

        assertEquals(BedLocation("101", "A"), viewModel().lastClosedTenancy.value?.location)
        assertTrue(viewModel().activeTenancies.value.isEmpty())
    }

    @Test
    fun `no agreement leaves the location empty`() {
        every { tenantRepository.observeById("t1") } returns flowOf(tenant)
        every { agreementRepository.observeByTenantId("t1") } returns flowOf(emptyList())

        assertNull(viewModel().lastClosedTenancy.value)
        assertTrue(viewModel().activeTenancies.value.isEmpty())
    }

    @Test
    fun `an active agreement's planned move-out date is surfaced`() {
        every { tenantRepository.observeById("t1") } returns flowOf(tenant)
        every { agreementRepository.observeByTenantId("t1") } returns flowOf(listOf(activeAgreement(moveOutDate = 5_000L)))
        coEvery { bedRepository.getLocation("b1") } returns BedLocation("101", "A")

        assertEquals(5_000L, viewModel().activeTenancies.value.single().plannedMoveOut)
    }

    @Test
    fun `a closed agreement's move-out date is history, not a pending notice`() {
        every { tenantRepository.observeById("t1") } returns flowOf(tenant)
        every { agreementRepository.observeByTenantId("t1") } returns flowOf(listOf(TenancyAgreement(tenantId = "t1", bedId = "b1", agreedRent = 0.0, advanceDeposit = 0.0, billingCycleDay = 1, moveInDate = 0L, moveOutDate = 5_000L, depositRefundAmount = null, status = AgreementStatus.CLOSED, createdAt = 0L, updatedAt = 0L)))
        coEvery { bedRepository.getLocation("b1") } returns BedLocation("101", "A")

        assertNull(viewModel().lastClosedTenancy.value?.plannedMoveOut)
    }

    @Test
    fun `setting notice records the date without closing the agreement`() {
        val agreement = activeAgreement()
        // Mirrors Room: the write re-emits on the observed query, which is what updates the state.
        val agreements = MutableStateFlow(listOf(agreement))
        every { tenantRepository.observeById("t1") } returns flowOf(tenant)
        every { agreementRepository.observeByTenantId("t1") } returns agreements
        coEvery { agreementRepository.getActiveByBedId("b1") } returns agreement
        coEvery { bedRepository.getLocation("b1") } returns BedLocation("101", "A")
        coEvery { agreementRepository.setPlannedMoveOut(any(), any()) } answers {
            agreements.value = listOf(agreement.copy(moveOutDate = secondArg()))
        }

        val viewModel = viewModel()
        viewModel.setPlannedMoveOut("b1", 9_000L)

        coVerify { agreementRepository.setPlannedMoveOut(agreement, 9_000L) }
        coVerify(exactly = 0) { agreementRepository.close(any(), any(), any()) }
        assertEquals(9_000L, viewModel.activeTenancies.value.single().plannedMoveOut)
    }

    @Test
    fun `withdrawing notice clears the date`() {
        val agreement = activeAgreement(moveOutDate = 9_000L)
        val agreements = MutableStateFlow(listOf(agreement))
        every { tenantRepository.observeById("t1") } returns flowOf(tenant)
        every { agreementRepository.observeByTenantId("t1") } returns agreements
        coEvery { agreementRepository.getActiveByBedId("b1") } returns agreement
        coEvery { bedRepository.getLocation("b1") } returns BedLocation("101", "A")
        coEvery { agreementRepository.setPlannedMoveOut(any(), any()) } answers {
            agreements.value = listOf(agreement.copy(moveOutDate = secondArg()))
        }

        val viewModel = viewModel()
        viewModel.setPlannedMoveOut("b1", null)

        coVerify { agreementRepository.setPlannedMoveOut(agreement, null) }
        assertNull(viewModel.activeTenancies.value.single().plannedMoveOut)
    }

    private fun activeAgreement(moveOutDate: Long? = null, nonRevenue: Boolean = false, bedId: String = "b1") = TenancyAgreement(
        nonRevenue = nonRevenue,
        tenantId = "t1",
        bedId = bedId,
        agreedRent = 0.0,
        advanceDeposit = 0.0,
        billingCycleDay = 1,
        moveInDate = 0L,
        moveOutDate = moveOutDate,
        depositRefundAmount = null,
        status = AgreementStatus.ACTIVE,
        createdAt = 0L,
        updatedAt = 0L,
    )

    /** LODGY-84 gates the forgone-rent action on this: a paying tenancy has no rent to forgo. */
    @Test
    fun `a warden or caretaker room is flagged non-revenue, a paying tenancy is not`() {
        every { tenantRepository.observeById("t1") } returns flowOf(tenant)
        coEvery { bedRepository.getLocation("b1") } returns BedLocation("101", "A")

        every { agreementRepository.observeByTenantId("t1") } returns
            flowOf(listOf(activeAgreement(nonRevenue = true)))
        assertTrue(viewModel().activeTenancies.value.single().nonRevenue)

        every { agreementRepository.observeByTenantId("t1") } returns
            flowOf(listOf(activeAgreement()))
        assertFalse(viewModel().activeTenancies.value.single().nonRevenue)
    }

    @Test
    fun `an active tenancy's stay duration runs from move-in to now`() {
        every { tenantRepository.observeById("t1") } returns flowOf(tenant)
        every { agreementRepository.observeByTenantId("t1") } returns
            flowOf(listOf(activeAgreement().copy(moveInDate = 1_000L)))
        coEvery { bedRepository.getLocation("b1") } returns BedLocation("101", "A")

        val duration = viewModel().activeTenancies.value.single().stayDuration
        assertEquals(1_000L, duration.fromMillis)
        assertTrue(duration.active)
    }

    @Test
    fun `a closed tenancy's stay duration runs from move-in to move-out, not to now`() {
        every { tenantRepository.observeById("t1") } returns flowOf(tenant)
        every { agreementRepository.observeByTenantId("t1") } returns flowOf(
            listOf(
                TenancyAgreement(
                    tenantId = "t1", bedId = "b1", agreedRent = 0.0, advanceDeposit = 0.0,
                    billingCycleDay = 1, moveInDate = 1_000L, moveOutDate = 5_000L,
                    depositRefundAmount = null, status = AgreementStatus.CLOSED,
                    createdAt = 0L, updatedAt = 0L,
                ),
            ),
        )
        coEvery { bedRepository.getLocation("b1") } returns BedLocation("101", "A")

        val duration = viewModel().lastClosedTenancy.value?.stayDuration
        assertEquals(1_000L, duration?.fromMillis)
        assertEquals(5_000L, duration?.toMillis)
        assertFalse(duration?.active == true)
    }

    @Test
    fun `no agreement at all leaves the stay duration empty`() {
        every { tenantRepository.observeById("t1") } returns flowOf(tenant)
        every { agreementRepository.observeByTenantId("t1") } returns flowOf(emptyList())

        assertNull(viewModel().lastClosedTenancy.value)
        assertTrue(viewModel().activeTenancies.value.isEmpty())
    }

    /** The actual point of LODGY-101: a tenant holding two beds gets two independent entries,
     *  not one picked arbitrarily. */
    @Test
    fun `a tenant with two active beds gets two tenancy entries`() {
        every { tenantRepository.observeById("t1") } returns flowOf(tenant)
        every { agreementRepository.observeByTenantId("t1") } returns flowOf(
            listOf(
                activeAgreement(bedId = "b1").copy(moveInDate = 1_000L),
                activeAgreement(bedId = "b2").copy(moveInDate = 2_000L),
            ),
        )
        coEvery { bedRepository.getLocation("b1") } returns BedLocation("101", "A")
        coEvery { bedRepository.getLocation("b2") } returns BedLocation("102", "A")

        val entries = viewModel().activeTenancies.value
        assertEquals(2, entries.size)
        assertEquals(setOf("b1", "b2"), entries.map { it.bedId }.toSet())
    }
}
