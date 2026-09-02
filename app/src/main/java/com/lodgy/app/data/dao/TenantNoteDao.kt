package com.lodgy.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lodgy.app.data.entity.TenantNote
import kotlinx.coroutines.flow.Flow

@Dao
interface TenantNoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: TenantNote)

    @Update
    suspend fun update(note: TenantNote)

    @Delete
    suspend fun delete(note: TenantNote)

    @Query("SELECT * FROM tenant_notes WHERE id = :id")
    suspend fun getById(id: String): TenantNote?

    @Query("SELECT * FROM tenant_notes WHERE tenantId = :tenantId ORDER BY occurredOn DESC")
    fun getByTenantId(tenantId: String): Flow<List<TenantNote>>
}
