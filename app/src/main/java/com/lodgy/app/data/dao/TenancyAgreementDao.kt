package com.lodgy.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lodgy.app.data.entity.TenancyAgreement
import kotlinx.coroutines.flow.Flow

@Dao
interface TenancyAgreementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(agreement: TenancyAgreement)

    @Update
    suspend fun update(agreement: TenancyAgreement)

    @Delete
    suspend fun delete(agreement: TenancyAgreement)

    @Query("SELECT * FROM tenancy_agreements WHERE id = :id")
    suspend fun getById(id: String): TenancyAgreement?

    @Query("SELECT * FROM tenancy_agreements WHERE tenantId = :tenantId")
    fun getByTenantId(tenantId: String): Flow<List<TenancyAgreement>>

    @Query("SELECT * FROM tenancy_agreements WHERE status = 'ACTIVE'")
    suspend fun getAllActive(): List<TenancyAgreement>

    /** Active agreement if there is one, else the most recent closed one, so a vacated
     *  tenant still resolves to the room/bed they last occupied. */
    @Query(
        "SELECT * FROM tenancy_agreements WHERE tenantId = :tenantId " +
            "ORDER BY status = 'ACTIVE' DESC, moveInDate DESC LIMIT 1",
    )
    suspend fun getLatestByTenantId(tenantId: String): TenancyAgreement?

    @Query("SELECT * FROM tenancy_agreements")
    suspend fun getAll(): List<TenancyAgreement>
}
