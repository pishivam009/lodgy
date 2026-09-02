package com.lodgy.app.data.repository

import com.lodgy.app.data.dao.FloorDao
import com.lodgy.app.data.entity.Floor
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class FloorRepository @Inject constructor(private val floorDao: FloorDao) {
    fun getByHostelId(hostelId: String): Flow<List<Floor>> = floorDao.getByHostelId(hostelId)

    suspend fun getById(id: String): Floor? = floorDao.getById(id)

    suspend fun create(hostelId: String, label: String): Floor {
        val nextSortOrder = (floorDao.getByHostelId(hostelId).first().maxOfOrNull { it.sortOrder } ?: -1) + 1
        val now = System.currentTimeMillis()
        val floor = Floor(
            hostelId = hostelId,
            label = label,
            sortOrder = nextSortOrder,
            createdAt = now,
            updatedAt = now,
        )
        floorDao.insert(floor)
        return floor
    }

    suspend fun rename(floor: Floor, label: String) {
        floorDao.update(floor.copy(label = label, updatedAt = System.currentTimeMillis()))
    }

    suspend fun delete(floor: Floor) {
        floorDao.delete(floor)
    }

    suspend fun moveUp(floor: Floor, hostelId: String) = swapWithNeighbor(floor, hostelId, isUp = true)

    suspend fun moveDown(floor: Floor, hostelId: String) = swapWithNeighbor(floor, hostelId, isUp = false)

    private suspend fun swapWithNeighbor(floor: Floor, hostelId: String, isUp: Boolean) {
        val floors = floorDao.getByHostelId(hostelId).first().sortedBy { it.sortOrder }
        val index = floors.indexOfFirst { it.id == floor.id }
        val neighborIndex = if (isUp) index - 1 else index + 1
        if (index < 0 || neighborIndex < 0 || neighborIndex >= floors.size) return

        val neighbor = floors[neighborIndex]
        val now = System.currentTimeMillis()
        floorDao.update(floor.copy(sortOrder = neighbor.sortOrder, updatedAt = now))
        floorDao.update(neighbor.copy(sortOrder = floor.sortOrder, updatedAt = now))
    }
}
