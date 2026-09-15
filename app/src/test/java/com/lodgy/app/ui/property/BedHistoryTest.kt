package com.lodgy.app.ui.property

import com.lodgy.app.data.dao.BedOccupancyRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BedHistoryTest {

    private fun row(id: String, start: Long, end: Long?) =
        BedOccupancyRow(periodId = id, tenantId = "t-$id", tenantName = "Tenant $id", startDate = start, endDate = end)

    @Test
    fun `a bed with one still-open period has no gap`() {
        val entries = bedHistoryEntries(listOf(row("p1", 100L, null)))

        assertEquals(listOf(BedHistoryEntry.Occupied(row("p1", 100L, null))), entries)
    }

    @Test
    fun `back-to-back periods produce no vacant gap`() {
        val entries = bedHistoryEntries(listOf(row("p1", 100L, 200L), row("p2", 200L, null)))

        assertEquals(
            listOf(BedHistoryEntry.Occupied(row("p1", 100L, 200L)), BedHistoryEntry.Occupied(row("p2", 200L, null))),
            entries,
        )
    }

    @Test
    fun `a real gap between two periods is surfaced as a vacant stretch`() {
        val entries = bedHistoryEntries(listOf(row("p1", 100L, 200L), row("p2", 500L, null)))

        assertEquals(
            listOf(
                BedHistoryEntry.Occupied(row("p1", 100L, 200L)),
                BedHistoryEntry.Vacant(200L, 500L),
                BedHistoryEntry.Occupied(row("p2", 500L, null)),
            ),
            entries,
        )
    }

    @Test
    fun `input order does not matter - periods are sorted chronologically first`() {
        val entries = bedHistoryEntries(listOf(row("p2", 500L, null), row("p1", 100L, 200L)))

        assertEquals(BedHistoryEntry.Occupied(row("p1", 100L, 200L)), entries.first())
    }

    @Test
    fun `an empty bed has no entries at all`() {
        assertTrue(bedHistoryEntries(emptyList()).isEmpty())
    }

    @Test
    fun `past-occupancy entries exclude the still-open period itself, most recent first`() {
        val state = BedGridUiState(
            bedHistory = listOf(row("p1", 100L, 200L), row("p2", 500L, null)),
        )

        val entries = state.pastOccupancyEntries()

        assertTrue(entries.none { it is BedHistoryEntry.Occupied && it.row.periodId == "p2" })
    }

    @Test
    fun `past-occupancy entries include a vacant gap even right before the current tenant`() {
        val state = BedGridUiState(
            bedHistory = listOf(row("p1", 100L, 200L), row("p2", 500L, null)),
        )

        val entries = state.pastOccupancyEntries()

        // p2 itself is excluded (still open, shown elsewhere as the current occupant), but the
        // stretch before it moved in is still a real answer to "why does this keep sitting empty".
        assertEquals(
            listOf(BedHistoryEntry.Vacant(200L, 500L), BedHistoryEntry.Occupied(row("p1", 100L, 200L))),
            entries,
        )
    }

    @Test
    fun `a closed history with a gap shows the gap, most recent first`() {
        val state = BedGridUiState(
            bedHistory = listOf(row("p1", 100L, 200L), row("p2", 500L, 700L)),
        )

        val entries = state.pastOccupancyEntries()

        assertEquals(
            listOf(
                BedHistoryEntry.Occupied(row("p2", 500L, 700L)),
                BedHistoryEntry.Vacant(200L, 500L),
                BedHistoryEntry.Occupied(row("p1", 100L, 200L)),
            ),
            entries,
        )
    }

    @Test
    fun `no periods at all means no past-occupancy entries`() {
        assertTrue(BedGridUiState().pastOccupancyEntries().isEmpty())
    }
}
