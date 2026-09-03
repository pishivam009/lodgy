package com.lodgy.app.data.dao

/** Where a bed physically sits, resolved in one query instead of a bed-then-room round trip. */
data class BedLocation(val roomNumber: String, val bedLabel: String)
