package com.lodgy.app.ui.tenant

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
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class TenantDirectoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val tenantRepository: TenantRepository = mockk()
    private val agreementRepository: TenancyAgreementRepository = mockk()
    private val bedRepository: BedRepository = mockk()

    private fun tenant(name: String, phone: String, id: String = name, status: TenantStatus = TenantStatus.ACTIVE) =
        Tenant(id = id, name = name, phone = phone, photoPath = null, idProofPhotoPath = null, emergencyContactName = "", emergencyContactPhone = "", status = status, createdAt = 0L, updatedAt = 0L)

    private fun agreement(tenantId: String, bedId: String) =
        TenancyAgreement(tenantId = tenantId, bedId = bedId, agreedRent = 0.0, advanceDeposit = 0.0, billingCycleDay = 1, moveInDate = 0L, moveOutDate = null, depositRefundAmount = null, status = AgreementStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)

    private fun viewModel() = TenantDirectoryViewModel(tenantRepository, agreementRepository, bedRepository)

    @Test
    fun `blank query returns every active tenant`() {
        every { tenantRepository.getAll() } returns flowOf(listOf(tenant("Ravi", "999"), tenant("Sita", "888")))
        coEvery { agreementRepository.getLatestByTenantId(any()) } returns null

        assertEquals(2, viewModel().uiState.value.items.size)
    }

    @Test
    fun `query filters by name case-insensitively or by phone`() {
        every { tenantRepository.getAll() } returns flowOf(listOf(tenant("Ravi Kumar", "9990001111"), tenant("Sita Devi", "8880002222")))
        coEvery { agreementRepository.getLatestByTenantId(any()) } returns null

        val viewModel = viewModel()
        viewModel.onQueryChange("ravi")

        assertEquals(listOf("Ravi Kumar"), viewModel.uiState.value.items.map { it.tenant.name })

        viewModel.onQueryChange("8880002222")
        assertEquals(listOf("Sita Devi"), viewModel.uiState.value.items.map { it.tenant.name })
    }

    @Test
    fun `each row carries the room and bed from the tenant's latest agreement`() {
        every { tenantRepository.getAll() } returns flowOf(listOf(tenant("Ravi", "999", id = "t1")))
        coEvery { agreementRepository.getLatestByTenantId("t1") } returns agreement("t1", "b1")
        coEvery { bedRepository.getLocation("b1") } returns BedLocation("204", "B")

        assertEquals(BedLocation("204", "B"), viewModel().uiState.value.items.single().location)
    }

    @Test
    fun `a tenant who never had an agreement has no location rather than failing`() {
        every { tenantRepository.getAll() } returns flowOf(listOf(tenant("Ravi", "999", id = "t1")))
        coEvery { agreementRepository.getLatestByTenantId("t1") } returns null

        assertNull(viewModel().uiState.value.items.single().location)
    }

    @Test
    fun `defaults to active-only and reports how many vacated tenants that hides`() {
        every { tenantRepository.getAll() } returns flowOf(
            listOf(tenant("Ravi", "999", id = "t1"), tenant("Old", "111", id = "t2", status = TenantStatus.VACATED)),
        )
        coEvery { agreementRepository.getLatestByTenantId(any()) } returns null

        val state = viewModel().uiState.value

        assertEquals(TenantFilter.ACTIVE, state.filter)
        assertEquals(listOf("Ravi"), state.items.map { it.tenant.name })
        assertEquals(1, state.hiddenByFilter)
    }

    @Test
    fun `switching to ALL brings vacated tenants back and clears the hidden count`() {
        every { tenantRepository.getAll() } returns flowOf(
            listOf(tenant("Ravi", "999", id = "t1"), tenant("Old", "111", id = "t2", status = TenantStatus.VACATED)),
        )
        coEvery { agreementRepository.getLatestByTenantId(any()) } returns null

        val viewModel = viewModel()
        viewModel.onFilterChange(TenantFilter.ALL)

        assertEquals(2, viewModel.uiState.value.items.size)
        assertEquals(0, viewModel.uiState.value.hiddenByFilter)
    }

    @Test
    fun `name sort is case-insensitive`() {
        every { tenantRepository.getAll() } returns flowOf(
            listOf(tenant("sita", "1", id = "t1"), tenant("Ravi", "2", id = "t2")),
        )
        coEvery { agreementRepository.getLatestByTenantId(any()) } returns null

        assertEquals(listOf("Ravi", "sita"), viewModel().uiState.value.items.map { it.tenant.name })
    }

    @Test
    fun `room sort orders numerically, then text labels, then tenants with no room`() {
        every { tenantRepository.getAll() } returns flowOf(
            listOf(
                tenant("Thousand", "1", id = "t1"),
                tenant("TwoOhFour", "2", id = "t2"),
                tenant("Ground", "3", id = "t3"),
                tenant("Unassigned", "4", id = "t4"),
            ),
        )
        coEvery { agreementRepository.getLatestByTenantId("t1") } returns agreement("t1", "b1")
        coEvery { agreementRepository.getLatestByTenantId("t2") } returns agreement("t2", "b2")
        coEvery { agreementRepository.getLatestByTenantId("t3") } returns agreement("t3", "b3")
        coEvery { agreementRepository.getLatestByTenantId("t4") } returns null
        coEvery { bedRepository.getLocation("b1") } returns BedLocation("1005", "A")
        coEvery { bedRepository.getLocation("b2") } returns BedLocation("204", "A")
        coEvery { bedRepository.getLocation("b3") } returns BedLocation("G-2", "A")

        val viewModel = viewModel()
        viewModel.onSortChange(TenantSort.ROOM)

        assertEquals(
            listOf("TwoOhFour", "Thousand", "Ground", "Unassigned"),
            viewModel.uiState.value.items.map { it.tenant.name },
        )
    }
}
