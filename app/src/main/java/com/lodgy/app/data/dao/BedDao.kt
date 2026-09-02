package com.lodgy.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lodgy.app.data.entity.Bed
import kotlinx.coroutines.flow.Flow

@Dao
interface BedDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bed: Bed)

    @Update
    suspend fun update(bed: Bed)

    @Delete
    suspend fun delete(bed: Bed)

    @Query("SELECT * FROM beds WHERE id = :id")
    suspend fun getById(id: String): Bed?

    @Query("SELECT * FROM beds WHERE roomId = :roomId")
    fun getByRoomId(roomId: String): Flow<List<Bed>>
}
