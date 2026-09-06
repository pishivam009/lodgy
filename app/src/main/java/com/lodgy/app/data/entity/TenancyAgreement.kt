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
    /** Opt-in bookkeeping for a non-revenue room (LODGY-84). A warden living in their own
     *  building has spent nothing, so this is off by default and never automatic; a warden who
     *  pays a caretaker partly in accommodation can switch it on so the rent they give up shows
     *  as a monthly cost. */
    @ColumnInfo(defaultValue = "0")
    val forgoneRentExpense: Boolean = false,
    /** Deliberately NOT [agreedRent]: that field is what the tenancy bills, and a non-revenue
     *  tenancy bills nothing. Keeping the forgone figure in its own column is what stops it
     *  ever being mistaken for billable rent, so LODGY-82's no-invoice guarantee still holds. */
    val forgoneRentAmount: Double? = null,
    val status: AgreementStatus,
    val createdAt: Long,
    val updatedAt: Long,
)
