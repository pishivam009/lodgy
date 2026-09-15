package com.lodgy.app.ui.note

import androidx.lifecycle.SavedStateHandle
import com.lodgy.app.data.dao.TenantStayRow
import com.lodgy.app.data.entity.Credit
import com.lodgy.app.data.entity.NoteType
import com.lodgy.app.data.entity.PropertyType
import com.lodgy.app.data.entity.TenantNote
import com.lodgy.app.data.repository.CreditRepository
import com.lodgy.app.data.repository.OccupancyPeriodRepository
import com.lodgy.app.data.repository.TenantNoteRepository
import com.lodgy.app.testutil.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NotesTimelineViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val tenantNoteRepository: TenantNoteRepository = mockk()
    private val creditRepository: CreditRepository = mockk()
    private val occupancyPeriodRepository: OccupancyPeriodRepository = mockk()

    private fun stay(periodId: String, agreementId: String?, start: Long, end: Long?) = TenantStayRow(
        periodId = periodId, tenancyAgreementId = agreementId, bedLabel = "A", roomNumber = "101",
        hostelName = "Sunrise Hostel", propertyType = PropertyType.HOSTEL, startDate = start, endDate = end,
    )

    private fun viewModel(
        notes: List<TenantNote> = emptyList(),
        credits: List<Credit> = emptyList(),
        stays: List<TenantStayRow> = emptyList(),
    ): NotesTimelineViewModel {
        every { tenantNoteRepository.getByTenantId("t1") } returns flowOf(notes)
        every { creditRepository.getByTenantId("t1") } returns flowOf(credits)
        every { occupancyPeriodRepository.observeStaysByTenantId("t1") } returns flowOf(stays)
        return NotesTimelineViewModel(
            tenantNoteRepository, creditRepository, occupancyPeriodRepository,
            SavedStateHandle(mapOf("tenantId" to "t1")),
        )
    }

    @Test
    fun `loads the tenant's notes`() {
        val note = TenantNote(id = "n1", tenantId = "t1", type = NoteType.GENERAL, text = "hi", photoPath = null, occurredOn = 0L, createdAt = 0L, updatedAt = 0L)

        val viewModel = viewModel(notes = listOf(note))

        assertFalse(viewModel.uiState.value.loading)
        assertEquals(listOf(note), viewModel.uiState.value.notes)
        assertEquals("t1", viewModel.tenantId)
    }

    @Test
    fun `credits appear in the timeline interleaved with notes by date`() {
        val olderNote = TenantNote(id = "n1", tenantId = "t1", type = NoteType.GENERAL, text = "hi", photoPath = null, occurredOn = 100L, createdAt = 0L, updatedAt = 0L)
        val newerNote = TenantNote(id = "n2", tenantId = "t1", type = NoteType.GENERAL, text = "later", photoPath = null, occurredOn = 300L, createdAt = 0L, updatedAt = 0L)
        val credit = Credit(id = "c1", tenantId = "t1", invoiceId = null, amount = 500.0, reason = "Plumbing", createdAt = 200L, updatedAt = 200L)

        val entries = viewModel(notes = listOf(olderNote, newerNote), credits = listOf(credit)).uiState.value.entries

        assertEquals(listOf(300L, 200L, 100L), entries.map { it.occurredOn })
        assertEquals(credit, (entries[1] as TimelineEntry.CreditEntry).credit)
    }

    @Test
    fun `a single ever-recorded period does not show as a room history section`() {
        val vm = viewModel(stays = listOf(stay("p1", "a1", 100L, null)))

        assertFalse(vm.uiState.value.showStays)
    }

    @Test
    fun `no periods at all does not show a room history section either`() {
        val vm = viewModel()

        assertFalse(vm.uiState.value.showStays)
    }

    @Test
    fun `a transfer within one tenancy groups both periods under the same stay`() {
        val vm = viewModel(
            stays = listOf(
                stay("p1", "a1", 100L, 200L),
                stay("p2", "a1", 200L, null),
            ),
        )

        val state = vm.uiState.value
        assertTrue(state.showStays)
        assertEquals(1, state.stayGroups.size)
        assertEquals(listOf("p1", "p2"), state.stayGroups.single().periods.map { it.periodId })
    }

    @Test
    fun `a returning tenant's two tenancies show as two separate stays, most recent first`() {
        val vm = viewModel(
            stays = listOf(
                stay("p1", "a1", 100L, 200L),
                stay("p2", "a2", 500L, null),
            ),
        )

        val groups = vm.uiState.value.stayGroups
        assertEquals(2, groups.size)
        assertEquals("a2", groups[0].tenancyAgreementId)
        assertEquals("a1", groups[1].tenancyAgreementId)
    }

    @Test
    fun `backfilled periods with no agreement each stand as their own stay`() {
        val vm = viewModel(
            stays = listOf(
                stay("p1", null, 100L, 200L),
                stay("p2", null, 300L, 400L),
            ),
        )

        val groups = vm.uiState.value.stayGroups
        assertEquals(2, groups.size)
        assertTrue(groups.all { it.tenancyAgreementId == null })
    }
}
