package com.lodgy.app.data.repository

import com.lodgy.app.data.dao.ExpenseDao
import com.lodgy.app.data.entity.Expense
import com.lodgy.app.data.entity.ExpenseCategory
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ExpenseRepository @Inject constructor(private val expenseDao: ExpenseDao) {
    fun getByHostelId(hostelId: String): Flow<List<Expense>> = expenseDao.getByHostelId(hostelId)

    suspend fun getById(id: String): Expense? = expenseDao.getById(id)

    suspend fun create(
        hostelId: String,
        category: ExpenseCategory,
        amount: Double,
        isRecurring: Boolean,
        incurredOn: Long,
        note: String?,
    ): Expense {
        val now = System.currentTimeMillis()
        val expense = Expense(
            hostelId = hostelId,
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
        category: ExpenseCategory,
        amount: Double,
        isRecurring: Boolean,
        incurredOn: Long,
        note: String?,
    ) {
        expenseDao.update(
            expense.copy(
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
