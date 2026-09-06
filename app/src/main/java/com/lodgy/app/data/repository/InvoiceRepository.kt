package com.lodgy.app.data.repository

import com.lodgy.app.data.dao.InvoiceDao
import com.lodgy.app.data.entity.Invoice
import com.lodgy.app.data.entity.InvoiceStatus
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class InvoiceRepository @Inject constructor(private val invoiceDao: InvoiceDao) {
    fun getAll(): Flow<List<Invoice>> = invoiceDao.getAll()

    fun getByTenancyAgreementId(id: String): Flow<List<Invoice>> = invoiceDao.getByTenancyAgreementId(id)

    suspend fun getById(id: String): Invoice? = invoiceDao.getById(id)

    /** Deleted only once the caller has confirmed no payment or credit still points at it, so it
     *  never orphans money (LODGY-64, AC4). */
    suspend fun delete(invoice: Invoice) = invoiceDao.delete(invoice)

    suspend fun existsForPeriod(tenancyAgreementId: String, periodMonth: Int, periodYear: Int): Boolean =
        invoiceDao.getForPeriod(tenancyAgreementId, periodMonth, periodYear) != null

    suspend fun updateStatus(invoice: Invoice, status: InvoiceStatus) {
        invoiceDao.update(invoice.copy(status = status, updatedAt = System.currentTimeMillis()))
    }

    suspend fun create(
        tenancyAgreementId: String,
        periodMonth: Int,
        periodYear: Int,
        amountDue: Double,
        dueDate: Long,
    ): Invoice {
        val now = System.currentTimeMillis()
        val invoice = Invoice(
            tenancyAgreementId = tenancyAgreementId,
            periodMonth = periodMonth,
            periodYear = periodYear,
            amountDue = amountDue,
            dueDate = dueDate,
            status = InvoiceStatus.UNPAID,
            createdAt = now,
            updatedAt = now,
        )
        invoiceDao.insert(invoice)
        return invoice
    }
}
