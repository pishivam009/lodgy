package com.lodgy.app.ui.payment

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

class ManualInvoiceTenantPickerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val tenantRepository: TenantRepository = mockk()

    private fun tenant(name: String, status: TenantStatus) =
        Tenant(name = name, phone = "1", photoPath = null, idProofPhotoPath = null, emergencyContactName = "", emergencyContactPhone = "", status = status, createdAt = 0L, updatedAt = 0L)

    @Test
    fun `only active tenants are offered`() {
        every { tenantRepository.getAll() } returns flowOf(
            listOf(tenant("Ravi", TenantStatus.ACTIVE), tenant("Old Tenant", TenantStatus.VACATED)),
        )

        val state = ManualInvoiceTenantPickerViewModel(tenantRepository).activeTenants.value

        assertEquals(listOf("Ravi"), state.map { it.name })
    }
}
