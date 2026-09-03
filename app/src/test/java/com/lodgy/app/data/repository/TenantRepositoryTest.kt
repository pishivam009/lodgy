package com.lodgy.app.data.repository

import com.lodgy.app.data.dao.TenantDao
import com.lodgy.app.data.entity.Tenant
import com.lodgy.app.data.entity.TenantStatus
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TenantRepositoryTest {

    private val dao: TenantDao = mockk()
    private val repository = TenantRepository(dao)

    @Test
    fun `create persists a new tenant as ACTIVE`() = runTest {
        val inserted = slot<Tenant>()
        coEvery { dao.insert(capture(inserted)) } returns Unit

        val created = repository.create("Ravi", "999", null, null, "Sita", "888")

        assertEquals(TenantStatus.ACTIVE, created.status)
        assertEquals("Ravi", created.name)
        assertEquals(created, inserted.captured)
    }

    @Test
    fun `update replaces the mutable fields`() = runTest {
        val existing = Tenant(id = "t1", name = "Old", phone = "1", photoPath = null, idProofPhotoPath = null, emergencyContactName = "e", emergencyContactPhone = "2", status = TenantStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)
        val updated = slot<Tenant>()
        coEvery { dao.update(capture(updated)) } returns Unit

        repository.update(existing, "New", "3", "p.jpg", "id.jpg", "e2", "4")

        assertEquals("New", updated.captured.name)
        assertEquals("3", updated.captured.phone)
        assertEquals("p.jpg", updated.captured.photoPath)
        assertEquals(TenantStatus.ACTIVE, updated.captured.status)
    }

    @Test
    fun `setVacated marks the tenant VACATED without touching other fields`() = runTest {
        val existing = Tenant(id = "t1", name = "Ravi", phone = "1", photoPath = null, idProofPhotoPath = null, emergencyContactName = "e", emergencyContactPhone = "2", status = TenantStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)
        val updated = slot<Tenant>()
        coEvery { dao.update(capture(updated)) } returns Unit

        repository.setVacated(existing)

        assertEquals(TenantStatus.VACATED, updated.captured.status)
        assertEquals("Ravi", updated.captured.name)
    }
}
