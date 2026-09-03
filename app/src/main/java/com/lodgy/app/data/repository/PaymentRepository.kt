package com.lodgy.app.data.repository

import com.lodgy.app.data.dao.PaymentDao
import com.lodgy.app.data.entity.Payment
import com.lodgy.app.data.entity.PaymentMode
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class PaymentRepository @Inject constructor(private val paymentDao: PaymentDao) {
    fun getByInvoiceId(invoiceId: String): Flow<List<Payment>> = paymentDao.getByInvoiceId(invoiceId)

    suspend fun getAll(): List<Payment> = paymentDao.getAll()

    suspend fun getTotalPaid(invoiceId: String): Double =
        paymentDao.getByInvoiceId(invoiceId).first().sumOf { it.amount }

    fun getMultiPeriod(): Flow<List<Payment>> = paymentDao.getMultiPeriod()

    suspend fun create(
        invoiceId: String,
        amount: Double,
        mode: PaymentMode,
        paidOn: Long,
        note: String?,
        multiPeriodGroupId: String? = null,
    ): Payment {
        val now = System.currentTimeMillis()
        val payment = Payment(
            invoiceId = invoiceId,
            amount = amount,
            paymentMode = mode,
            paidOn = paidOn,
            note = note,
            multiPeriodGroupId = multiPeriodGroupId,
            createdAt = now,
            updatedAt = now,
        )
        paymentDao.insert(payment)
        return payment
    }
}
