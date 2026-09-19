package com.lodgy.app.ui.expense

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.entity.Expense
import com.lodgy.app.data.entity.ExpenseCategory
import com.lodgy.app.data.prefs.HostelPreferences
import com.lodgy.app.data.repository.ExpenseRepository
import com.lodgy.app.data.repository.HostelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExpenseHostelOption(val id: String, val name: String)

data class ExpenseFormUiState(
    val isEditing: Boolean = false,
    val category: ExpenseCategory = ExpenseCategory.WIFI,
    val amount: String = "",
    val incurredOnMillis: Long = System.currentTimeMillis(),
    val isRecurring: Boolean = false,
    val note: String = "",
    val hostels: List<ExpenseHostelOption> = emptyList(),
    val selectedHostelId: String? = null,
    val saved: Boolean = false,
    /** Delete a duplicate or wrong expense row (LODGY-64); confirmed first (LODGY-57). */
    val pendingDelete: Boolean = false,
    val deleted: Boolean = false,
) {
    val canSave: Boolean get() = amount.toDoubleOrNull() != null && selectedHostelId != null
}

@HiltViewModel
class ExpenseFormViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val hostelRepository: HostelRepository,
    private val hostelPreferences: HostelPreferences,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val expenseId: String? = savedStateHandle["expenseId"]
    private var existingExpense: Expense? = null

    private val _uiState = MutableStateFlow(ExpenseFormUiState(isEditing = expenseId != null))
    val uiState: StateFlow<ExpenseFormUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val hostels = hostelRepository.getAll().first()
            val hostelOptions = hostels.map { hostel -> ExpenseHostelOption(hostel.id, hostel.name) }
            val id = expenseId
            if (id != null) {
                val expense = expenseRepository.getById(id) ?: return@launch
                existingExpense = expense
                _uiState.update {
                    it.copy(
                        category = expense.category,
                        amount = expense.amount.toString(),
                        incurredOnMillis = expense.incurredOn,
                        isRecurring = expense.isRecurring,
                        note = expense.note.orEmpty(),
                        hostels = hostelOptions,
                        // The expense's own current hostel, not the app's globally selected one -
                        // editing a Sunrise expense should never default the picker to Moonlight
                        // just because that's what the warden happens to be viewing elsewhere.
                        selectedHostelId = expense.hostelId,
                    )
                }
            } else {
                // Pre-filled from the currently selected hostel rather than left blank - the
                // warden almost always wants that one - but shown and changeable, not silently
                // applied (LODGY-107): a warden who forgets to switch it before logging an
                // expense for a different property used to get no chance to notice.
                val defaultId = hostelPreferences.selectedHostelId.first() ?: hostels.firstOrNull()?.id
                _uiState.update { it.copy(hostels = hostelOptions, selectedHostelId = defaultId) }
            }
        }
    }

    fun onCategoryChange(value: ExpenseCategory) = _uiState.update { it.copy(category = value) }
    fun onAmountChange(value: String) = _uiState.update { it.copy(amount = value) }
    fun onIncurredOnChange(millis: Long) = _uiState.update { it.copy(incurredOnMillis = millis) }
    fun onRecurringToggle(value: Boolean) = _uiState.update { it.copy(isRecurring = value) }
    fun onNoteChange(value: String) = _uiState.update { it.copy(note = value) }
    fun onHostelChange(value: String) = _uiState.update { it.copy(selectedHostelId = value) }

    fun save() {
        val state = _uiState.value
        val amount = state.amount.toDoubleOrNull() ?: return
        val hostelId = state.selectedHostelId ?: return
        viewModelScope.launch {
            val existing = existingExpense
            if (existing != null) {
                expenseRepository.update(existing, hostelId, state.category, amount, state.isRecurring, state.incurredOnMillis, state.note.ifBlank { null })
            } else {
                expenseRepository.create(hostelId, state.category, amount, state.isRecurring, state.incurredOnMillis, state.note.ifBlank { null })
            }
            _uiState.update { it.copy(saved = true) }
        }
    }

    fun requestDelete() = _uiState.update { it.copy(pendingDelete = true) }
    fun dismissDelete() = _uiState.update { it.copy(pendingDelete = false) }

    fun confirmDelete() {
        val existing = existingExpense ?: return
        viewModelScope.launch {
            expenseRepository.delete(existing)
            _uiState.update { it.copy(pendingDelete = false, deleted = true) }
        }
    }
}
