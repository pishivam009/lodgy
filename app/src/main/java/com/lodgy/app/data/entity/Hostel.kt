package com.lodgy.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "hostels",
    foreignKeys = [
        ForeignKey(
            entity = Warden::class,
            parentColumns = ["id"],
            childColumns = ["wardenId"],
        ),
    ],
    indices = [Index("wardenId")],
)
data class Hostel(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val wardenId: String,
    val name: String,
    val address: String,
    val contactPhone: String,
    val createdAt: Long,
    val updatedAt: Long,
)
