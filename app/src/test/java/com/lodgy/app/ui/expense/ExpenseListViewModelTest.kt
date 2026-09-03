package com.lodgy.app.ui.expense

import com.lodgy.app.data.entity.Expense
import com.lodgy.app.data.entity.ExpenseCategory
import com.lodgy.app.data.entity.Hostel
import com.lodgy.app.data.prefs.HostelPreferences
import com.lodgy.app.data.repository.ExpenseRepository
import com.lodgy.app.data.repository.HostelRepository
import com.lodgy.app.testutil.MainDispatcherRule
import io.mockk.coEvery
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

    private fun viewModel() = ExpenseListViewModel(hostelPreferences, hostelRepository, expenseRepository)

    @Test
    fun `no active hostel reports hasActiveHostel false`() {
        every { hostelPreferences.selectedHostelId } returns flowOf(null)

        val state = viewModel().uiState.value

        assertFalse(state.loading)
        assertFalse(state.hasActiveHostel)
    }

    @Test
    fun `loads the hostel name and its expenses, exposing their total`() {
        every { hostelPreferences.selectedHostelId } returns flowOf("h1")
        coEvery { hostelRepository.getById("h1") } returns Hostel(id = "h1", wardenId = "w1", name = "Sunrise", address = "", contactPhone = "", createdAt = 0L, updatedAt = 0L)
        val expenses = listOf(
            Expense(id = "e1", hostelId = "h1", category = ExpenseCategory.WIFI, amount = 500.0, isRecurring = true, incurredOn = 0L, note = null, createdAt = 0L, updatedAt = 0L),
            Expense(id = "e2", hostelId = "h1", category = ExpenseCategory.WATER, amount = 300.0, isRecurring = false, incurredOn = 0L, note = null, createdAt = 0L, updatedAt = 0L),
        )
        every { expenseRepository.getByHostelId("h1") } returns flowOf(expenses)

        val state = viewModel().uiState.value

        assertTrue(state.hasActiveHostel)
        assertEquals("Sunrise", state.hostelName)
        assertEquals("h1", state.hostelId)
        assertEquals(800.0, state.total, 0.0001)
    }

    @Test
    fun `the category filter narrows the list and the total follows it`() {
        every { hostelPreferences.selectedHostelId } returns flowOf("h1")
        coEvery { hostelRepository.getById("h1") } returns Hostel(id = "h1", wardenId = "w1", name = "Sunrise", address = "", contactPhone = "", createdAt = 0L, updatedAt = 0L)
        every { expenseRepository.getByHostelId("h1") } returns flowOf(
            listOf(
                Expense(id = "e1", hostelId = "h1", category = ExpenseCategory.WIFI, amount = 500.0, isRecurring = true, incurredOn = 0L, note = null, createdAt = 0L, updatedAt = 0L),
                Expense(id = "e2", hostelId = "h1", category = ExpenseCategory.WATER, amount = 300.0, isRecurring = false, incurredOn = 0L, note = null, createdAt = 0L, updatedAt = 0L),
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
        coEvery { hostelRepository.getById("h1") } returns Hostel(id = "h1", wardenId = "w1", name = "Sunrise", address = "", contactPhone = "", createdAt = 0L, updatedAt = 0L)
        every { expenseRepository.getByHostelId("h1") } returns flowOf(
            listOf(
                Expense(id = "old-big", hostelId = "h1", category = ExpenseCategory.WIFI, amount = 900.0, isRecurring = false, incurredOn = 100L, note = null, createdAt = 0L, updatedAt = 0L),
                Expense(id = "new-small", hostelId = "h1", category = ExpenseCategory.WATER, amount = 100.0, isRecurring = false, incurredOn = 900L, note = null, createdAt = 0L, updatedAt = 0L),
            ),
        )

        val viewModel = viewModel()

        assertEquals(listOf("new-small", "old-big"), viewModel.uiState.value.visibleExpenses.map { it.id })

        viewModel.onSortChange(ExpenseSort.AMOUNT)
        assertEquals(listOf("old-big", "new-small"), viewModel.uiState.value.visibleExpenses.map { it.id })
    }
}
