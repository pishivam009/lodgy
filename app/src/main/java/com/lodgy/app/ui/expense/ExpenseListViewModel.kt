package com.lodgy.app.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.entity.Expense
import com.lodgy.app.data.entity.ExpenseCategory
import com.lodgy.app.data.repository.ExpenseRepository
import com.lodgy.app.data.repository.HostelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ExpenseSort { DATE, AMOUNT }

data class ExpenseListUiState(
    val loading: Boolean = true,
    val hasActiveHostel: Boolean = false,
    /** Null means every property - the default for a multi-hostel warden, so the total is not
     *  silently scoped to whichever property happens to be selected elsewhere (LODGY-109). Only
     *  shown as a picker when there is more than one hostel. */
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
    private val hostelRepository: HostelRepository,
    private val expenseRepository: ExpenseRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpenseListUiState())
    val uiState: StateFlow<ExpenseListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                hostelRepository.getAll(),
                expenseRepository.observeAll(),
            ) { hostels, expenses ->
                hostels.map { ExpenseHostelOption(it.id, it.name) } to expenses
            }.collect { (hostels, expenses) ->
                _uiState.update {
                    it.copy(loading = false, hasActiveHostel = hostels.isNotEmpty(), hostels = hostels, expenses = expenses)
                }
            }
        }
    }

    fun onHostelFilterChange(hostelId: String?) = _uiState.update { it.copy(filterHostelId = hostelId) }

    fun onCategoryChange(category: ExpenseCategory?) = _uiState.update { it.copy(category = category) }

    fun onSortChange(sort: ExpenseSort) = _uiState.update { it.copy(sort = sort) }
}
