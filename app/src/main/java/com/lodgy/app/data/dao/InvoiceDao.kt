package com.lodgy.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lodgy.app.data.entity.Invoice
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(invoice: Invoice)

    @Update
    suspend fun update(invoice: Invoice)

    @Delete
    suspend fun delete(invoice: Invoice)

    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getById(id: String): Invoice?

    @Query("SELECT * FROM invoices WHERE tenancyAgreementId = :tenancyAgreementId")
    fun getByTenancyAgreementId(tenancyAgreementId: String): Flow<List<Invoice>>
}
