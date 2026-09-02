package com.lodgy.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lodgy.app.data.entity.Hostel
import kotlinx.coroutines.flow.Flow

@Dao
interface HostelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(hostel: Hostel)

    @Update
    suspend fun update(hostel: Hostel)

    @Delete
    suspend fun delete(hostel: Hostel)

    @Query("SELECT * FROM hostels WHERE id = :id")
    suspend fun getById(id: String): Hostel?

    @Query("SELECT * FROM hostels WHERE wardenId = :wardenId")
    fun getByWardenId(wardenId: String): Flow<List<Hostel>>
}
