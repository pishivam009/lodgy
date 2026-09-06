package com.lodgy.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.lodgy.app.R
import com.lodgy.app.data.dao.BedLocation

/**
 * "Room 204 · Bed B" — how wardens identify a tenant, so it sits next to the name everywhere.
 *
 * A shop, warehouse or flat is named as itself instead. Its room and bed exist only to keep the
 * hierarchy whole, so "Room Corner shop · Bed A" was both wrong and absurd — and this label is
 * the one that reaches the tenant directory, the profile, the invoice list, the acknowledgement
 * and the transfer screen, which is why the fix belongs here rather than at each call site
 * (LODGY-79, LODGY-86).
 */
@Composable
fun BedLocation.label(): String = if (propertyType.isSingleUnit) {
    propertyName.ifBlank { roomNumber }
} else {
    stringResource(R.string.bed_location, roomNumber, bedLabel)
}
