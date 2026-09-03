package com.lodgy.app.data.repository

import com.lodgy.app.data.dao.HostelDao
import com.lodgy.app.data.entity.Hostel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class HostelRepository @Inject constructor(private val hostelDao: HostelDao) {
    fun getByWardenId(wardenId: String): Flow<List<Hostel>> = hostelDao.getByWardenId(wardenId)

    /** Single-warden app, so "every hostel" needs no warden filter - see DESIGN.md 1. */
    fun getAll(): Flow<List<Hostel>> = hostelDao.getAll()

    suspend fun getById(id: String): Hostel? = hostelDao.getById(id)

    suspend fun create(wardenId: String, name: String, address: String, contactPhone: String): Hostel {
        val now = System.currentTimeMillis()
        val hostel = Hostel(
            wardenId = wardenId,
            name = name,
            address = address,
            contactPhone = contactPhone,
            createdAt = now,
            updatedAt = now,
        )
        hostelDao.insert(hostel)
        return hostel
    }

    suspend fun update(hostel: Hostel, name: String, address: String, contactPhone: String) {
        hostelDao.update(
            hostel.copy(
                name = name,
                address = address,
                contactPhone = contactPhone,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }
}
