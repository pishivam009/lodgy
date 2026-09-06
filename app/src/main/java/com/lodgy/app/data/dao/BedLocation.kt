package com.lodgy.app.data.dao

import com.lodgy.app.data.entity.PropertyType

/** Where a tenant lives, as the warden would say it. Carries the property type because a shop,
 *  warehouse or flat has no room and no bed to name - its room and bed rows exist only to keep
 *  the hierarchy whole, and printing them reads as nonsense (LODGY-79, LODGY-86). */
data class BedLocation(
    val roomNumber: String,
    val bedLabel: String,
    val propertyType: PropertyType = PropertyType.HOSTEL,
    val propertyName: String = "",
)
