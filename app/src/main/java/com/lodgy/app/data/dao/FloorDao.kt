package com.lodgy.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lodgy.app.data.entity.Floor
import kotlinx.coroutines.flow.Flow

@Dao
interface FloorDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(floor: Floor)

    @Update
    suspend fun update(floor: Floor)

    @Delete
    suspend fun delete(floor: Floor)

    @Query("SELECT * FROM floors WHERE id = :id")
    suspend fun getById(id: String): Floor?

    @Query("SELECT * FROM floors WHERE hostelId = :hostelId ORDER BY sortOrder")
    fun getByHostelId(hostelId: String): Flow<List<Floor>>
}
