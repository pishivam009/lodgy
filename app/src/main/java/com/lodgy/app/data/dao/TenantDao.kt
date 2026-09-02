package com.lodgy.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lodgy.app.data.entity.Tenant
import kotlinx.coroutines.flow.Flow

@Dao
interface TenantDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tenant: Tenant)

    @Update
    suspend fun update(tenant: Tenant)

    @Delete
    suspend fun delete(tenant: Tenant)

    @Query("SELECT * FROM tenants WHERE id = :id")
    suspend fun getById(id: String): Tenant?

    @Query("SELECT * FROM tenants WHERE id = :id")
    fun getByIdFlow(id: String): Flow<Tenant?>

    @Query("SELECT * FROM tenants")
    fun getAll(): Flow<List<Tenant>>
}
