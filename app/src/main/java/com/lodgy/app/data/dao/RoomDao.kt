package com.lodgy.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lodgy.app.data.entity.Room
import kotlinx.coroutines.flow.Flow

@Dao
interface RoomDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(room: Room)

    @Update
    suspend fun update(room: Room)

    @Delete
    suspend fun delete(room: Room)

    @Query("SELECT * FROM rooms WHERE id = :id")
    suspend fun getById(id: String): Room?

    @Query("SELECT * FROM rooms WHERE floorId = :floorId")
    fun getByFloorId(floorId: String): Flow<List<Room>>
}
