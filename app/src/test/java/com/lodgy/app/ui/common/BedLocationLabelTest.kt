package com.lodgy.app.ui.common

import com.lodgy.app.data.dao.BedLocation
import com.lodgy.app.data.entity.PropertyType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * LODGY-86. The label reaches the tenant directory, the profile, the invoice list, the payment
 * acknowledgement and the transfer screen, so getting it wrong showed "Room Corner shop · Bed A"
 * in five places at once. The Composable itself needs a resource lookup; what is worth pinning
 * here is the branch it takes and the data that feeds it.
 */
class BedLocationLabelTest {

    @Test
    fun `a single-unit property carries its own name and is flagged as such`() {
        val shop = BedLocation("Corner shop", "A", PropertyType.SHOP, "Corner shop")

        assertTrue(shop.propertyType.isSingleUnit)
        assertEquals("Corner shop", shop.propertyName)
    }

    @Test
    fun `a hostel bed is not single-unit, so it keeps room and bed`() {
        val bed = BedLocation("101", "B", PropertyType.HOSTEL, "Sunrise PG")

        assertTrue(!bed.propertyType.isSingleUnit)
        assertEquals("101", bed.roomNumber)
        assertEquals("B", bed.bedLabel)
    }

    /** Every pre-existing construction site omits the new fields, and must keep behaving as a
     *  hostel bed rather than silently becoming a single unit. */
    @Test
    fun `the default is a hostel bed`() {
        assertTrue(!BedLocation("101", "A").propertyType.isSingleUnit)
    }
}
