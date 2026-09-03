package com.lodgy.app.data.repository

import com.lodgy.app.data.dao.ReconciliationMarkDao
import com.lodgy.app.data.entity.ReconciliationMark
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ReconciliationRepository @Inject constructor(private val dao: ReconciliationMarkDao) {

    suspend fun getForPeriod(hostelId: String, periodMonth: Int, periodYear: Int): ReconciliationMark? =
        dao.getForPeriod(hostelId, periodMonth, periodYear)

    fun getByHostelId(hostelId: String): Flow<List<ReconciliationMark>> = dao.getByHostelId(hostelId)

    suspend fun mark(hostelId: String, periodMonth: Int, periodYear: Int, note: String?): ReconciliationMark {
        val now = System.currentTimeMillis()
        val existing = dao.getForPeriod(hostelId, periodMonth, periodYear)
        val mark = existing?.copy(note = note, updatedAt = now) ?: ReconciliationMark(
            hostelId = hostelId,
            periodMonth = periodMonth,
            periodYear = periodYear,
            note = note,
            createdAt = now,
            updatedAt = now,
        )
        dao.insert(mark)
        return mark
    }

    /** Reversible: an attestation made by mistake can be taken back. */
    suspend fun unmark(hostelId: String, periodMonth: Int, periodYear: Int) {
        dao.getForPeriod(hostelId, periodMonth, periodYear)?.let { dao.delete(it) }
    }
}
