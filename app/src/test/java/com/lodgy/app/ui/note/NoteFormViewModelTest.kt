package com.lodgy.app.ui.note

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.lodgy.app.data.entity.NoteType
import com.lodgy.app.data.entity.TenantNote
import com.lodgy.app.data.repository.TenantNoteRepository
import com.lodgy.app.media.PhotoStorage
import com.lodgy.app.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NoteFormViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val tenantNoteRepository: TenantNoteRepository = mockk()
    private val photoStorage: PhotoStorage = mockk()

    private fun viewModel(noteId: String? = null) = NoteFormViewModel(
        tenantNoteRepository, photoStorage,
        SavedStateHandle(mapOf<String, Any?>("tenantId" to "t1", "noteId" to noteId).filterValues { it != null }),
    )

    @Test
    fun `editing an existing note preloads its fields`() {
        val note = TenantNote(id = "n1", tenantId = "t1", type = NoteType.DAMAGE, text = "broke window", photoPath = "p.jpg", occurredOn = 999L, createdAt = 0L, updatedAt = 0L)
        coEvery { tenantNoteRepository.getById("n1") } returns note

        val state = viewModel("n1").uiState.value

        assertTrue(state.isEditing)
        assertEquals(NoteType.DAMAGE, state.type)
        assertEquals("broke window", state.text)
        assertEquals(999L, state.occurredOnMillis)
    }

    @Test
    fun `canSave requires non-blank text`() {
        val viewModel = viewModel()
        assertFalse(viewModel.uiState.value.canSave)
        viewModel.onTextChange("   ")
        assertFalse(viewModel.uiState.value.canSave)
        viewModel.onTextChange("Loud music")
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun `onPhotoPicked persists the photo and stores its path`() {
        val uri: Uri = mockk()
        coEvery { photoStorage.persist(uri) } returns "/data/n.jpg"

        val viewModel = viewModel()
        viewModel.onPhotoPicked(uri)

        assertEquals("/data/n.jpg", viewModel.uiState.value.photoPath)
    }

    @Test
    fun `save creates a new note for the tenant`() {
        coEvery { tenantNoteRepository.create("t1", NoteType.GENERAL, "Loud music", null, any()) } returns mockk()

        val viewModel = viewModel()
        viewModel.onTextChange("Loud music")
        viewModel.save()

        coVerify { tenantNoteRepository.create("t1", NoteType.GENERAL, "Loud music", null, any()) }
        assertTrue(viewModel.uiState.value.saved)
    }

    @Test
    fun `save updates an existing note instead of creating a new one`() {
        val note = TenantNote(id = "n1", tenantId = "t1", type = NoteType.DAMAGE, text = "old", photoPath = null, occurredOn = 0L, createdAt = 0L, updatedAt = 0L)
        coEvery { tenantNoteRepository.getById("n1") } returns note
        coEvery { tenantNoteRepository.update(note, NoteType.DAMAGE, "fixed text", null, 0L) } returns Unit

        val viewModel = viewModel("n1")
        viewModel.onTextChange("fixed text")
        viewModel.save()

        coVerify { tenantNoteRepository.update(note, NoteType.DAMAGE, "fixed text", null, 0L) }
        coVerify(exactly = 0) { tenantNoteRepository.create(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `save does nothing when text is blank`() {
        val viewModel = viewModel()

        viewModel.save()

        coVerify(exactly = 0) { tenantNoteRepository.create(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `delete removes an existing note`() {
        val note = TenantNote(id = "n1", tenantId = "t1", type = NoteType.GENERAL, text = "x", photoPath = null, occurredOn = 0L, createdAt = 0L, updatedAt = 0L)
        coEvery { tenantNoteRepository.getById("n1") } returns note
        coEvery { tenantNoteRepository.delete(note) } returns Unit

        val viewModel = viewModel("n1")
        viewModel.delete()

        coVerify { tenantNoteRepository.delete(note) }
        assertTrue(viewModel.uiState.value.deleted)
    }

    @Test
    fun `delete on a note that never loaded does nothing`() {
        val viewModel = viewModel()

        viewModel.delete()

        coVerify(exactly = 0) { tenantNoteRepository.delete(any()) }
    }
}
