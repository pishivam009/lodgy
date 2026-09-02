package com.lodgy.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "tenant_notes",
    foreignKeys = [
        ForeignKey(
            entity = Tenant::class,
            parentColumns = ["id"],
            childColumns = ["tenantId"],
        ),
    ],
    indices = [Index("tenantId")],
)
data class TenantNote(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val tenantId: String,
    val type: NoteType,
    val text: String,
    val photoPath: String?,
    val occurredOn: Long,
    val createdAt: Long,
    val updatedAt: Long,
)
