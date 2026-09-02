package com.lodgy.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "wardens")
data class Warden(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val pinHash: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
)
