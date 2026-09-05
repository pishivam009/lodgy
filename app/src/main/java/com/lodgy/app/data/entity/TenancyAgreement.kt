package com.lodgy.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "tenancy_agreements",
    foreignKeys = [
        ForeignKey(
            entity = Tenant::class,
            parentColumns = ["id"],
            childColumns = ["tenantId"],
        ),
        ForeignKey(
            entity = Bed::class,
            parentColumns = ["id"],
            childColumns = ["bedId"],
        ),
    ],
    indices = [Index("tenantId"), Index("bedId")],
)
data class TenancyAgreement(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val tenantId: String,
    val bedId: String,
    val agreedRent: Double,
    val advanceDeposit: Double,
    val billingCycleDay: Int,
    val moveInDate: Long,
    val moveOutDate: Long?,
    val depositRefundAmount: Double?,
    /** A room the warden or a caretaker lives in: real occupancy, no rent. Without this such a
     *  bed could only be modelled wrongly - left vacant, which corrupts occupancy and invites the
     *  long-vacancy nudge, or given a real tenancy that bills forever and shows as overdue
     *  (LODGY-82). Defaults false so every existing agreement is unaffected. */
    @ColumnInfo(defaultValue = "0")
    val nonRevenue: Boolean = false,
    val status: AgreementStatus,
    val createdAt: Long,
    val updatedAt: Long,
)
