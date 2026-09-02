package com.lodgy.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lodgy.app.data.entity.Warden
import kotlinx.coroutines.flow.Flow

@Dao
interface WardenDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(warden: Warden)

    @Update
    suspend fun update(warden: Warden)

    @Delete
    suspend fun delete(warden: Warden)

    @Query("SELECT * FROM wardens WHERE id = :id")
    suspend fun getById(id: String): Warden?

    @Query("SELECT * FROM wardens")
    fun getAll(): Flow<List<Warden>>
}
