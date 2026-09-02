package com.lodgy.app.data.entity

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
    val status: AgreementStatus,
    val createdAt: Long,
    val updatedAt: Long,
)
