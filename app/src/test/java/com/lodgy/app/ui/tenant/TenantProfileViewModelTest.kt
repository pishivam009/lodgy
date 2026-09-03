package com.lodgy.app.ui.tenant

import androidx.lifecycle.SavedStateHandle
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

class TenantProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val tenantRepository: TenantRepository = mockk()

    @Test
    fun `observes and exposes the tenant by id`() {
        val tenant = Tenant(id = "t1", name = "Ravi", phone = "1", photoPath = null, idProofPhotoPath = null, emergencyContactName = "", emergencyContactPhone = "", status = TenantStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)
        every { tenantRepository.observeById("t1") } returns flowOf(tenant)

        val viewModel = TenantProfileViewModel(tenantRepository, SavedStateHandle(mapOf("tenantId" to "t1")))

        assertEquals("t1", viewModel.tenantId)
        assertEquals(tenant, viewModel.tenant.value)
    }
}
