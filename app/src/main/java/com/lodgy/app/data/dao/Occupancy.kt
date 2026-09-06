package com.lodgy.app.data.dao

import com.lodgy.app.data.entity.PropertyType

/** Bed counts rolled up in SQL. Combining one getByRoomId flow per room would re-emit the
 *  whole list on every bed edit and grow with the property; a GROUP BY does not. */
data class RoomOccupancy(val roomId: String, val totalBeds: Int, val occupiedBeds: Int) {
    val vacantBeds: Int get() = totalBeds - occupiedBeds
}

data class FloorOccupancy(val floorId: String, val totalBeds: Int, val occupiedBeds: Int) {
    val vacantBeds: Int get() = totalBeds - occupiedBeds
}

/** A long-vacant bed, named the way a warden would say it out loud. */
data class VacantBedDetail(
    val bedId: String,
    val bedLabel: String,
    val roomNumber: String,
    val floorLabel: String,
    val hostelName: String,
    val vacantSince: Long,
    /** So the nudge can name a shop as itself rather than as a bed inside a room inside
     *  itself, which is what a warden with an empty shop was being told (LODGY-86). */
    val propertyType: PropertyType = PropertyType.HOSTEL,
)

/** A vacant bed with everything needed to describe and price it, in one query. */
/**
 * A vacant space anywhere in the warden's portfolio, for onboarding (LODGY-85). Carries the
 * property and its type because the picker spans every property: a bare room number is ambiguous
 * between two hostels, and a shop's floor and room exist only to keep the hierarchy whole and
 * must never be shown as if they were real (LODGY-79).
 */
data class VacantBedChoice(
    val bedId: String,
    val bedLabel: String,
    val roomNumber: String,
    val floorLabel: String,
    val hostelId: String,
    val hostelName: String,
    val propertyType: PropertyType,
)

data class VacantBedRow(
    val bedId: String,
    val bedLabel: String,
    val roomNumber: String,
    val pricePerBed: Double,
    val floorLabel: String,
    val propertyType: PropertyType = PropertyType.HOSTEL,
)
