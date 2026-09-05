package com.lodgy.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lodgy.app.data.entity.Room
import com.lodgy.app.data.entity.RoomType
import kotlinx.coroutines.flow.Flow

/** A room plus the floor it sits on, so the hostel-wide room list can label each row without
 *  a second lookup per room. */
data class RoomWithFloor(
    val roomId: String,
    val roomNumber: String,
    val type: RoomType,
    val pricePerBed: Double,
    val floorId: String,
    val floorLabel: String,
    /** Carried so a room can be attributed once the view spans properties - two hostels can each
     *  have a Room 101, and a bare room number is ambiguous between them (LODGY-70). */
    val hostelId: String = "",
    val hostelName: String = "",
)

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

    @Query(
        "SELECT rooms.id AS roomId, rooms.roomNumber AS roomNumber, rooms.type AS type, " +
            "rooms.pricePerBed AS pricePerBed, floors.id AS floorId, floors.label AS floorLabel, " +
            "hostels.id AS hostelId, hostels.name AS hostelName " +
            "FROM rooms INNER JOIN floors ON floors.id = rooms.floorId " +
            "INNER JOIN hostels ON hostels.id = floors.hostelId " +
            "WHERE floors.hostelId = :hostelId " +
            "ORDER BY floors.sortOrder, rooms.roomNumber",
    )
    fun getByHostelIdWithFloor(hostelId: String): Flow<List<RoomWithFloor>>

    /** Every room the warden owns, ordered so one property's rooms stay together. */
    @Query(
        "SELECT rooms.id AS roomId, rooms.roomNumber AS roomNumber, rooms.type AS type, " +
            "rooms.pricePerBed AS pricePerBed, floors.id AS floorId, floors.label AS floorLabel, " +
            "hostels.id AS hostelId, hostels.name AS hostelName " +
            "FROM rooms INNER JOIN floors ON floors.id = rooms.floorId " +
            "INNER JOIN hostels ON hostels.id = floors.hostelId " +
            "ORDER BY hostels.name, floors.sortOrder, rooms.roomNumber",
    )
    fun getAllWithFloor(): Flow<List<RoomWithFloor>>
}
