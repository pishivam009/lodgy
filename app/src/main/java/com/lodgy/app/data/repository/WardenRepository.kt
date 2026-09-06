package com.lodgy.app.data.repository

import com.lodgy.app.data.dao.WardenDao
import com.lodgy.app.data.entity.Warden
import javax.inject.Inject

class WardenRepository @Inject constructor(private val wardenDao: WardenDao) {
    suspend fun getWarden(): Warden? = wardenDao.getFirst()

    /**
     * Sets the PIN, creating the warden on first launch or updating the existing one afterwards.
     * It must reuse the same row on a re-setup: hostels carry a foreign key to `warden.id`, so
     * replacing the warden with a new id would orphan every property. Keeping the row also means a
     * forgotten-PIN reset (which only blanks the hash) is undone cleanly by the next setup.
     */
    suspend fun setPin(pinHash: String) {
        val now = System.currentTimeMillis()
        val existing = wardenDao.getFirst()
        if (existing == null) {
            wardenDao.insert(Warden(pinHash = pinHash, name = "Warden", createdAt = now, updatedAt = now))
        } else {
            wardenDao.update(existing.copy(pinHash = pinHash, updatedAt = now))
        }
    }

    /**
     * Forgotten-PIN recovery (LODGY-76): blank the stored PIN so the next launch asks the warden to
     * set a new one, WITHOUT deleting the warden row - hostels reference it, and deleting it fails a
     * foreign-key constraint. The warden's data is deliberately left in place; only the hash goes.
     */
    suspend fun clearWarden() {
        val existing = wardenDao.getFirst() ?: return
        wardenDao.update(existing.copy(pinHash = "", updatedAt = System.currentTimeMillis()))
    }
}
