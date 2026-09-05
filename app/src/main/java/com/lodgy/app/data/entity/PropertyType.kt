package com.lodgy.app.data.entity

/**
 * What a property actually is, which decides how much of Hostel > Floor > Room > Bed the warden is
 * shown. The schema keeps all four levels for every type - a single-unit property gets one real
 * floor, room and bed rather than nulls - so every existing query, rollup and export keeps working
 * untouched. What changes is only what the UI renders (LODGY-79).
 *
 * Wardens rent out shops, warehouses and whole flats as well as hostel beds. For those the rentable
 * unit IS the property, and before this they had to invent a fake floor and a fake bed to rent
 * anything at all.
 */
enum class PropertyType {
    /** The original model: floors, rooms, and a tenancy per bed. */
    HOSTEL,
    SHOP,
    WAREHOUSE,
    FLAT,
    ;

    /** True when the property is let as a whole, so floors, rooms and beds are noise to the warden. */
    val isSingleUnit: Boolean get() = this != HOSTEL
}
