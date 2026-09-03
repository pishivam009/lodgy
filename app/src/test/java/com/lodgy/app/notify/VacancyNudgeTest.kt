package com.lodgy.app.notify

import com.lodgy.app.data.dao.VacantBedDetail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VacancyNudgeTest {

    private fun bed(id: String) =
        VacantBedDetail(bedId = id, bedLabel = "A", roomNumber = "101", floorLabel = "G", hostelName = "Sunrise", vacantSince = 0L)

    @Test
    fun `a newly long-vacant bed is nudged and then remembered`() {
        val decision = decideVacancyNudges(
            longVacant = listOf(bed("b1")),
            currentlyVacantIds = setOf("b1"),
            alreadyNotified = emptySet(),
        )

        assertEquals(listOf("b1"), decision.toNotify.map { it.bedId })
        assertEquals(setOf("b1"), decision.nextNotifiedIds)
    }

    @Test
    fun `a bed already nudged about is not nudged again the next day`() {
        val decision = decideVacancyNudges(
            longVacant = listOf(bed("b1")),
            currentlyVacantIds = setOf("b1"),
            alreadyNotified = setOf("b1"),
        )

        assertTrue(decision.toNotify.isEmpty())
        assertEquals(setOf("b1"), decision.nextNotifiedIds)
    }

    @Test
    fun `a bed that got filled is forgotten, so it can be nudged again if it empties later`() {
        val filled = decideVacancyNudges(
            longVacant = emptyList(),
            currentlyVacantIds = emptySet(),
            alreadyNotified = setOf("b1"),
        )
        assertTrue(filled.nextNotifiedIds.isEmpty())

        val emptyAgain = decideVacancyNudges(
            longVacant = listOf(bed("b1")),
            currentlyVacantIds = setOf("b1"),
            alreadyNotified = filled.nextNotifiedIds,
        )
        assertEquals(listOf("b1"), emptyAgain.toNotify.map { it.bedId })
    }

    @Test
    fun `only the beds past the threshold are nudged, the rest are not remembered`() {
        val decision = decideVacancyNudges(
            longVacant = listOf(bed("old")),
            currentlyVacantIds = setOf("old", "recent"),
            alreadyNotified = emptySet(),
        )

        assertEquals(listOf("old"), decision.toNotify.map { it.bedId })
        assertEquals(setOf("old"), decision.nextNotifiedIds)
    }

    @Test
    fun `nothing vacant means nothing to say and nothing to remember`() {
        val decision = decideVacancyNudges(emptyList(), emptySet(), emptySet())

        assertTrue(decision.toNotify.isEmpty())
        assertTrue(decision.nextNotifiedIds.isEmpty())
    }
}
