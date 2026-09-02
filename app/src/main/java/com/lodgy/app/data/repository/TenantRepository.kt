package com.lodgy.app.data.repository

import com.lodgy.app.data.dao.TenantDao
import com.lodgy.app.data.entity.Tenant
import com.lodgy.app.data.entity.TenantStatus
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class TenantRepository @Inject constructor(private val tenantDao: TenantDao) {
    fun getAll(): Flow<List<Tenant>> = tenantDao.getAll()

    suspend fun getById(id: String): Tenant? = tenantDao.getById(id)

    fun observeById(id: String): Flow<Tenant?> = tenantDao.getByIdFlow(id)

    suspend fun create(
        name: String,
        phone: String,
        photoPath: String?,
        idProofPhotoPath: String?,
        emergencyContactName: String,
        emergencyContactPhone: String,
    ): Tenant {
        val now = System.currentTimeMillis()
        val tenant = Tenant(
            name = name,
            phone = phone,
            photoPath = photoPath,
            idProofPhotoPath = idProofPhotoPath,
            emergencyContactName = emergencyContactName,
            emergencyContactPhone = emergencyContactPhone,
            status = TenantStatus.ACTIVE,
            createdAt = now,
            updatedAt = now,
        )
        tenantDao.insert(tenant)
        return tenant
    }

    suspend fun update(
        tenant: Tenant,
        name: String,
        phone: String,
        photoPath: String?,
        idProofPhotoPath: String?,
        emergencyContactName: String,
        emergencyContactPhone: String,
    ) {
        tenantDao.update(
            tenant.copy(
                name = name,
                phone = phone,
                photoPath = photoPath,
                idProofPhotoPath = idProofPhotoPath,
                emergencyContactName = emergencyContactName,
                emergencyContactPhone = emergencyContactPhone,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun setVacated(tenant: Tenant) {
        tenantDao.update(tenant.copy(status = TenantStatus.VACATED, updatedAt = System.currentTimeMillis()))
    }
}
