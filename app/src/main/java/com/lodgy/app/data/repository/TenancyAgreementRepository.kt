package com.lodgy.app.data.repository

import com.lodgy.app.data.dao.TenancyAgreementDao
import com.lodgy.app.data.entity.AgreementStatus
import com.lodgy.app.data.entity.TenancyAgreement
import javax.inject.Inject

class TenancyAgreementRepository @Inject constructor(private val dao: TenancyAgreementDao) {
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
