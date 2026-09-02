package com.lodgy.app.data.repository

import com.lodgy.app.data.dao.RoomDao
import com.lodgy.app.data.entity.Room
import com.lodgy.app.data.entity.RoomType
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class RoomRepository @Inject constructor(private val roomDao: RoomDao) {
    fun getByFloorId(floorId: String): Flow<List<Room>> = roomDao.getByFloorId(floorId)

    suspend fun getById(id: String): Room? = roomDao.getById(id)

    suspend fun create(
        floorId: String,
        roomNumber: String,
        type: RoomType,
        pricePerBed: Double,
        amenities: String,
    ): Room {
        val now = System.currentTimeMillis()
        val room = Room(
            floorId = floorId,
            roomNumber = roomNumber,
            type = type,
            pricePerBed = pricePerBed,
            amenities = amenities,
            createdAt = now,
            updatedAt = now,
        )
        roomDao.insert(room)
        return room
    }

    suspend fun update(room: Room, roomNumber: String, type: RoomType, pricePerBed: Double, amenities: String) {
        roomDao.update(
            room.copy(
                roomNumber = roomNumber,
                type = type,
                pricePerBed = pricePerBed,
                amenities = amenities,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun delete(room: Room) = roomDao.delete(room)
}
