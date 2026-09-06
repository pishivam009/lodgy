package com.lodgy.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = Hostel::class,
            parentColumns = ["id"],
            childColumns = ["hostelId"],
        ),
    ],
    indices = [Index("hostelId"), Index("tenancyAgreementId")],
)
data class Expense(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val hostelId: String,
    /** Set only on the forgone rent of a warden or caretaker room (LODGY-84), so a later run
     *  can tell whether this month's entry already exists and the warden can switch it off.
     *  A soft link, not a foreign key: recorded history must survive whatever happens to the
     *  tenancy, and a cascade would erase months the warden has already reported on. */
    val tenancyAgreementId: String? = null,
    val category: ExpenseCategory,
    val amount: Double,
    val isRecurring: Boolean,
    val incurredOn: Long,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
