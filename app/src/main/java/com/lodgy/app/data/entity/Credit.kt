package com.lodgy.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Money the tenant is owed back - typically a repair they paid for out of pocket - recorded as
 * its own row rather than by shrinking [Invoice.amountDue]. An invoice stays the immutable
 * snapshot DESIGN.md 3 describes, and the reason for every rupee of relief stays on the record.
 *
 * [invoiceId] null means "not applied yet": it attaches to the tenant's next generated invoice.
 */
@Entity(
    tableName = "credits",
    foreignKeys = [
        ForeignKey(
            entity = Tenant::class,
            parentColumns = ["id"],
            childColumns = ["tenantId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Invoice::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("tenantId"), Index("invoiceId")],
)
data class Credit(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val tenantId: String,
    val invoiceId: String?,
    val amount: Double,
    val reason: String,
    val createdAt: Long,
    val updatedAt: Long,
)
