package com.lodgy.app.ui.tenant

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.entity.TenancyAgreement
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.TenancyAgreementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ForgoneRentUiState(
    val loading: Boolean = true,
    val hasNonRevenueTenancy: Boolean = false,
    val tenantName: String = "",
    val enabled: Boolean = false,
    val amount: String = "",
    val saved: Boolean = false,
) {
    /** An amount is only required once the warden has switched it on; turning it off saves
     *  whatever is in the field, since nothing will be recorded from it. */
    val canSave: Boolean get() = !enabled || (amount.toDoubleOrNull() ?: 0.0) > 0.0
}

/**
 * The discretionary half of a warden or caretaker room (LODGY-84). LODGY-82 stopped these rooms
 * being wrong - no invoices, no dues, still occupied - and this is the bookkeeping a warden may
 * want on top: the rent they give up recorded monthly as a cost, so the P&L shows what the
 * accommodation actually costs them. It stays off unless the warden asks for it, because a warden
 * living in their own building has spent nothing.
 */
@HiltViewModel
class ForgoneRentViewModel @Inject constructor(
    private val tenancyAgreementRepository: TenancyAgreementRepository,
    private val bedRepository: BedRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val tenantId: String = checkNotNull(savedStateHandle["tenantId"])
    private var agreement: TenancyAgreement? = null

    private val _uiState = MutableStateFlow(ForgoneRentUiState())
    val uiState: StateFlow<ForgoneRentUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val active = tenancyAgreementRepository.getActiveByTenantId(tenantId)
            agreement = active
            if (active == null || !active.nonRevenue) {
                _uiState.update { it.copy(loading = false, hasNonRevenueTenancy = false) }
                return@launch
            }
            // The room's own rate is the market rent being forgone, which is the only figure the
            // app can defend on the warden's behalf. It is a starting point, not a rule - they can
            // type a different one, and a figure they set already wins over it.
            val amount = active.forgoneRentAmount ?: bedRepository.getRoomPrice(active.bedId)
            _uiState.update {
                it.copy(
                    loading = false,
                    hasNonRevenueTenancy = true,
                    enabled = active.forgoneRentExpense,
                    amount = amount?.toString().orEmpty(),
                )
            }
        }
    }

    fun onEnabledChange(value: Boolean) = _uiState.update { it.copy(enabled = value) }

    fun onAmountChange(value: String) = _uiState.update { it.copy(amount = value) }

    fun save() {
        val current = agreement ?: return
        val state = _uiState.value
        if (!state.canSave) return
        viewModelScope.launch {
            tenancyAgreementRepository.setForgoneRentExpense(
                agreement = current,
                enabled = state.enabled,
                // Kept even when the switch goes off, so turning it back on later offers the
                // warden the figure they chose rather than starting from the room rate again.
                amount = state.amount.toDoubleOrNull() ?: current.forgoneRentAmount,
            )
            _uiState.update { it.copy(saved = true) }
        }
    }
}
