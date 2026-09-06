package com.lodgy.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lodgy.app.data.entity.Expense
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: Expense)

    @Update
    suspend fun update(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: String): Expense?

    @Query("SELECT * FROM expenses WHERE hostelId = :hostelId ORDER BY incurredOn DESC")
    fun getByHostelId(hostelId: String): Flow<List<Expense>>

    @Query("SELECT * FROM expenses")
    suspend fun getAll(): List<Expense>

    /** The idempotency check behind the monthly forgone-rent entry (LODGY-84): the period is
     *  read off incurredOn rather than stored twice, so a re-run on the same day cannot
     *  double-count. Times are local, which is what the warden's month means. */
    @Query(
        "SELECT COUNT(*) > 0 FROM expenses WHERE tenancyAgreementId = :tenancyAgreementId " +
            "AND incurredOn >= :periodStart AND incurredOn < :periodEnd",
    )
    suspend fun existsForTenancyInPeriod(
        tenancyAgreementId: String,
        periodStart: Long,
        periodEnd: Long,
    ): Boolean
}
