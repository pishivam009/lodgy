package com.lodgy.app.data.repository

import com.lodgy.app.data.dao.TenancyAgreementDao
import com.lodgy.app.data.entity.AgreementStatus
import com.lodgy.app.data.entity.TenancyAgreement
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class TenancyAgreementRepository @Inject constructor(private val dao: TenancyAgreementDao) {
    suspend fun getActiveByTenantId(tenantId: String): TenancyAgreement? =
        dao.getByTenantId(tenantId).first().firstOrNull { it.status == AgreementStatus.ACTIVE }

    suspend fun getAllActive(): List<TenancyAgreement> = dao.getAllActive()

    suspend fun getById(id: String): TenancyAgreement? = dao.getById(id)

    suspend fun close(agreement: TenancyAgreement, moveOutDate: Long, depositRefundAmount: Double) {
        dao.update(
            agreement.copy(
                moveOutDate = moveOutDate,
                depositRefundAmount = depositRefundAmount,
                status = AgreementStatus.CLOSED,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun create(
        tenantId: String,
        bedId: String,
        agreedRent: Double,
        advanceDeposit: Double,
        billingCycleDay: Int,
        moveInDate: Long,
    ): TenancyAgreement {
        val now = System.currentTimeMillis()
        val agreement = TenancyAgreement(
            tenantId = tenantId,
            bedId = bedId,
            agreedRent = agreedRent,
            advanceDeposit = advanceDeposit,
            billingCycleDay = billingCycleDay,
            moveInDate = moveInDate,
            moveOutDate = null,
            depositRefundAmount = null,
            status = AgreementStatus.ACTIVE,
            createdAt = now,
            updatedAt = now,
        )
        dao.insert(agreement)
        return agreement
    }
}
