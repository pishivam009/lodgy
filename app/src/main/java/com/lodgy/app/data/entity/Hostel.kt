package com.lodgy.app.data.entity

import androidx.room.ColumnInfo
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
    /** Decides how much of the hierarchy the warden sees. Defaults to HOSTEL so every property that
     *  existed before this behaves exactly as it did (LODGY-79). */
    @ColumnInfo(defaultValue = "HOSTEL")
    val propertyType: PropertyType = PropertyType.HOSTEL,
    val createdAt: Long,
    val updatedAt: Long,
)
