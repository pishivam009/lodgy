package com.lodgy.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * The warden's own attestation that a month's figures have been checked against the paper
 * register. Deliberately a separate row and nothing else: it never alters, hides or gates any
 * invoice, payment or expense, and nothing is diffed automatically - it only records that a
 * person looked.
 */
@Entity(
    tableName = "reconciliation_marks",
    foreignKeys = [
        ForeignKey(
            entity = Hostel::class,
            parentColumns = ["id"],
            childColumns = ["hostelId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["hostelId", "periodMonth", "periodYear"], unique = true)],
)
data class ReconciliationMark(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val hostelId: String,
    val periodMonth: Int,
    val periodYear: Int,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
