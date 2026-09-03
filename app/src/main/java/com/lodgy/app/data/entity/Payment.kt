package com.lodgy.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = Invoice::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
        ),
    ],
    indices = [Index("invoiceId")],
)
data class Payment(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val invoiceId: String,
    val amount: Double,
    val paymentMode: PaymentMode,
    val paidOn: Long,
    val note: String?,
    /** Shared by every row a single lump-sum transaction produced, so a payment that cleared
     *  several months stays recognisable as one payment rather than looking like several
     *  ordinary ones. Null for a normal single-period payment. */
    val multiPeriodGroupId: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)
