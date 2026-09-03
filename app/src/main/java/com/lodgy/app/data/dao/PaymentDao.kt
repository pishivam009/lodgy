package com.lodgy.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lodgy.app.data.entity.Payment
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: Payment)

    @Update
    suspend fun update(payment: Payment)

    @Delete
    suspend fun delete(payment: Payment)

    @Query("SELECT * FROM payments WHERE id = :id")
    suspend fun getById(id: String): Payment?

    @Query("SELECT * FROM payments WHERE invoiceId = :invoiceId")
    fun getByInvoiceId(invoiceId: String): Flow<List<Payment>>

    @Query("SELECT * FROM payments")
    suspend fun getAll(): List<Payment>
}
