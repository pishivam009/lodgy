package com.lodgy.app.notify

import com.lodgy.app.data.dao.VacantBedDetail

data class VacancyNudgeDecision(
    val toNotify: List<VacantBedDetail>,
    /** Replaces the stored set: what should be considered "already nudged" after this run. */
    val nextNotifiedIds: Set<String>,
)

/**
 * Decides which long-vacant beds are worth waking the warden for.
 *
 * A bed is nudged once and then remembered, so a room that stays empty for a month produces one
 * notification rather than thirty. The memory is pruned to beds that are still vacant, so a bed
 * that gets filled and later empties again is treated as new - which is exactly when the warden
 * wants to hear about it.
 */
fun decideVacancyNudges(
    longVacant: List<VacantBedDetail>,
    currentlyVacantIds: Set<String>,
    alreadyNotified: Set<String>,
): VacancyNudgeDecision {
    val fresh = longVacant.filter { it.bedId !in alreadyNotified }
    val stillRelevant = alreadyNotified intersect currentlyVacantIds
    return VacancyNudgeDecision(
        toNotify = fresh,
        nextNotifiedIds = stillRelevant + fresh.map { it.bedId },
    )
}
