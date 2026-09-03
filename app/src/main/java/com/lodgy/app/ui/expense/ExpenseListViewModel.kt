package com.lodgy.app.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.entity.Expense
import com.lodgy.app.data.prefs.HostelPreferences
import com.lodgy.app.data.repository.ExpenseRepository
import com.lodgy.app.data.repository.HostelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExpenseListUiState(
    val loading: Boolean = true,
    val hasActiveHostel: Boolean = false,
    val hostelId: String? = null,
    val hostelName: String = "",
    val expenses: List<Expense> = emptyList(),
) {
    val total: Double get() = expenses.sumOf { it.amount }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExpenseListViewModel @Inject constructor(
    private val hostelPreferences: HostelPreferences,
    private val hostelRepository: HostelRepository,
    private val expenseRepository: ExpenseRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpenseListUiState())
    val uiState: StateFlow<ExpenseListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            hostelPreferences.selectedHostelId.flatMapLatest { hostelId ->
                if (hostelId == null) {
                    flowOf(null)
                } else {
                    expenseRepository.getByHostelId(hostelId)
                }
            }.collect { expenses ->
                if (expenses == null) {
                    _uiState.update { it.copy(loading = false, hasActiveHostel = false) }
                } else {
                    _uiState.update { it.copy(loading = false, hasActiveHostel = true, expenses = expenses) }
                }
            }
        }
        viewModelScope.launch {
            hostelPreferences.selectedHostelId.collect { hostelId ->
                _uiState.update { it.copy(hostelId = hostelId) }
                if (hostelId != null) {
                    val hostel = hostelRepository.getById(hostelId)
                    _uiState.update { it.copy(hostelName = hostel?.name.orEmpty()) }
                }
            }
        }
    }
}
