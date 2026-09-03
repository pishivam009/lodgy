package com.lodgy.app.data.dao

/** Bed counts rolled up in SQL. Combining one getByRoomId flow per room would re-emit the
 *  whole list on every bed edit and grow with the property; a GROUP BY does not. */
data class RoomOccupancy(val roomId: String, val totalBeds: Int, val occupiedBeds: Int) {
    val vacantBeds: Int get() = totalBeds - occupiedBeds
}

data class FloorOccupancy(val floorId: String, val totalBeds: Int, val occupiedBeds: Int) {
    val vacantBeds: Int get() = totalBeds - occupiedBeds
}

/** A vacant bed with everything needed to describe and price it, in one query. */
data class VacantBedRow(
    val bedId: String,
    val bedLabel: String,
    val roomNumber: String,
    val pricePerBed: Double,
    val floorLabel: String,
)
