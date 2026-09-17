package com.lodgy.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lodgy.app.data.entity.OccupancyPeriod
import kotlinx.coroutines.flow.Flow

@Dao
interface OccupancyPeriodDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(period: OccupancyPeriod)

    @Update
    suspend fun update(period: OccupancyPeriod)

    @Delete
    suspend fun delete(period: OccupancyPeriod)

    @Query("SELECT * FROM occupancy_periods WHERE id = :id")
    suspend fun getById(id: String): OccupancyPeriod?

    /** Every warden-entered stay, named the way a warden would read it, for the backfill screen's
     *  correct/remove list (LODGY-94). */
    @Query(
        "SELECT occupancy_periods.id AS periodId, tenants.name AS tenantName, " +
            "beds.label AS bedLabel, rooms.roomNumber AS roomNumber, hostels.name AS hostelName, " +
            "hostels.propertyType AS propertyType, occupancy_periods.startDate AS startDate, " +
            "occupancy_periods.endDate AS endDate " +
            "FROM occupancy_periods " +
            "INNER JOIN tenants ON tenants.id = occupancy_periods.tenantId " +
            "INNER JOIN beds ON beds.id = occupancy_periods.bedId " +
            "INNER JOIN rooms ON rooms.id = beds.roomId " +
            "INNER JOIN floors ON floors.id = rooms.floorId " +
            "INNER JOIN hostels ON hostels.id = floors.hostelId " +
            "WHERE occupancy_periods.backfilled = 1 " +
            "ORDER BY occupancy_periods.startDate DESC",
    )
    suspend fun getBackfilledStays(): List<BackfilledStayRow>

    /** The one still-open span for an agreement's current bed - there is at most one, since a
     *  transfer closes it in the same operation that opens the next. */
    @Query(
        "SELECT * FROM occupancy_periods WHERE tenancyAgreementId = :tenancyAgreementId " +
            "AND endDate IS NULL LIMIT 1",
    )
    suspend fun getOpenByTenancyAgreementId(tenancyAgreementId: String): OccupancyPeriod?

    @Query("SELECT * FROM occupancy_periods WHERE bedId = :bedId ORDER BY startDate DESC")
    suspend fun getByBedId(bedId: String): List<OccupancyPeriod>

    @Query("SELECT * FROM occupancy_periods WHERE tenantId = :tenantId ORDER BY startDate DESC")
    suspend fun getByTenantId(tenantId: String): List<OccupancyPeriod>

    /** A tenant's whole bed history, location resolved and live - a transfer or a backfill must
     *  show up on the timeline without a re-open (LODGY-92, LODGY-33's observed-query rule). */
    @Query(
        "SELECT occupancy_periods.id AS periodId, " +
            "occupancy_periods.tenancyAgreementId AS tenancyAgreementId, " +
            "beds.label AS bedLabel, rooms.roomNumber AS roomNumber, hostels.name AS hostelName, " +
            "hostels.propertyType AS propertyType, occupancy_periods.startDate AS startDate, " +
            "occupancy_periods.endDate AS endDate, occupancy_periods.backfilled AS backfilled " +
            "FROM occupancy_periods " +
            "INNER JOIN beds ON beds.id = occupancy_periods.bedId " +
            "INNER JOIN rooms ON rooms.id = beds.roomId " +
            "INNER JOIN floors ON floors.id = rooms.floorId " +
            "INNER JOIN hostels ON hostels.id = floors.hostelId " +
            "WHERE occupancy_periods.tenantId = :tenantId " +
            "ORDER BY occupancy_periods.startDate ASC",
    )
    fun observeStaysByTenantId(tenantId: String): Flow<List<TenantStayRow>>

    /** Everyone who has ever occupied this bed, current tenant included - the bed sheet's history
     *  list, live so a transfer or checkout updates it without a re-open (LODGY-93). */
    @Query(
        "SELECT occupancy_periods.id AS periodId, tenants.id AS tenantId, tenants.name AS tenantName, " +
            "occupancy_periods.startDate AS startDate, occupancy_periods.endDate AS endDate " +
            "FROM occupancy_periods INNER JOIN tenants ON tenants.id = occupancy_periods.tenantId " +
            "WHERE occupancy_periods.bedId = :bedId " +
            "ORDER BY occupancy_periods.startDate DESC",
    )
    fun observeByBedId(bedId: String): Flow<List<BedOccupancyRow>>
}
