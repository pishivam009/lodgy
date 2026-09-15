package com.lodgy.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * A tenant's unbroken span on one bed. Onboarding opens the first one, a transfer closes the old
 * bed's span and opens a new one on the same agreement, checkout closes the last one - this is
 * what lets a bed or a tenant answer "who occupied this, and when" past the current [Bed]/
 * [TenancyAgreement.bedId] snapshot (LODGY-91).
 */
@Entity(
    tableName = "occupancy_periods",
    foreignKeys = [
        ForeignKey(entity = Tenant::class, parentColumns = ["id"], childColumns = ["tenantId"]),
        ForeignKey(entity = Bed::class, parentColumns = ["id"], childColumns = ["bedId"]),
        ForeignKey(entity = TenancyAgreement::class, parentColumns = ["id"], childColumns = ["tenancyAgreementId"]),
    ],
    indices = [Index("tenantId"), Index("bedId"), Index("tenancyAgreementId")],
)
data class OccupancyPeriod(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val tenantId: String,
    val bedId: String,
    /** Null for a stay the warden entered from memory rather than one the app watched happen
     *  (LODGY-94) - someone who left before the app existed may have no agreement to attach a
     *  period to. */
    val tenancyAgreementId: String?,
    val startDate: Long,
    /** Null while this is the tenant's current bed. */
    val endDate: Long?,
    /** True when the warden typed this in from memory or paper rather than the app recording it
     *  as it happened (LODGY-94) - not a trust flag, just where the row came from. */
    @ColumnInfo(defaultValue = "0")
    val backfilled: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)
