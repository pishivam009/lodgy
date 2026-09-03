package com.lodgy.app.ui.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.entity.Invoice
import com.lodgy.app.data.entity.InvoiceStatus
import com.lodgy.app.data.repository.CreditRepository
import com.lodgy.app.data.repository.InvoiceRepository
import com.lodgy.app.data.repository.TenancyAgreementRepository
import com.lodgy.app.data.repository.TenantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreditFormUiState(
    val loading: Boolean = true,
    val tenantName: String = "",
    val amount: String = "",
    val reason: String = "",
    /** Open invoices the credit can be attached to; null selection means the next one generated. */
    val openInvoices: List<Invoice> = emptyList(),
    val selectedInvoiceId: String? = null,
    val saved: Boolean = false,
) {
    val canSave: Boolean
        get() = (amount.toDoubleOrNull() ?: 0.0) > 0.0 && reason.isNotBlank()
}

@HiltViewModel
class CreditFormViewModel @Inject constructor(
    private val creditRepository: CreditRepository,
    private val tenantRepository: TenantRepository,
    private val tenancyAgreementRepository: TenancyAgreementRepository,
    private val invoiceRepository: InvoiceRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val tenantId: String = checkNotNull(savedStateHandle["tenantId"])

    private val _uiState = MutableStateFlow(CreditFormUiState())
    val uiState: StateFlow<CreditFormUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val agreement = tenancyAgreementRepository.getLatestByTenantId(tenantId)
            val openInvoices = agreement
                ?.let { invoiceRepository.getByTenancyAgreementId(it.id).first() }
                .orEmpty()
                .filter { it.status != InvoiceStatus.PAID }
                .sortedByDescending { it.dueDate }
            _uiState.update {
                it.copy(
                    loading = false,
                    tenantName = tenantRepository.getById(tenantId)?.name.orEmpty(),
                    openInvoices = openInvoices,
                )
            }
        }
    }

    fun onAmountChange(value: String) = _uiState.update { it.copy(amount = value) }

    fun onReasonChange(value: String) = _uiState.update { it.copy(reason = value) }

    fun onInvoiceSelected(invoiceId: String?) = _uiState.update { it.copy(selectedInvoiceId = invoiceId) }

    fun save() {
        val state = _uiState.value
        val amount = state.amount.toDoubleOrNull() ?: return
        if (amount <= 0.0 || state.reason.isBlank()) return
        viewModelScope.launch {
            creditRepository.create(tenantId, state.selectedInvoiceId, amount, state.reason.trim())
            _uiState.update { it.copy(saved = true) }
        }
    }
}
