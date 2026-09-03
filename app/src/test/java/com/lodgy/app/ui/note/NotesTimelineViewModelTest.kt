package com.lodgy.app.ui.note

import androidx.lifecycle.SavedStateHandle
import com.lodgy.app.data.entity.Credit
import com.lodgy.app.data.entity.NoteType
import com.lodgy.app.data.entity.TenantNote
import com.lodgy.app.data.repository.CreditRepository
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
    private val creditRepository: CreditRepository = mockk()

    @Test
    fun `loads the tenant's notes`() {
        val note = TenantNote(id = "n1", tenantId = "t1", type = NoteType.GENERAL, text = "hi", photoPath = null, occurredOn = 0L, createdAt = 0L, updatedAt = 0L)
        every { tenantNoteRepository.getByTenantId("t1") } returns flowOf(listOf(note))

        every { creditRepository.getByTenantId("t1") } returns flowOf(emptyList())

        val viewModel = NotesTimelineViewModel(tenantNoteRepository, creditRepository, SavedStateHandle(mapOf("tenantId" to "t1")))

        assertFalse(viewModel.uiState.value.loading)
        assertEquals(listOf(note), viewModel.uiState.value.notes)
        assertEquals("t1", viewModel.tenantId)
    }

    @Test
    fun `credits appear in the timeline interleaved with notes by date`() {
        val olderNote = TenantNote(id = "n1", tenantId = "t1", type = NoteType.GENERAL, text = "hi", photoPath = null, occurredOn = 100L, createdAt = 0L, updatedAt = 0L)
        val newerNote = TenantNote(id = "n2", tenantId = "t1", type = NoteType.GENERAL, text = "later", photoPath = null, occurredOn = 300L, createdAt = 0L, updatedAt = 0L)
        val credit = Credit(id = "c1", tenantId = "t1", invoiceId = null, amount = 500.0, reason = "Plumbing", createdAt = 200L, updatedAt = 200L)
        every { tenantNoteRepository.getByTenantId("t1") } returns flowOf(listOf(olderNote, newerNote))
        every { creditRepository.getByTenantId("t1") } returns flowOf(listOf(credit))

        val entries = NotesTimelineViewModel(tenantNoteRepository, creditRepository, SavedStateHandle(mapOf("tenantId" to "t1"))).uiState.value.entries

        assertEquals(listOf(300L, 200L, 100L), entries.map { it.occurredOn })
        assertEquals(credit, (entries[1] as TimelineEntry.CreditEntry).credit)
    }
}
