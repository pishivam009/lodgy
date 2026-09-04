package com.lodgy.app.data.repository

import com.lodgy.app.data.dao.BedDao
import com.lodgy.app.data.dao.BedLocation
import com.lodgy.app.data.dao.FloorOccupancy
import com.lodgy.app.data.dao.RoomOccupancy
import com.lodgy.app.data.dao.VacantBedDetail
import com.lodgy.app.data.dao.VacantBedRow
import com.lodgy.app.data.entity.Bed
import com.lodgy.app.data.entity.BedStatus
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class BedRepository @Inject constructor(private val bedDao: BedDao) {
    fun getByRoomId(roomId: String): Flow<List<Bed>> = bedDao.getByRoomId(roomId)

    suspend fun getById(id: String): Bed? = bedDao.getById(id)

    suspend fun getLocation(bedId: String): BedLocation? = bedDao.getLocation(bedId)

    suspend fun getHostelId(bedId: String): String? = bedDao.getHostelId(bedId)

    fun observeOccupancyByFloor(floorId: String): Flow<List<RoomOccupancy>> =
        bedDao.observeOccupancyByFloor(floorId)

    fun observeOccupancyByHostel(hostelId: String): Flow<List<FloorOccupancy>> =
        bedDao.observeOccupancyByHostel(hostelId)

    fun observeRoomOccupancyByHostel(hostelId: String): Flow<List<RoomOccupancy>> =
        bedDao.observeRoomOccupancyByHostel(hostelId)

    suspend fun getVacantBedsByHostel(hostelId: String): List<VacantBedRow> =
        bedDao.getVacantBedsByHostel(hostelId)

    suspend fun getLongVacantBeds(vacantSinceBefore: Long): List<VacantBedDetail> =
        bedDao.getLongVacantBeds(vacantSinceBefore)

    suspend fun getVacantBedIds(): List<String> = bedDao.getVacantBedIds()

    suspend fun hasOccupiedBed(roomId: String): Boolean =
        bedDao.getByRoomId(roomId).first().any { it.status == BedStatus.OCCUPIED }

    suspend fun generateForRoom(roomId: String, bedCount: Int) {
        val now = System.currentTimeMillis()
        repeat(bedCount) { index ->
            val label = ('A' + index).toString()
            bedDao.insert(
                Bed(roomId = roomId, label = label, status = BedStatus.VACANT, createdAt = now, updatedAt = now),
            )
        }
    }

    suspend fun setOccupied(bedId: String) = setStatus(bedId, BedStatus.OCCUPIED)

    suspend fun setVacant(bedId: String) = setStatus(bedId, BedStatus.VACANT)

    private suspend fun setStatus(bedId: String, status: BedStatus) {
        val bed = bedDao.getById(bedId) ?: return
        bedDao.update(bed.copy(status = status, updatedAt = System.currentTimeMillis()))
    }
}
