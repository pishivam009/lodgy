package com.lodgy.app.ui.tenant

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.entity.TenancyAgreement
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.TenancyAgreementRepository
import com.lodgy.app.data.repository.TenantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CheckoutUiState(
    val loading: Boolean = true,
    val hasActiveAgreement: Boolean = false,
    val tenantName: String = "",
    val advanceDeposit: Double = 0.0,
    val moveOutDateMillis: Long = System.currentTimeMillis(),
    val damageDeduction: String = "0",
    val saved: Boolean = false,
) {
    val refundAmount: Double
        get() = advanceDeposit - (damageDeduction.toDoubleOrNull() ?: 0.0)
}

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val tenancyAgreementRepository: TenancyAgreementRepository,
    private val tenantRepository: TenantRepository,
    private val bedRepository: BedRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val tenantId: String = checkNotNull(savedStateHandle["tenantId"])
    private var agreement: TenancyAgreement? = null

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val tenant = tenantRepository.getById(tenantId)
            val active = tenancyAgreementRepository.getActiveByTenantId(tenantId)
            agreement = active
            _uiState.update {
                it.copy(
                    loading = false,
                    hasActiveAgreement = active != null,
                    tenantName = tenant?.name.orEmpty(),
                    advanceDeposit = active?.advanceDeposit ?: 0.0,
                )
            }
        }
    }

    fun onMoveOutDateChange(millis: Long) = _uiState.update { it.copy(moveOutDateMillis = millis) }
    fun onDamageDeductionChange(value: String) = _uiState.update { it.copy(damageDeduction = value) }

    fun confirmCheckout() {
        val current = agreement ?: return
        val state = _uiState.value
        viewModelScope.launch {
            tenancyAgreementRepository.close(current, state.moveOutDateMillis, state.refundAmount)
            bedRepository.setVacant(current.bedId)
            tenantRepository.getById(tenantId)?.let { tenantRepository.setVacated(it) }
            _uiState.update { it.copy(saved = true) }
        }
    }
}
