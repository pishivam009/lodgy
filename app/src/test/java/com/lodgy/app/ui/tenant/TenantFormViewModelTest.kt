package com.lodgy.app.ui.tenant

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.lodgy.app.data.entity.Tenant
import com.lodgy.app.data.entity.TenantStatus
import com.lodgy.app.data.repository.TenantRepository
import com.lodgy.app.media.PhotoStorage
import com.lodgy.app.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TenantFormViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val tenantRepository: TenantRepository = mockk()
    private val photoStorage: PhotoStorage = mockk()

    private fun viewModel(tenantId: String? = null) = TenantFormViewModel(
        tenantRepository, photoStorage,
        SavedStateHandle(mapOf<String, Any?>("tenantId" to tenantId).filterValues { it != null }),
    )

    private fun tenant() = Tenant(id = "t1", name = "Ravi", phone = "999", photoPath = "old.jpg", idProofPhotoPath = "id.jpg", emergencyContactName = "Sita", emergencyContactPhone = "888", status = TenantStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)

    @Test
    fun `editing an existing tenant preloads its fields`() {
        coEvery { tenantRepository.getById("t1") } returns tenant()

        val state = viewModel("t1").uiState.value

        assertTrue(state.isEditing)
        assertEquals("Ravi", state.name)
        assertEquals("old.jpg", state.photoPath)
    }

    @Test
    fun `canSave requires both a name and a phone number`() {
        val viewModel = viewModel()
        assertFalse(viewModel.uiState.value.canSave)
        viewModel.onNameChange("Ravi")
        assertFalse(viewModel.uiState.value.canSave)
        viewModel.onPhoneChange("999")
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun `onPhotoPicked persists the photo and stores its path under the right field`() {
        val uri: Uri = mockk()
        coEvery { photoStorage.persist(uri) } returns "/data/photos/new.jpg"

        val viewModel = viewModel()
        viewModel.onPhotoPicked(PhotoField.PROFILE, uri)
        assertEquals("/data/photos/new.jpg", viewModel.uiState.value.photoPath)

        viewModel.onPhotoPicked(PhotoField.ID_PROOF, uri)
        assertEquals("/data/photos/new.jpg", viewModel.uiState.value.idProofPhotoPath)
    }

    @Test
    fun `createCameraOutputUri delegates to PhotoStorage`() {
        val uri: Uri = mockk()
        every { photoStorage.createCameraOutputUri() } returns uri

        assertEquals(uri, viewModel().createCameraOutputUri())
    }

    @Test
    fun `save creates a new tenant and records its id`() {
        val created = tenant()
        coEvery { tenantRepository.create("Ravi", "999", null, null, "", "") } returns created

        val viewModel = viewModel()
        viewModel.onNameChange("Ravi")
        viewModel.onPhoneChange("999")
        viewModel.save()

        assertTrue(viewModel.uiState.value.saved)
        assertEquals("t1", viewModel.uiState.value.savedTenantId)
    }

    @Test
    fun `save updates an existing tenant instead of creating a new one`() {
        val existing = tenant()
        coEvery { tenantRepository.getById("t1") } returns existing
        coEvery { tenantRepository.update(existing, "Ravi Kumar", "999", "old.jpg", "id.jpg", "Sita", "888") } returns Unit

        val viewModel = viewModel("t1")
        viewModel.onNameChange("Ravi Kumar")
        viewModel.save()

        coVerify { tenantRepository.update(existing, "Ravi Kumar", "999", "old.jpg", "id.jpg", "Sita", "888") }
        assertEquals("t1", viewModel.uiState.value.savedTenantId)
    }

    @Test
    fun `save does nothing while required fields are missing`() {
        val viewModel = viewModel()
        viewModel.onNameChange("Ravi")

        viewModel.save()

        coVerify(exactly = 0) { tenantRepository.create(any(), any(), any(), any(), any(), any()) }
    }
}
