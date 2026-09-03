package com.lodgy.app.ui.note

import androidx.lifecycle.SavedStateHandle
import com.lodgy.app.data.entity.NoteType
import com.lodgy.app.data.entity.TenantNote
import com.lodgy.app.data.repository.TenantNoteRepository
import com.lodgy.app.testutil.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class NotesTimelineViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val tenantNoteRepository: TenantNoteRepository = mockk()

    @Test
    fun `loads the tenant's notes`() {
        val note = TenantNote(id = "n1", tenantId = "t1", type = NoteType.GENERAL, text = "hi", photoPath = null, occurredOn = 0L, createdAt = 0L, updatedAt = 0L)
        every { tenantNoteRepository.getByTenantId("t1") } returns flowOf(listOf(note))

        val viewModel = NotesTimelineViewModel(tenantNoteRepository, SavedStateHandle(mapOf("tenantId" to "t1")))

        assertFalse(viewModel.uiState.value.loading)
        assertEquals(listOf(note), viewModel.uiState.value.notes)
        assertEquals("t1", viewModel.tenantId)
    }
}
