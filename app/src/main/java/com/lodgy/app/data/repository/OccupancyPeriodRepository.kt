package com.lodgy.app.data.repository

import com.lodgy.app.data.dao.BackfilledStayRow
import com.lodgy.app.data.dao.BedOccupancyRow
import com.lodgy.app.data.dao.OccupancyPeriodDao
import com.lodgy.app.data.dao.TenantStayRow
import com.lodgy.app.data.entity.OccupancyPeriod
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** What a warden's backfilled stay resolved to (LODGY-94): saved, or refused because it would sit
 *  two tenancies on the same bed at once. */
sealed interface BackfillOutcome {
    data class Saved(val period: OccupancyPeriod) : BackfillOutcome
    data object Overlaps : BackfillOutcome
}

class OccupancyPeriodRepository @Inject constructor(private val dao: OccupancyPeriodDao) {
    suspend fun getByBedId(bedId: String): List<OccupancyPeriod> = dao.getByBedId(bedId)

    suspend fun getByTenantId(tenantId: String): List<OccupancyPeriod> = dao.getByTenantId(tenantId)

    fun observeStaysByTenantId(tenantId: String): Flow<List<TenantStayRow>> = dao.observeStaysByTenantId(tenantId)

    fun observeByBedId(bedId: String): Flow<List<BedOccupancyRow>> = dao.observeByBedId(bedId)

    suspend fun getBackfilledStays(): List<BackfilledStayRow> = dao.getBackfilledStays()

    suspend fun getById(id: String): OccupancyPeriod? = dao.getById(id)

    /** Opens a new span for a tenant taking up a bed - onboarding's first bed, or the bed a
     *  transfer moves them to. */
    suspend fun open(
        tenantId: String,
        bedId: String,
        tenancyAgreementId: String?,
        startDate: Long,
    ): OccupancyPeriod {
        val now = System.currentTimeMillis()
        val period = OccupancyPeriod(
            tenantId = tenantId,
            bedId = bedId,
            tenancyAgreementId = tenancyAgreementId,
            startDate = startDate,
            endDate = null,
            createdAt = now,
            updatedAt = now,
        )
        dao.insert(period)
        return period
    }

    /** Closes the agreement's currently open span, at a transfer's move or a checkout's
     *  move-out. A no-op if none is open, so callers never have to check first. */
    suspend fun close(tenancyAgreementId: String, endDate: Long) {
        val open = dao.getOpenByTenancyAgreementId(tenancyAgreementId) ?: return
        dao.update(open.copy(endDate = endDate, updatedAt = System.currentTimeMillis()))
    }

    /**
     * Records a stay the warden remembers rather than one the app watched happen (LODGY-94) - no
     * agreement, no invoice, nothing else touched. Refused rather than saved if it would put two
     * tenancies on the same bed at the same time, which is where a warden's own typos surface.
     */
    suspend fun backfill(tenantId: String, bedId: String, startDate: Long, endDate: Long): BackfillOutcome {
        if (overlaps(bedId, startDate, endDate, excludingPeriodId = null)) return BackfillOutcome.Overlaps
        val now = System.currentTimeMillis()
        val period = OccupancyPeriod(
            tenantId = tenantId,
            bedId = bedId,
            tenancyAgreementId = null,
            startDate = startDate,
            endDate = endDate,
            backfilled = true,
            createdAt = now,
            updatedAt = now,
        )
        dao.insert(period)
        return BackfillOutcome.Saved(period)
    }

    /** Corrects an already-backfilled stay in place - a warden entering years of history from
     *  paper will get some of it wrong (LODGY-94). */
    suspend fun editBackfilled(period: OccupancyPeriod, bedId: String, startDate: Long, endDate: Long): BackfillOutcome {
        if (overlaps(bedId, startDate, endDate, excludingPeriodId = period.id)) return BackfillOutcome.Overlaps
        val updated = period.copy(
            bedId = bedId,
            startDate = startDate,
            endDate = endDate,
            updatedAt = System.currentTimeMillis(),
        )
        dao.update(updated)
        return BackfillOutcome.Saved(updated)
    }

    suspend fun delete(period: OccupancyPeriod) = dao.delete(period)

    private suspend fun overlaps(bedId: String, startDate: Long, endDate: Long, excludingPeriodId: String?): Boolean =
        dao.getByBedId(bedId).any { existing ->
            existing.id != excludingPeriodId &&
                existing.startDate <= endDate &&
                (existing.endDate == null || existing.endDate >= startDate)
        }
}
