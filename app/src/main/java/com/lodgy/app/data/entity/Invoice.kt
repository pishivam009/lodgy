package com.lodgy.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "invoices",
    foreignKeys = [
        ForeignKey(
            entity = TenancyAgreement::class,
            parentColumns = ["id"],
            childColumns = ["tenancyAgreementId"],
        ),
    ],
    indices = [Index("tenancyAgreementId")],
)
data class Invoice(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val tenancyAgreementId: String,
    val periodMonth: Int,
    val periodYear: Int,
    val amountDue: Double,
    val dueDate: Long,
    val status: InvoiceStatus,
    val createdAt: Long,
    val updatedAt: Long,
)
