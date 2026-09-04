package com.lodgy.app.ui.common

import com.lodgy.app.ui.theme.StatusLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomFillTest {

    @Test
    fun `a room with nobody in it is empty`() {
        assertEquals(RoomFill.EMPTY, roomFillOf(totalBeds = 3, occupiedBeds = 0))
    }

    @Test
    fun `a room with some beds taken is partial`() {
        assertEquals(RoomFill.PARTIAL, roomFillOf(totalBeds = 3, occupiedBeds = 1))
        assertEquals(RoomFill.PARTIAL, roomFillOf(totalBeds = 3, occupiedBeds = 2))
    }

    @Test
    fun `a room with every bed taken is full`() {
        assertEquals(RoomFill.FULL, roomFillOf(totalBeds = 3, occupiedBeds = 3))
    }

    @Test
    fun `a single-bed room is only ever empty or full, never partial`() {
        assertEquals(RoomFill.EMPTY, roomFillOf(totalBeds = 1, occupiedBeds = 0))
        assertEquals(RoomFill.FULL, roomFillOf(totalBeds = 1, occupiedBeds = 1))
    }

    /** A room created but not yet given beds must not read as full - it has nobody in it, and
     *  showing it red would hide a room that is still being set up. */
    @Test
    fun `a room with no beds yet reads as empty rather than full`() {
        assertEquals(RoomFill.EMPTY, roomFillOf(totalBeds = 0, occupiedBeds = 0))
    }

    /** Defensive: occupied should never exceed total, but if the counts ever disagree the tile
     *  must still resolve to something rather than fall through. */
    @Test
    fun `more occupied than total still resolves to full`() {
        assertEquals(RoomFill.FULL, roomFillOf(totalBeds = 2, occupiedBeds = 5))
    }

    @Test
    fun `fill maps onto the shared RAG levels, vacant being the good state`() {
        assertEquals(StatusLevel.GOOD, RoomFill.EMPTY.level)
        assertEquals(StatusLevel.WARN, RoomFill.PARTIAL.level)
        assertEquals(StatusLevel.BAD, RoomFill.FULL.level)
    }

    @Test
    fun `every fill state has its own icon, so colour is never the only cue`() {
        val icons = RoomFill.entries.map { it.icon }
        assertEquals(icons.size, icons.distinct().size)
    }
}
