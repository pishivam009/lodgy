package com.lodgy.app.ui.common

import androidx.compose.ui.graphics.vector.ImageVector
import com.lodgy.app.data.entity.BedStatus
import com.lodgy.app.data.entity.InvoiceStatus
import com.lodgy.app.data.entity.TenantStatus
import com.lodgy.app.ui.icons.StatusIcons
import com.lodgy.app.ui.theme.StatusLevel

/** Shared by every screen that lists beds, so the three read the same way. */
enum class BedFilter { ALL, VACANT, OCCUPIED }

fun BedFilter.matches(status: BedStatus): Boolean = when (this) {
    BedFilter.ALL -> true
    BedFilter.VACANT -> status == BedStatus.VACANT
    BedFilter.OCCUPIED -> status == BedStatus.OCCUPIED
}

/** A vacant bed is the one the warden can still fill, so vacant reads as the good state. */
val BedStatus.level: StatusLevel
    get() = when (this) {
        BedStatus.VACANT -> StatusLevel.GOOD
        BedStatus.OCCUPIED -> StatusLevel.BAD
    }

val InvoiceStatus.level: StatusLevel
    get() = when (this) {
        InvoiceStatus.UNPAID -> StatusLevel.BAD
        InvoiceStatus.PARTIAL -> StatusLevel.WARN
        InvoiceStatus.PAID -> StatusLevel.GOOD
    }

val TenantStatus.level: StatusLevel
    get() = when (this) {
        TenantStatus.ACTIVE -> StatusLevel.GOOD
        TenantStatus.VACATED -> StatusLevel.NEUTRAL
    }

val BedStatus.icon: ImageVector
    get() = if (this == BedStatus.OCCUPIED) StatusIcons.BedOccupied else StatusIcons.BedVacant

val InvoiceStatus.icon: ImageVector
    get() = when (this) {
        InvoiceStatus.UNPAID -> StatusIcons.Alert
        InvoiceStatus.PARTIAL -> StatusIcons.Half
        InvoiceStatus.PAID -> StatusIcons.Check
    }

val TenantStatus.icon: ImageVector
    get() = if (this == TenantStatus.ACTIVE) StatusIcons.Check else StatusIcons.Exit
