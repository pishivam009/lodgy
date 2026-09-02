package com.lodgy.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "beds",
    foreignKeys = [
        ForeignKey(
            entity = Room::class,
            parentColumns = ["id"],
            childColumns = ["roomId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("roomId")],
)
data class Bed(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val roomId: String,
    val label: String,
    val status: BedStatus,
    val createdAt: Long,
    val updatedAt: Long,
)
