package com.lodgy.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.lodgy.app.R
import com.lodgy.app.data.dao.BedLocation

/** "Room 204 · Bed B" — how wardens identify a tenant, so it sits next to the name everywhere. */
@Composable
fun BedLocation.label(): String = stringResource(R.string.bed_location, roomNumber, bedLabel)
