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
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        coEvery { agreementRepository.getLatestByTenantId("t1") } returns null

        val viewModel = viewModel()

        assertEquals("t1", viewModel.tenantId)
        assertEquals(tenant, viewModel.tenant.value)
    }

    @Test
    fun `resolves room and bed from the latest agreement, including a closed one`() {
        every { tenantRepository.observeById("t1") } returns flowOf(tenant)
        coEvery { agreementRepository.getLatestByTenantId("t1") } returns TenancyAgreement(tenantId = "t1", bedId = "b1", agreedRent = 0.0, advanceDeposit = 0.0, billingCycleDay = 1, moveInDate = 0L, moveOutDate = 5L, depositRefundAmount = null, status = AgreementStatus.CLOSED, createdAt = 0L, updatedAt = 0L)
        coEvery { bedRepository.getLocation("b1") } returns BedLocation("101", "A")

        assertEquals(BedLocation("101", "A"), viewModel().location.value)
    }

    @Test
    fun `no agreement leaves the location empty`() {
        every { tenantRepository.observeById("t1") } returns flowOf(tenant)
        coEvery { agreementRepository.getLatestByTenantId("t1") } returns null

        assertNull(viewModel().location.value)
    }

    @Test
    fun `an active agreement's planned move-out date is surfaced`() {
        every { tenantRepository.observeById("t1") } returns flowOf(tenant)
        coEvery { agreementRepository.getLatestByTenantId("t1") } returns activeAgreement(moveOutDate = 5_000L)
        coEvery { bedRepository.getLocation("b1") } returns BedLocation("101", "A")

        assertEquals(5_000L, viewModel().plannedMoveOut.value)
    }

    @Test
    fun `a closed agreement's move-out date is history, not a pending notice`() {
        every { tenantRepository.observeById("t1") } returns flowOf(tenant)
        coEvery { agreementRepository.getLatestByTenantId("t1") } returns TenancyAgreement(tenantId = "t1", bedId = "b1", agreedRent = 0.0, advanceDeposit = 0.0, billingCycleDay = 1, moveInDate = 0L, moveOutDate = 5_000L, depositRefundAmount = null, status = AgreementStatus.CLOSED, createdAt = 0L, updatedAt = 0L)
        coEvery { bedRepository.getLocation("b1") } returns BedLocation("101", "A")

        assertNull(viewModel().plannedMoveOut.value)
    }

    @Test
    fun `setting notice records the date without closing the agreement`() {
        val agreement = activeAgreement()
        every { tenantRepository.observeById("t1") } returns flowOf(tenant)
        coEvery { agreementRepository.getLatestByTenantId("t1") } returns agreement
        coEvery { agreementRepository.getActiveByTenantId("t1") } returns agreement
        coEvery { bedRepository.getLocation("b1") } returns BedLocation("101", "A")
        coEvery { agreementRepository.setPlannedMoveOut(any(), any()) } returns Unit

        val viewModel = viewModel()
        viewModel.setPlannedMoveOut(9_000L)

        coVerify { agreementRepository.setPlannedMoveOut(agreement, 9_000L) }
        coVerify(exactly = 0) { agreementRepository.close(any(), any(), any()) }
        assertEquals(9_000L, viewModel.plannedMoveOut.value)
    }

    @Test
    fun `withdrawing notice clears the date`() {
        val agreement = activeAgreement(moveOutDate = 9_000L)
        every { tenantRepository.observeById("t1") } returns flowOf(tenant)
        coEvery { agreementRepository.getLatestByTenantId("t1") } returns agreement
        coEvery { agreementRepository.getActiveByTenantId("t1") } returns agreement
        coEvery { bedRepository.getLocation("b1") } returns BedLocation("101", "A")
        coEvery { agreementRepository.setPlannedMoveOut(any(), any()) } returns Unit

        val viewModel = viewModel()
        viewModel.setPlannedMoveOut(null)

        coVerify { agreementRepository.setPlannedMoveOut(agreement, null) }
        assertNull(viewModel.plannedMoveOut.value)
    }

    private fun activeAgreement(moveOutDate: Long? = null) = TenancyAgreement(
        tenantId = "t1",
        bedId = "b1",
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
}
