package com.lodgy.app.ui.property

import com.lodgy.app.data.dao.BedOccupancyRow
import com.lodgy.app.data.entity.Bed
import com.lodgy.app.data.entity.BedStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BedHistoryTest {

    private fun row(id: String, start: Long, end: Long?) =
        BedOccupancyRow(periodId = id, tenantId = "t-$id", tenantName = "Tenant $id", startDate = start, endDate = end)

    private fun occupiedBed() = SelectedBed(
        bed = Bed(id = "b1", roomId = "r1", label = "A", status = BedStatus.OCCUPIED, createdAt = 0L, updatedAt = 0L),
        tenantId = "t-current",
        tenantName = "Current Tenant",
    )

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

        // nowMillis pinned to the last checkout itself, so this test stays about the
        // between-tenancies gap and doesn't also exercise the trailing-vacancy entry below.
        val entries = state.pastOccupancyEntries(nowMillis = 700L)

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

    @Test
    fun `a bed that closed and has stayed vacant since gets a trailing vacant entry`() {
        val entries = bedHistoryEntries(listOf(row("p1", 100L, 200L)), nowMillis = 900L)

        assertEquals(
            listOf(BedHistoryEntry.Occupied(row("p1", 100L, 200L)), BedHistoryEntry.Vacant(200L, 900L)),
            entries,
        )
    }

    @Test
    fun `a still-open period never gets a trailing vacant entry, no matter how much later now is`() {
        val entries = bedHistoryEntries(listOf(row("p1", 100L, null)), nowMillis = 9_999_999L)

        assertEquals(listOf(BedHistoryEntry.Occupied(row("p1", 100L, null))), entries)
    }

    @Test
    fun `a bed vacant since its last checkout shows that stretch first in past occupants`() {
        val state = BedGridUiState(bedHistory = listOf(row("p1", 100L, 200L)))

        val entries = state.pastOccupancyEntries(nowMillis = 900L)

        assertEquals(
            listOf(BedHistoryEntry.Vacant(200L, 900L), BedHistoryEntry.Occupied(row("p1", 100L, 200L))),
            entries,
        )
    }

    @Test
    fun `a currently-occupied bed never gets a trailing vacant entry, even if its only recorded period is closed`() {
        // The tenant genuinely in this bed right now predates occupancy-period tracking and has
        // never transferred, so they have no period of their own - the only period on file is an
        // older, unrelated one (e.g. backfilled for a previous tenant). Its being "closed" and
        // "last" must not be read as "the bed is empty" - it is occupied, just not by this record.
        val state = BedGridUiState(bedHistory = listOf(row("p1", 100L, 200L)), selectedBed = occupiedBed())

        val entries = state.pastOccupancyEntries(nowMillis = 900L)

        assertEquals(listOf(BedHistoryEntry.Occupied(row("p1", 100L, 200L))), entries)
    }

    @Test
    fun `bedHistoryEntries itself suppresses the trailing entry when currentlyOccupied is true`() {
        val entries = bedHistoryEntries(listOf(row("p1", 100L, 200L)), nowMillis = 900L, currentlyOccupied = true)

        assertEquals(listOf(BedHistoryEntry.Occupied(row("p1", 100L, 200L))), entries)
    }
}
