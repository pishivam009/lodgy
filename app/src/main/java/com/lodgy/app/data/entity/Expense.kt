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
    indices = [Index("hostelId")],
)
data class Expense(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val hostelId: String,
    val category: ExpenseCategory,
    val amount: Double,
    val isRecurring: Boolean,
    val incurredOn: Long,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
