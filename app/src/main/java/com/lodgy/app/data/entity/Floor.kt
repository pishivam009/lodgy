package com.lodgy.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "floors",
    foreignKeys = [
        ForeignKey(
            entity = Hostel::class,
            parentColumns = ["id"],
            childColumns = ["hostelId"],
        ),
    ],
    indices = [Index("hostelId")],
)
data class Floor(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val hostelId: String,
    val label: String,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
)
