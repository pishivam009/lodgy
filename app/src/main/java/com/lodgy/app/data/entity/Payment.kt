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
    val createdAt: Long,
    val updatedAt: Long,
)
