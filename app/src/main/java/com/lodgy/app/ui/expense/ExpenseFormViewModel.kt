package com.lodgy.app.ui.expense

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.entity.Expense
import com.lodgy.app.data.entity.ExpenseCategory
import com.lodgy.app.data.prefs.HostelPreferences
import com.lodgy.app.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExpenseFormUiState(
    val isEditing: Boolean = false,
    val category: ExpenseCategory = ExpenseCategory.WIFI,
    val amount: String = "",
    val incurredOnMillis: Long = System.currentTimeMillis(),
    val isRecurring: Boolean = false,
    val note: String = "",
    val saved: Boolean = false,
) {
    val canSave: Boolean get() = amount.toDoubleOrNull() != null
}

@HiltViewModel
class ExpenseFormViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val hostelPreferences: HostelPreferences,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val expenseId: String? = savedStateHandle["expenseId"]
    private var existingExpense: Expense? = null

    private val _uiState = MutableStateFlow(ExpenseFormUiState(isEditing = expenseId != null))
    val uiState: StateFlow<ExpenseFormUiState> = _uiState.asStateFlow()

    init {
        val id = expenseId
        if (id != null) {
            viewModelScope.launch {
                val expense = expenseRepository.getById(id) ?: return@launch
                existingExpense = expense
                _uiState.update {
                    it.copy(
                        category = expense.category,
                        amount = expense.amount.toString(),
                        incurredOnMillis = expense.incurredOn,
                        isRecurring = expense.isRecurring,
                        note = expense.note.orEmpty(),
                    )
                }
            }
        }
    }

    fun onCategoryChange(value: ExpenseCategory) = _uiState.update { it.copy(category = value) }
    fun onAmountChange(value: String) = _uiState.update { it.copy(amount = value) }
    fun onIncurredOnChange(millis: Long) = _uiState.update { it.copy(incurredOnMillis = millis) }
    fun onRecurringToggle(value: Boolean) = _uiState.update { it.copy(isRecurring = value) }
    fun onNoteChange(value: String) = _uiState.update { it.copy(note = value) }

    fun save() {
        val state = _uiState.value
        val amount = state.amount.toDoubleOrNull() ?: return
        viewModelScope.launch {
            val existing = existingExpense
            if (existing != null) {
                expenseRepository.update(existing, state.category, amount, state.isRecurring, state.incurredOnMillis, state.note.ifBlank { null })
            } else {
                val hostelId = hostelPreferences.selectedHostelId.first() ?: return@launch
                expenseRepository.create(hostelId, state.category, amount, state.isRecurring, state.incurredOnMillis, state.note.ifBlank { null })
            }
            _uiState.update { it.copy(saved = true) }
        }
    }
}
