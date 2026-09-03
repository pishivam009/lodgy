package com.lodgy.app.data.repository

import com.lodgy.app.data.dao.CreditDao
import com.lodgy.app.data.entity.Credit
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class CreditRepository @Inject constructor(private val creditDao: CreditDao) {
    fun getAll(): Flow<List<Credit>> = creditDao.getAll()

    suspend fun getAllOnce(): List<Credit> = creditDao.getAllOnce()

    fun getByTenantId(tenantId: String): Flow<List<Credit>> = creditDao.getByTenantId(tenantId)

    suspend fun getByInvoiceId(invoiceId: String): List<Credit> = creditDao.getByInvoiceId(invoiceId)

    suspend fun create(tenantId: String, invoiceId: String?, amount: Double, reason: String): Credit {
        val now = System.currentTimeMillis()
        val credit = Credit(
            tenantId = tenantId,
            invoiceId = invoiceId,
            amount = amount,
            reason = reason,
            createdAt = now,
            updatedAt = now,
        )
        creditDao.insert(credit)
        return credit
    }

    /** Attaches everything the tenant is still owed to a freshly generated invoice. */
    suspend fun applyPendingTo(tenantId: String, invoiceId: String) {
        val now = System.currentTimeMillis()
        creditDao.getPendingByTenantId(tenantId).forEach { credit ->
            creditDao.update(credit.copy(invoiceId = invoiceId, updatedAt = now))
        }
    }

    suspend fun delete(credit: Credit) = creditDao.delete(credit)
}
