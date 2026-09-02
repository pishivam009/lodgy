package com.lodgy.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "rooms",
    foreignKeys = [
        ForeignKey(
            entity = Floor::class,
            parentColumns = ["id"],
            childColumns = ["floorId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("floorId")],
)
data class Room(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val floorId: String,
    val roomNumber: String,
    val type: RoomType,
    val pricePerBed: Double,
    val amenities: String,
    val createdAt: Long,
    val updatedAt: Long,
)
