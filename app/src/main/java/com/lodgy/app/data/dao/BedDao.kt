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

    @Query(
        "SELECT rooms.roomNumber AS roomNumber, beds.label AS bedLabel FROM beds " +
            "INNER JOIN rooms ON rooms.id = beds.roomId WHERE beds.id = :bedId",
    )
    suspend fun getLocation(bedId: String): BedLocation?

    @Query(
        "SELECT beds.roomId AS roomId, COUNT(*) AS totalBeds, " +
            "SUM(CASE WHEN beds.status = 'OCCUPIED' THEN 1 ELSE 0 END) AS occupiedBeds " +
            "FROM beds INNER JOIN rooms ON rooms.id = beds.roomId " +
            "WHERE rooms.floorId = :floorId GROUP BY beds.roomId",
    )
    fun observeOccupancyByFloor(floorId: String): Flow<List<RoomOccupancy>>

    /** LEFT JOINs so a floor with no rooms (or rooms with no beds yet) still reports 0/0
     *  rather than vanishing from the floor list's summary. */
    @Query(
        "SELECT floors.id AS floorId, COUNT(beds.id) AS totalBeds, " +
            "SUM(CASE WHEN beds.status = 'OCCUPIED' THEN 1 ELSE 0 END) AS occupiedBeds " +
            "FROM floors LEFT JOIN rooms ON rooms.floorId = floors.id " +
            "LEFT JOIN beds ON beds.roomId = rooms.id " +
            "WHERE floors.hostelId = :hostelId GROUP BY floors.id",
    )
    fun observeOccupancyByHostel(hostelId: String): Flow<List<FloorOccupancy>>

    @Query(
        "SELECT beds.roomId AS roomId, COUNT(*) AS totalBeds, " +
            "SUM(CASE WHEN beds.status = 'OCCUPIED' THEN 1 ELSE 0 END) AS occupiedBeds " +
            "FROM beds INNER JOIN rooms ON rooms.id = beds.roomId " +
            "INNER JOIN floors ON floors.id = rooms.floorId " +
            "WHERE floors.hostelId = :hostelId GROUP BY beds.roomId",
    )
    fun observeRoomOccupancyByHostel(hostelId: String): Flow<List<RoomOccupancy>>

    /** Room occupancy everywhere, for the all-hostels room view (LODGY-70). */
    @Query(
        "SELECT beds.roomId AS roomId, COUNT(*) AS totalBeds, " +
            "SUM(CASE WHEN beds.status = 'OCCUPIED' THEN 1 ELSE 0 END) AS occupiedBeds " +
            "FROM beds GROUP BY beds.roomId",
    )
    fun observeRoomOccupancy(): Flow<List<RoomOccupancy>>

    @Query(
        "SELECT beds.id AS bedId, beds.label AS bedLabel, rooms.roomNumber AS roomNumber, " +
            "rooms.pricePerBed AS pricePerBed, floors.label AS floorLabel " +
            "FROM beds INNER JOIN rooms ON rooms.id = beds.roomId " +
            "INNER JOIN floors ON floors.id = rooms.floorId " +
            "WHERE floors.hostelId = :hostelId AND beds.status = 'VACANT' " +
            "ORDER BY floors.sortOrder, rooms.roomNumber, beds.label",
    )
    suspend fun getVacantBedsByHostel(hostelId: String): List<VacantBedRow>

    /** beds.updatedAt is when the status last changed, which for a VACANT bed is when it was
     *  freed - the closest thing the schema has to "vacant since". */
    @Query(
        "SELECT beds.id AS bedId, beds.label AS bedLabel, rooms.roomNumber AS roomNumber, " +
            "floors.label AS floorLabel, hostels.name AS hostelName, beds.updatedAt AS vacantSince " +
            "FROM beds INNER JOIN rooms ON rooms.id = beds.roomId " +
            "INNER JOIN floors ON floors.id = rooms.floorId " +
            "INNER JOIN hostels ON hostels.id = floors.hostelId " +
            "WHERE beds.status = 'VACANT' AND beds.updatedAt <= :vacantSinceBefore " +
            "ORDER BY beds.updatedAt",
    )
    suspend fun getLongVacantBeds(vacantSinceBefore: Long): List<VacantBedDetail>

    @Query("SELECT id FROM beds WHERE status = 'VACANT'")
    suspend fun getVacantBedIds(): List<String>

    @Query(
        "SELECT floors.hostelId FROM beds " +
            "INNER JOIN rooms ON rooms.id = beds.roomId " +
            "INNER JOIN floors ON floors.id = rooms.floorId WHERE beds.id = :bedId",
    )
    suspend fun getHostelId(bedId: String): String?

    /** Tenants whose ACTIVE tenancy sits on a bed under this floor. These are what actually block a
     *  floor delete: the cascade would remove the beds, but Bed <- TenancyAgreement is NO ACTION, so
     *  SQLite refuses and the delete crashes. Naming them lets the block say who is in the way. */
    @Query(
        "SELECT DISTINCT tenants.name FROM tenancy_agreements " +
            "INNER JOIN beds ON beds.id = tenancy_agreements.bedId " +
            "INNER JOIN rooms ON rooms.id = beds.roomId " +
            "INNER JOIN tenants ON tenants.id = tenancy_agreements.tenantId " +
            "WHERE rooms.floorId = :floorId AND tenancy_agreements.status = 'ACTIVE' " +
            "ORDER BY tenants.name",
    )
    suspend fun getActiveTenantNamesOnFloor(floorId: String): List<String>

    /** Same question for one room, so the room-level block can name names too rather than saying
     *  only that "a bed" is occupied. */
    @Query(
        "SELECT DISTINCT tenants.name FROM tenancy_agreements " +
            "INNER JOIN beds ON beds.id = tenancy_agreements.bedId " +
            "INNER JOIN tenants ON tenants.id = tenancy_agreements.tenantId " +
            "WHERE beds.roomId = :roomId AND tenancy_agreements.status = 'ACTIVE' " +
            "ORDER BY tenants.name",
    )
    suspend fun getActiveTenantNamesInRoom(roomId: String): List<String>
}
