package com.lodgy.app.ui.expense

import androidx.lifecycle.SavedStateHandle
import com.lodgy.app.data.entity.Expense
import com.lodgy.app.data.entity.ExpenseCategory
import com.lodgy.app.data.prefs.HostelPreferences
import com.lodgy.app.data.repository.ExpenseRepository
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

class ExpenseFormViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val expenseRepository: ExpenseRepository = mockk()
    private val hostelPreferences: HostelPreferences = mockk()

    private fun viewModel(expenseId: String? = null) =
        ExpenseFormViewModel(expenseRepository, hostelPreferences, SavedStateHandle(mapOf<String, Any?>("expenseId" to expenseId).filterValues { it != null }))

    @Test
    fun `creating a new expense starts blank and not in edit mode`() {
        val state = viewModel().uiState.value
        assertFalse(state.isEditing)
        assertEquals("", state.amount)
    }

    @Test
    fun `editing an existing expense preloads its fields`() {
        val expense = Expense(id = "e1", hostelId = "h1", category = ExpenseCategory.WATER, amount = 250.0, isRecurring = true, incurredOn = 555L, note = "tanker", createdAt = 0L, updatedAt = 0L)
        coEvery { expenseRepository.getById("e1") } returns expense

        val state = viewModel("e1").uiState.value

        assertTrue(state.isEditing)
        assertEquals(ExpenseCategory.WATER, state.category)
        assertEquals("250.0", state.amount)
        assertEquals(555L, state.incurredOnMillis)
        assertTrue(state.isRecurring)
        assertEquals("tanker", state.note)
    }

    @Test
    fun `canSave requires a numeric amount`() {
        val viewModel = viewModel()
        assertFalse(viewModel.uiState.value.canSave)
        viewModel.onAmountChange("abc")
        assertFalse(viewModel.uiState.value.canSave)
        viewModel.onAmountChange("100")
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun `saving a new expense reads the selected hostel and creates it there`() {
        every { hostelPreferences.selectedHostelId } returns flowOf("h1")
        coEvery { expenseRepository.create("h1", ExpenseCategory.WIFI, 100.0, false, any(), null) } returns mockk()

        val viewModel = viewModel()
        viewModel.onAmountChange("100")
        viewModel.save()

        coVerify { expenseRepository.create("h1", ExpenseCategory.WIFI, 100.0, false, any(), null) }
        assertTrue(viewModel.uiState.value.saved)
    }

    @Test
    fun `saving a new expense with no selected hostel does nothing`() {
        every { hostelPreferences.selectedHostelId } returns flowOf(null)

        val viewModel = viewModel()
        viewModel.onAmountChange("100")
        viewModel.save()

        coVerify(exactly = 0) { expenseRepository.create(any(), any(), any(), any(), any(), any()) }
        assertFalse(viewModel.uiState.value.saved)
    }

    @Test
    fun `saving an edited expense updates it instead of creating a new one`() {
        val expense = Expense(id = "e1", hostelId = "h1", category = ExpenseCategory.WATER, amount = 250.0, isRecurring = true, incurredOn = 555L, note = "tanker", createdAt = 0L, updatedAt = 0L)
        coEvery { expenseRepository.getById("e1") } returns expense
        coEvery { expenseRepository.update(expense, ExpenseCategory.WATER, 300.0, true, 555L, "tanker") } returns Unit

        val viewModel = viewModel("e1")
        viewModel.onAmountChange("300")
        viewModel.save()

        coVerify { expenseRepository.update(expense, ExpenseCategory.WATER, 300.0, true, 555L, "tanker") }
        coVerify(exactly = 0) { expenseRepository.create(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `save does nothing when the amount is not numeric`() {
        val viewModel = viewModel()
        viewModel.onAmountChange("nope")

        viewModel.save()

        coVerify(exactly = 0) { expenseRepository.create(any(), any(), any(), any(), any(), any()) }
    }
}
