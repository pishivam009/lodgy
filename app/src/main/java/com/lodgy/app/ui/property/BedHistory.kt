package com.lodgy.app.ui.property

import com.lodgy.app.data.dao.BedOccupancyRow
import com.lodgy.app.data.entity.BedStatus

/** A row in the bed sheet's history list - either a tenancy that occupied the bed, or a vacant
 *  stretch between two tenancies, since "why does this bed keep sitting empty" is as much a reason
 *  to open this sheet as who was in it (LODGY-93). */
sealed interface BedHistoryEntry {
    data class Occupied(val row: BedOccupancyRow) : BedHistoryEntry
    data class Vacant(val fromMillis: Long, val toMillis: Long) : BedHistoryEntry
}

/**
 * Turns a bed's periods (any order) into a chronological list with the gaps between them made
 * explicit. Pulled out of the screen so the gap arithmetic, the part worth getting right, is
 * plain and testable without a Composition. A bed whose most recent period has already closed,
 * with nothing after it, gets a trailing entry for the stretch from that checkout to [nowMillis] -
 * "why does this bed keep sitting empty" applies just as much to a bed nobody has moved into since
 * the last tenant left as it does to a gap between two tenants (LODGY-95).
 *
 * [currentlyOccupied] must come from the bed's actual status, not be inferred from whether the
 * last period is closed: a bed can be genuinely occupied right now by a tenancy that predates
 * occupancy-period tracking and has never transferred, which has no period of its own at all. If
 * that same bed also carries an older, unrelated closed period (e.g. a backfilled stay from a
 * previous tenant), that period being "last" doesn't mean the bed is empty - it means the bed's
 * current occupant simply isn't tracked here. Trusting the periods alone for this produced exactly
 * that false "vacant" reading in QA, on a bed that was occupied the whole time.
 */
internal fun bedHistoryEntries(
    periods: List<BedOccupancyRow>,
    nowMillis: Long = System.currentTimeMillis(),
    currentlyOccupied: Boolean = false,
): List<BedHistoryEntry> {
    val chronological = periods.sortedBy { it.startDate }
    val result = mutableListOf<BedHistoryEntry>()
    chronological.forEachIndexed { index, period ->
        if (index > 0) {
            val previousEnd = chronological[index - 1].endDate
            if (previousEnd != null && period.startDate > previousEnd) {
                result += BedHistoryEntry.Vacant(previousEnd, period.startDate)
            }
        }
        result += BedHistoryEntry.Occupied(period)
    }
    val lastEnd = chronological.lastOrNull()?.endDate
    if (!currentlyOccupied && lastEnd != null && nowMillis > lastEnd) {
        result += BedHistoryEntry.Vacant(lastEnd, nowMillis)
    }
    return result
}

/** The past-occupants list for the sheet: most recent first, the still-open period left out since
 *  the sheet already names the current occupant above this list. */
internal fun BedGridUiState.pastOccupancyEntries(nowMillis: Long = System.currentTimeMillis()): List<BedHistoryEntry> =
    bedHistoryEntries(bedHistory, nowMillis, currentlyOccupied = selectedBed?.bed?.status == BedStatus.OCCUPIED)
        .filterNot { it is BedHistoryEntry.Occupied && it.row.endDate == null }
        .reversed()
