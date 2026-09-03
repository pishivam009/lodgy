package com.lodgy.app.media

import android.content.Context
import com.lodgy.app.data.entity.Tenant
import com.lodgy.app.data.entity.TenantStatus
import com.lodgy.app.data.repository.TenantRepository
import io.mockk.every
import io.mockk.mockk
import java.io.File
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class OrphanPhotoCleanerTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val context: Context = mockk(relaxed = true)
    private val tenantRepository: TenantRepository = mockk()

    private fun tenant(
        id: String,
        photoPath: String?,
        idProofPhotoPath: String? = null,
        status: TenantStatus = TenantStatus.ACTIVE,
    ) = Tenant(
        id = id,
        name = id,
        phone = "1",
        photoPath = photoPath,
        idProofPhotoPath = idProofPhotoPath,
        emergencyContactName = "",
        emergencyContactPhone = "",
        status = status,
        createdAt = 0L,
        updatedAt = 0L,
    )

    private fun cleaner(tenants: List<Tenant>): OrphanPhotoCleaner {
        every { tenantRepository.getAll() } returns flowOf(tenants)
        return OrphanPhotoCleaner(context, tenantRepository)
    }

    private fun photo(dir: File, name: String) = File(dir, name).apply { writeText("jpeg") }

    @Test
    fun `deletes files no tenant references and keeps the ones that are referenced`() = runTest {
        val dir = temporaryFolder.newFolder("photos")
        val kept = photo(dir, "kept.jpg")
        val orphan = photo(dir, "orphan.jpg")

        val removed = cleaner(listOf(tenant("t1", photoPath = kept.absolutePath))).cleanIn(dir)

        assertEquals(1, removed)
        assertTrue(kept.exists())
        assertFalse(orphan.exists())
    }

    @Test
    fun `an id proof photo counts as a reference just as much as a profile photo`() = runTest {
        val dir = temporaryFolder.newFolder("photos")
        val idProof = photo(dir, "id-proof.jpg")

        val removed = cleaner(listOf(tenant("t1", photoPath = null, idProofPhotoPath = idProof.absolutePath))).cleanIn(dir)

        assertEquals(0, removed)
        assertTrue(idProof.exists())
    }

    @Test
    fun `a vacated tenant's photos are never swept`() = runTest {
        val dir = temporaryFolder.newFolder("photos")
        val kept = photo(dir, "vacated.jpg")

        val removed = cleaner(
            listOf(tenant("t1", photoPath = kept.absolutePath, status = TenantStatus.VACATED)),
        ).cleanIn(dir)

        assertEquals(0, removed)
        assertTrue(kept.exists())
    }

    @Test
    fun `photos restored under a different filesDir path are matched by name, not deleted`() = runTest {
        val dir = temporaryFolder.newFolder("photos")
        val restored = photo(dir, "abc-123.jpg")

        val removed = cleaner(
            listOf(tenant("t1", photoPath = "/data/user/0/com.lodgy.app/files/photos/abc-123.jpg")),
        ).cleanIn(dir)

        assertEquals(0, removed)
        assertTrue(restored.exists())
    }

    @Test
    fun `an empty or missing photos directory is a no-op`() = runTest {
        assertEquals(0, cleaner(emptyList()).cleanIn(temporaryFolder.newFolder("photos")))
        assertEquals(0, cleaner(emptyList()).cleanIn(File(temporaryFolder.root, "never-created")))
    }

    @Test
    fun `with no tenants at all every photo is an orphan`() = runTest {
        val dir = temporaryFolder.newFolder("photos")
        photo(dir, "a.jpg")
        photo(dir, "b.jpg")

        assertEquals(2, cleaner(emptyList()).cleanIn(dir))
        assertEquals(0, dir.listFiles()!!.size)
    }
}
