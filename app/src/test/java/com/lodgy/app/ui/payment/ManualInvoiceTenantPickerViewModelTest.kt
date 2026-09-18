package com.lodgy.app.ui.payment

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
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ManualInvoiceTenantPickerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val tenancyAgreementRepository: TenancyAgreementRepository = mockk()
    private val tenantRepository: TenantRepository = mockk()
    private val bedRepository: BedRepository = mockk()

    private fun tenant(id: String, name: String) =
        Tenant(id = id, name = name, phone = "1", photoPath = null, idProofPhotoPath = null, emergencyContactName = "", emergencyContactPhone = "", status = TenantStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)

    private fun agreement(tenantId: String, bedId: String) = TenancyAgreement(
        tenantId = tenantId, bedId = bedId, agreedRent = 0.0, advanceDeposit = 0.0,
        billingCycleDay = 1, moveInDate = 0L, moveOutDate = null, depositRefundAmount = null,
        status = AgreementStatus.ACTIVE, createdAt = 0L, updatedAt = 0L,
    )

    @Test
    fun `lists one row per active tenancy, not per tenant`() {
        coEvery { tenancyAgreementRepository.getAllActive() } returns listOf(
            agreement("t1", "b1"),
            agreement("t1", "b2"),
        )
        coEvery { tenantRepository.getById("t1") } returns tenant("t1", "Ravi")
        coEvery { bedRepository.getLocation("b1") } returns BedLocation("101", "A")
        coEvery { bedRepository.getLocation("b2") } returns BedLocation("102", "A")

        val rows = ManualInvoiceTenantPickerViewModel(tenancyAgreementRepository, tenantRepository, bedRepository).rows.value

        assertEquals(2, rows.size)
        assertEquals(setOf("b1", "b2"), rows.map { it.bedId }.toSet())
        assertEquals(listOf("Ravi", "Ravi"), rows.map { it.tenantName })
    }
}
