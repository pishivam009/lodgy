package com.lodgy.app.data.repository

import com.lodgy.app.data.dao.ExpenseDao
import com.lodgy.app.data.entity.Expense
import com.lodgy.app.data.entity.ExpenseCategory
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ExpenseRepository @Inject constructor(private val expenseDao: ExpenseDao) {
    fun getByHostelId(hostelId: String): Flow<List<Expense>> = expenseDao.getByHostelId(hostelId)

    /** Across every hostel - the notification check is not scoped to the selected one. */
    suspend fun getAll(): List<Expense> = expenseDao.getAll()

    /** Reactive version of [getAll] - backs the Expenses screen's All-properties total (LODGY-109). */
    fun observeAll(): Flow<List<Expense>> = expenseDao.observeAll()

    suspend fun getById(id: String): Expense? = expenseDao.getById(id)

    /** Expenses have nothing hanging off them, so a duplicate or wrong row deletes freely (LODGY-64). */
    suspend fun delete(expense: Expense) = expenseDao.delete(expense)

    suspend fun existsForTenancyInPeriod(tenancyAgreementId: String, periodStart: Long, periodEnd: Long): Boolean =
        expenseDao.existsForTenancyInPeriod(tenancyAgreementId, periodStart, periodEnd)

    suspend fun create(
        hostelId: String,
        category: ExpenseCategory,
        amount: Double,
        isRecurring: Boolean,
        incurredOn: Long,
        note: String?,
        tenancyAgreementId: String? = null,
    ): Expense {
        val now = System.currentTimeMillis()
        val expense = Expense(
            hostelId = hostelId,
            tenancyAgreementId = tenancyAgreementId,
            category = category,
            amount = amount,
            isRecurring = isRecurring,
            incurredOn = incurredOn,
            note = note,
            createdAt = now,
            updatedAt = now,
        )
        expenseDao.insert(expense)
        return expense
    }

    suspend fun update(
        expense: Expense,
        hostelId: String,
        category: ExpenseCategory,
        amount: Double,
        isRecurring: Boolean,
        incurredOn: Long,
        note: String?,
    ) {
        expenseDao.update(
            expense.copy(
                hostelId = hostelId,
                category = category,
                amount = amount,
                isRecurring = isRecurring,
                incurredOn = incurredOn,
                note = note,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }
}
