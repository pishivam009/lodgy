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

/** Any bed anywhere in the warden's portfolio, occupied or not - unlike [VacantBedChoice], for
 *  picking a bed a past stay happened on rather than one to move into (LODGY-94). */
data class BedChoice(
    val bedId: String,
    val bedLabel: String,
    val roomNumber: String,
    val hostelId: String,
    val hostelName: String,
    val propertyType: PropertyType,
)

/** One tenant's span on one bed, with the location resolved for display - the timeline's room
 *  history section groups these by [tenancyAgreementId] into stays (LODGY-92). Covers both
 *  app-recorded and backfilled (LODGY-94) periods alike; the two read the same here. */
data class TenantStayRow(
    val periodId: String,
    val tenancyAgreementId: String?,
    val bedLabel: String,
    val roomNumber: String,
    val hostelName: String,
    val propertyType: PropertyType,
    val startDate: Long,
    val endDate: Long?,
)

/** One tenancy that has occupied a bed, named for the bed sheet's past-occupants list (LODGY-93).
 *  Deliberately no room/bed/property fields - the sheet showing this already knows which bed it
 *  is, and printing that here would be the "Room Corner shop · Bed A" mistake LODGY-86 fixed. */
data class BedOccupancyRow(
    val periodId: String,
    val tenantId: String,
    val tenantName: String,
    val startDate: Long,
    val endDate: Long?,
)

/** A stay the warden entered from memory rather than one the app recorded live, ready to show on
 *  the backfill screen's correct/remove list (LODGY-94). */
data class BackfilledStayRow(
    val periodId: String,
    val tenantName: String,
    val bedLabel: String,
    val roomNumber: String,
    val hostelName: String,
    val propertyType: PropertyType,
    val startDate: Long,
    val endDate: Long?,
)
