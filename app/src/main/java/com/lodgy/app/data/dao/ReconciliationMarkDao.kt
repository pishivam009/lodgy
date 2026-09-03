package com.lodgy.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lodgy.app.data.entity.ReconciliationMark
import kotlinx.coroutines.flow.Flow

@Dao
interface ReconciliationMarkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mark: ReconciliationMark)

    @Delete
    suspend fun delete(mark: ReconciliationMark)

    @Query(
        "SELECT * FROM reconciliation_marks " +
            "WHERE hostelId = :hostelId AND periodMonth = :periodMonth AND periodYear = :periodYear",
    )
    suspend fun getForPeriod(hostelId: String, periodMonth: Int, periodYear: Int): ReconciliationMark?

    @Query("SELECT * FROM reconciliation_marks WHERE hostelId = :hostelId")
    fun getByHostelId(hostelId: String): Flow<List<ReconciliationMark>>
}
