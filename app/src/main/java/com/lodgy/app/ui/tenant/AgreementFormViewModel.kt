package com.lodgy.app.ui.tenant

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.TenancyAgreementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AgreementFormUiState(
    val agreedRent: String = "",
    val advanceDeposit: String = "",
    val billingCycleDay: String = "",
    val moveInDateMillis: Long = System.currentTimeMillis(),
    val saved: Boolean = false,
) {
    val billingCycleDayValid: Boolean
        get() = billingCycleDay.toIntOrNull()?.let { it in 1..28 } == true

    val canSave: Boolean
        get() = agreedRent.toDoubleOrNull() != null && advanceDeposit.toDoubleOrNull() != null && billingCycleDayValid
}

@HiltViewModel
class AgreementFormViewModel @Inject constructor(
    private val tenancyAgreementRepository: TenancyAgreementRepository,
    private val bedRepository: BedRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val tenantId: String = checkNotNull(savedStateHandle["tenantId"])
    private val bedId: String = checkNotNull(savedStateHandle["bedId"])

    private val _uiState = MutableStateFlow(AgreementFormUiState())
    val uiState: StateFlow<AgreementFormUiState> = _uiState.asStateFlow()

    fun onAgreedRentChange(value: String) = _uiState.update { it.copy(agreedRent = value) }
    fun onAdvanceDepositChange(value: String) = _uiState.update { it.copy(advanceDeposit = value) }
    fun onBillingCycleDayChange(value: String) = _uiState.update { it.copy(billingCycleDay = value) }
    fun onMoveInDateChange(millis: Long) = _uiState.update { it.copy(moveInDateMillis = millis) }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        val rent = state.agreedRent.toDoubleOrNull() ?: return
        val deposit = state.advanceDeposit.toDoubleOrNull() ?: return
        val billingDay = state.billingCycleDay.toIntOrNull() ?: return
        viewModelScope.launch {
            tenancyAgreementRepository.create(tenantId, bedId, rent, deposit, billingDay, state.moveInDateMillis)
            bedRepository.setOccupied(bedId)
            _uiState.update { it.copy(saved = true) }
        }
    }
}
