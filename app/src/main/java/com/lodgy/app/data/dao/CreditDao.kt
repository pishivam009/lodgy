package com.lodgy.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lodgy.app.data.entity.Credit
import kotlinx.coroutines.flow.Flow

@Dao
interface CreditDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(credit: Credit)

    @Update
    suspend fun update(credit: Credit)

    @Delete
    suspend fun delete(credit: Credit)

    @Query("SELECT * FROM credits WHERE id = :id")
    suspend fun getById(id: String): Credit?

    @Query("SELECT * FROM credits WHERE tenantId = :tenantId ORDER BY createdAt DESC")
    fun getByTenantId(tenantId: String): Flow<List<Credit>>

    @Query("SELECT * FROM credits WHERE tenantId = :tenantId AND invoiceId IS NULL")
    suspend fun getPendingByTenantId(tenantId: String): List<Credit>

    @Query("SELECT * FROM credits WHERE invoiceId = :invoiceId")
    suspend fun getByInvoiceId(invoiceId: String): List<Credit>

    @Query("SELECT * FROM credits")
    fun getAll(): Flow<List<Credit>>

    @Query("SELECT * FROM credits")
    suspend fun getAllOnce(): List<Credit>
}
