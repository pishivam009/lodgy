package com.lodgy.app.ui.expense

import com.lodgy.app.data.entity.Expense
import com.lodgy.app.data.entity.ExpenseCategory
import com.lodgy.app.data.entity.Hostel
import com.lodgy.app.data.prefs.HostelPreferences
import com.lodgy.app.data.repository.ExpenseRepository
import com.lodgy.app.data.repository.HostelRepository
import com.lodgy.app.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ExpenseListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val hostelPreferences: HostelPreferences = mockk()
    private val hostelRepository: HostelRepository = mockk()
    private val expenseRepository: ExpenseRepository = mockk()

    private fun hostel(id: String, name: String) =
        Hostel(id = id, wardenId = "w1", name = name, address = "", contactPhone = "", createdAt = 0L, updatedAt = 0L)

    private fun expense(id: String, hostelId: String, category: ExpenseCategory, amount: Double, incurredOn: Long = 0L) =
        Expense(id = id, hostelId = hostelId, category = category, amount = amount, isRecurring = false, incurredOn = incurredOn, note = null, createdAt = 0L, updatedAt = 0L)

    private fun viewModel(): ExpenseListViewModel {
        coEvery { hostelPreferences.setSelectedHostelId(any()) } returns Unit
        return ExpenseListViewModel(hostelPreferences, hostelRepository, expenseRepository)
    }

    @Test
    fun `no hostels at all reports hasActiveHostel false`() {
        every { hostelPreferences.selectedHostelId } returns flowOf(null)
        every { hostelRepository.getAll() } returns flowOf(emptyList())
        every { expenseRepository.observeAll() } returns flowOf(emptyList())

        val state = viewModel().uiState.value

        assertFalse(state.loading)
        assertFalse(state.hasActiveHostel)
    }

    @Test
    fun `defaults to the app's selected hostel, matching the Monthly Report`() {
        every { hostelPreferences.selectedHostelId } returns flowOf("h1")
        every { hostelRepository.getAll() } returns flowOf(listOf(hostel("h1", "Sunrise"), hostel("h2", "Moonlight")))
        every { expenseRepository.observeAll() } returns flowOf(
            listOf(
                expense("e1", "h1", ExpenseCategory.WIFI, 500.0),
                expense("e2", "h2", ExpenseCategory.WATER, 300.0),
            ),
        )

        val state = viewModel().uiState.value

        assertTrue(state.hasActiveHostel)
        assertEquals("h1", state.filterHostelId)
        assertEquals(listOf("e1"), state.visibleExpenses.map { it.id })
        assertEquals(500.0, state.total, 0.0001)
    }

    @Test
    fun `switching to a specific property narrows the list and writes back to the preference`() {
        every { hostelPreferences.selectedHostelId } returns flowOf("h1")
        every { hostelRepository.getAll() } returns flowOf(listOf(hostel("h1", "Sunrise"), hostel("h2", "Moonlight")))
        every { expenseRepository.observeAll() } returns flowOf(
            listOf(
                expense("e1", "h1", ExpenseCategory.WIFI, 500.0),
                expense("e2", "h2", ExpenseCategory.WATER, 300.0),
            ),
        )

        val viewModel = viewModel()
        viewModel.onHostelFilterChange("h2")

        assertEquals(listOf("e2"), viewModel.uiState.value.visibleExpenses.map { it.id })
        assertEquals(300.0, viewModel.uiState.value.total, 0.0001)
        assertEquals("Moonlight", viewModel.uiState.value.filterHostelName)
        coVerify { hostelPreferences.setSelectedHostelId("h2") }
    }

    @Test
    fun `switching to All shows every property's expenses without writing back to the preference`() {
        every { hostelPreferences.selectedHostelId } returns flowOf("h1")
        every { hostelRepository.getAll() } returns flowOf(listOf(hostel("h1", "Sunrise"), hostel("h2", "Moonlight")))
        every { expenseRepository.observeAll() } returns flowOf(
            listOf(
                expense("e1", "h1", ExpenseCategory.WIFI, 500.0),
                expense("e2", "h2", ExpenseCategory.WATER, 300.0),
            ),
        )

        val viewModel = viewModel()
        // The initial seed (h1) writes back once; picking All afterwards must not write again.
        coVerify(exactly = 1) { hostelPreferences.setSelectedHostelId(any()) }

        viewModel.onHostelFilterChange(null)

        assertEquals(null, viewModel.uiState.value.filterHostelId)
        assertEquals(2, viewModel.uiState.value.visibleExpenses.size)
        assertEquals(800.0, viewModel.uiState.value.total, 0.0001)
        coVerify(exactly = 1) { hostelPreferences.setSelectedHostelId(any()) }
    }

    @Test
    fun `the category filter narrows the list and the total follows it`() {
        every { hostelPreferences.selectedHostelId } returns flowOf("h1")
        every { hostelRepository.getAll() } returns flowOf(listOf(hostel("h1", "Sunrise")))
        every { expenseRepository.observeAll() } returns flowOf(
            listOf(
                expense("e1", "h1", ExpenseCategory.WIFI, 500.0),
                expense("e2", "h1", ExpenseCategory.WATER, 300.0),
            ),
        )

        val viewModel = viewModel()
        assertEquals(800.0, viewModel.uiState.value.total, 0.0001)

        viewModel.onCategoryChange(ExpenseCategory.WIFI)

        assertEquals(listOf("e1"), viewModel.uiState.value.visibleExpenses.map { it.id })
        assertEquals(500.0, viewModel.uiState.value.total, 0.0001)

        viewModel.onCategoryChange(null)
        assertEquals(2, viewModel.uiState.value.visibleExpenses.size)
    }

    @Test
    fun `sorting switches between most recent and largest`() {
        every { hostelPreferences.selectedHostelId } returns flowOf("h1")
        every { hostelRepository.getAll() } returns flowOf(listOf(hostel("h1", "Sunrise")))
        every { expenseRepository.observeAll() } returns flowOf(
            listOf(
                expense("old-big", "h1", ExpenseCategory.WIFI, 900.0, incurredOn = 100L),
                expense("new-small", "h1", ExpenseCategory.WATER, 100.0, incurredOn = 900L),
            ),
        )

        val viewModel = viewModel()

        assertEquals(listOf("new-small", "old-big"), viewModel.uiState.value.visibleExpenses.map { it.id })

        viewModel.onSortChange(ExpenseSort.AMOUNT)
        assertEquals(listOf("old-big", "new-small"), viewModel.uiState.value.visibleExpenses.map { it.id })
    }
}
