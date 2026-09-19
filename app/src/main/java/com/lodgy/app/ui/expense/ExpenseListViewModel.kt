package com.lodgy.app.ui.expense

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

enum class ExpenseSort { DATE, AMOUNT }

data class ExpenseListUiState(
    val loading: Boolean = true,
    val hasActiveHostel: Boolean = false,
    /** Null means every property - selectable on this screen, but never the seeded default and
     *  never written back to the preference (there is no single "global All") - see
     *  [ExpenseListViewModel.onHostelFilterChange]. */
    val filterHostelId: String? = null,
    val hostels: List<ExpenseHostelOption> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    /** null means every category. */
    val category: ExpenseCategory? = null,
    val sort: ExpenseSort = ExpenseSort.DATE,
) {
    val filterHostelName: String?
        get() = filterHostelId?.let { id -> hostels.firstOrNull { it.id == id }?.name }

    val visibleExpenses: List<Expense>
        get() {
            val inScope = if (filterHostelId == null) expenses else expenses.filter { it.hostelId == filterHostelId }
            val byCategory = if (category == null) inScope else inScope.filter { it.category == category }
            return when (sort) {
                ExpenseSort.DATE -> byCategory.sortedByDescending { it.incurredOn }
                ExpenseSort.AMOUNT -> byCategory.sortedByDescending { it.amount }
            }
        }

    /** Follows the filter: the number under a "Repair only" list is the repair total. */
    val total: Double get() = visibleExpenses.sumOf { it.amount }
}

@HiltViewModel
class ExpenseListViewModel @Inject constructor(
    private val hostelPreferences: HostelPreferences,
    private val hostelRepository: HostelRepository,
    private val expenseRepository: ExpenseRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpenseListUiState())
    val uiState: StateFlow<ExpenseListUiState> = _uiState.asStateFlow()

    init {
        // Same shape as MonthlyReportViewModel (LODGY-110): seed from the app's selected-hostel
        // preference, falling back to the first hostel, then treat a further pick the same way -
        // both screens should feel identical, not merely similar.
        viewModelScope.launch {
            val hostels = hostelRepository.getAll().first()
            val hostelOptions = hostels.map { hostel -> ExpenseHostelOption(hostel.id, hostel.name) }
            _uiState.update { it.copy(hasActiveHostel = hostelOptions.isNotEmpty(), hostels = hostelOptions) }
            val initialId = hostelPreferences.selectedHostelId.first() ?: hostels.firstOrNull()?.id
            onHostelFilterChange(initialId)
        }
        viewModelScope.launch {
            expenseRepository.observeAll().collect { expenses ->
                _uiState.update { it.copy(loading = false, expenses = expenses) }
            }
        }
    }

    /** Null means every property. Picking a specific one also becomes the app's overall selected
     *  hostel (LODGY-110), matching the Monthly Report - All is a real choice on this screen but
     *  never becomes the stored preference, since the rest of the app has nothing to open to for it. */
    fun onHostelFilterChange(hostelId: String?) {
        _uiState.update { it.copy(filterHostelId = hostelId) }
        if (hostelId != null) {
            viewModelScope.launch { hostelPreferences.setSelectedHostelId(hostelId) }
        }
    }

    fun onCategoryChange(category: ExpenseCategory?) = _uiState.update { it.copy(category = category) }

    fun onSortChange(sort: ExpenseSort) = _uiState.update { it.copy(sort = sort) }
}
