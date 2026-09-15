package com.lodgy.app.ui.property

import com.lodgy.app.data.dao.BedOccupancyRow

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
 * plain and testable without a Composition.
 */
internal fun bedHistoryEntries(periods: List<BedOccupancyRow>): List<BedHistoryEntry> {
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
    return result
}

/** The past-occupants list for the sheet: most recent first, the still-open period left out since
 *  the sheet already names the current occupant above this list. */
internal fun BedGridUiState.pastOccupancyEntries(): List<BedHistoryEntry> =
    bedHistoryEntries(bedHistory)
        .filterNot { it is BedHistoryEntry.Occupied && it.row.endDate == null }
        .reversed()
