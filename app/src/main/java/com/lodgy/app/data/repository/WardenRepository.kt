package com.lodgy.app.data.repository

import com.lodgy.app.data.dao.WardenDao
import com.lodgy.app.data.entity.Warden
import javax.inject.Inject

class WardenRepository @Inject constructor(private val wardenDao: WardenDao) {
    suspend fun getWarden(): Warden? = wardenDao.getFirst()

    suspend fun createWarden(pinHash: String) {
        val now = System.currentTimeMillis()
        wardenDao.insert(Warden(pinHash = pinHash, name = "Warden", createdAt = now, updatedAt = now))
    }
}
