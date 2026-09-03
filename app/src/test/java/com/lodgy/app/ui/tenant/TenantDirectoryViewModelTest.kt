package com.lodgy.app.ui.tenant

import com.lodgy.app.data.entity.Tenant
import com.lodgy.app.data.entity.TenantStatus
import com.lodgy.app.data.repository.TenantRepository
import com.lodgy.app.testutil.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TenantDirectoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val tenantRepository: TenantRepository = mockk()

    private fun tenant(name: String, phone: String) =
        Tenant(name = name, phone = phone, photoPath = null, idProofPhotoPath = null, emergencyContactName = "", emergencyContactPhone = "", status = TenantStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)

    @Test
    fun `blank query returns every tenant`() {
        every { tenantRepository.getAll() } returns flowOf(listOf(tenant("Ravi", "999"), tenant("Sita", "888")))

        val state = TenantDirectoryViewModel(tenantRepository).uiState.value

        assertEquals(2, state.tenants.size)
    }

    @Test
    fun `query filters by name case-insensitively or by phone`() {
        every { tenantRepository.getAll() } returns flowOf(listOf(tenant("Ravi Kumar", "9990001111"), tenant("Sita Devi", "8880002222")))

        val viewModel = TenantDirectoryViewModel(tenantRepository)
        viewModel.onQueryChange("ravi")

        assertEquals(listOf("Ravi Kumar"), viewModel.uiState.value.tenants.map { it.name })

        viewModel.onQueryChange("8880002222")
        assertEquals(listOf("Sita Devi"), viewModel.uiState.value.tenants.map { it.name })
    }
}
