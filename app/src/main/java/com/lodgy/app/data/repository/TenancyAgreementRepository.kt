package com.lodgy.app.data.repository

import com.lodgy.app.data.dao.TenancyAgreementDao
import com.lodgy.app.data.entity.AgreementStatus
import com.lodgy.app.data.entity.TenancyAgreement
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class TenancyAgreementRepository @Inject constructor(private val dao: TenancyAgreementDao) {
    suspend fun getActiveByTenantId(tenantId: String): TenancyAgreement? =
        dao.getByTenantId(tenantId).first().firstOrNull { it.status == AgreementStatus.ACTIVE }

    fun observeByTenantId(tenantId: String): Flow<List<TenancyAgreement>> = dao.getByTenantId(tenantId)

    /** Screens that show a tenant's room/bed need to react to a transfer, which touches only
     *  this table - watching tenants alone leaves the label stale. */
    fun observeAll(): Flow<List<TenancyAgreement>> = dao.observeAll()

    suspend fun getAllActive(): List<TenancyAgreement> = dao.getAllActive()

    suspend fun getActiveByBedId(bedId: String): TenancyAgreement? = dao.getActiveByBedId(bedId)

    suspend fun getLatestByTenantId(tenantId: String): TenancyAgreement? = dao.getLatestByTenantId(tenantId)

    /** Includes closed agreements - historical reporting needs a checked-out tenant's past
     *  invoices/payments to still count. [getAllActive] would silently drop them. */
    suspend fun getAll(): List<TenancyAgreement> = dao.getAll()

    suspend fun getById(id: String): TenancyAgreement? = dao.getById(id)

    /** Records notice without ending the tenancy: the agreement stays ACTIVE and the bed stays
     *  OCCUPIED. Checkout remains a separate, explicit action on or after the date. Passing null
     *  withdraws the notice. */
    suspend fun setPlannedMoveOut(agreement: TenancyAgreement, moveOutDate: Long?) {
        dao.update(agreement.copy(moveOutDate = moveOutDate, updatedAt = System.currentTimeMillis()))
    }

    /** Moves the tenancy to another bed on the SAME agreement row - no close, no new agreement -
     *  so invoices and payments keyed to this agreement stay one continuous tenancy. */
    suspend fun transferBed(agreement: TenancyAgreement, newBedId: String, agreedRent: Double) {
        dao.update(
            agreement.copy(bedId = newBedId, agreedRent = agreedRent, updatedAt = System.currentTimeMillis()),
        )
    }

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
        nonRevenue: Boolean = false,
    ): TenancyAgreement {
        val now = System.currentTimeMillis()
        val agreement = TenancyAgreement(
            tenantId = tenantId,
            bedId = bedId,
            agreedRent = agreedRent,
            advanceDeposit = advanceDeposit,
            billingCycleDay = billingCycleDay,
            moveInDate = moveInDate,
            nonRevenue = nonRevenue,
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
