package com.lodgy.app.ui.common

import com.lodgy.app.data.entity.BedStatus
import com.lodgy.app.data.entity.InvoiceStatus
import com.lodgy.app.data.entity.TenantStatus
import com.lodgy.app.ui.icons.StatusIcons
import com.lodgy.app.ui.theme.DarkStatusColors
import com.lodgy.app.ui.theme.LightStatusColors
import com.lodgy.app.ui.theme.StatusLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class StatusLevelTest {

    @Test
    fun `a vacant bed is the good state and an occupied one the bad state`() {
        assertEquals(StatusLevel.GOOD, BedStatus.VACANT.level)
        assertEquals(StatusLevel.BAD, BedStatus.OCCUPIED.level)
    }

    @Test
    fun `invoice status maps onto red, amber, green in that order`() {
        assertEquals(StatusLevel.BAD, InvoiceStatus.UNPAID.level)
        assertEquals(StatusLevel.WARN, InvoiceStatus.PARTIAL.level)
        assertEquals(StatusLevel.GOOD, InvoiceStatus.PAID.level)
    }

    @Test
    fun `a vacated tenant is neutral rather than bad - leaving is not a failure state`() {
        assertEquals(StatusLevel.GOOD, TenantStatus.ACTIVE.level)
        assertEquals(StatusLevel.NEUTRAL, TenantStatus.VACATED.level)
    }

    @Test
    fun `each state carries its own icon, so the symbol alone distinguishes them`() {
        assertEquals(StatusIcons.BedVacant, BedStatus.VACANT.icon)
        assertEquals(StatusIcons.BedOccupied, BedStatus.OCCUPIED.icon)
        assertEquals(3, InvoiceStatus.entries.map { it.icon }.distinct().size)
        assertNotEquals(TenantStatus.ACTIVE.icon, TenantStatus.VACATED.icon)
    }

    @Test
    fun `every level resolves to its own distinct palette in both schemes`() {
        for (colors in listOf(LightStatusColors, DarkStatusColors)) {
            val palettes = StatusLevel.entries.map { colors[it] }
            assertEquals(StatusLevel.entries.size, palettes.distinct().size)
            assertEquals(colors.good, colors[StatusLevel.GOOD])
            assertEquals(colors.bad, colors[StatusLevel.BAD])
        }
        assertNotEquals(LightStatusColors.good.container, DarkStatusColors.good.container)
    }
}
