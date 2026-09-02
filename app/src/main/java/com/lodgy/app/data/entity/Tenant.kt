package com.lodgy.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "tenants")
data class Tenant(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String,
    val photoPath: String?,
    val idProofPhotoPath: String?,
    val emergencyContactName: String,
    val emergencyContactPhone: String,
    val status: TenantStatus,
    val createdAt: Long,
    val updatedAt: Long,
)
