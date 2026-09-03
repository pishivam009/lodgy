package com.lodgy.app.ui.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.repository.InvoiceRepository
import com.lodgy.app.data.repository.TenancyAgreementRepository
import com.lodgy.app.data.repository.TenantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ManualInvoiceFormUiState(
    val loading: Boolean = true,
    val hasActiveAgreement: Boolean = false,
    val tenantName: String = "",
    val periodMonth: String = "",
    val periodYear: String = "",
    val amountDue: String = "",
    val dueDateMillis: Long = System.currentTimeMillis(),
    val duplicateError: Boolean = false,
    val saved: Boolean = false,
) {
    val canSave: Boolean
        get() = periodMonth.toIntOrNull()?.let { it in 1..12 } == true &&
            periodYear.toIntOrNull() != null &&
            amountDue.toDoubleOrNull() != null
}

@HiltViewModel
class ManualInvoiceFormViewModel @Inject constructor(
    private val invoiceRepository: InvoiceRepository,
    private val tenancyAgreementRepository: TenancyAgreementRepository,
    tenantRepository: TenantRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val tenantId: String = checkNotNull(savedStateHandle["tenantId"])
    private var tenancyAgreementId: String? = null

    private val _uiState = MutableStateFlow(ManualInvoiceFormUiState())
    val uiState: StateFlow<ManualInvoiceFormUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val tenant = tenantRepository.getById(tenantId)
            val agreement = tenancyAgreementRepository.getActiveByTenantId(tenantId)
            tenancyAgreementId = agreement?.id
            val today = Calendar.getInstance()
            _uiState.update {
                it.copy(
                    loading = false,
                    hasActiveAgreement = agreement != null,
                    tenantName = tenant?.name.orEmpty(),
                    periodMonth = (today.get(Calendar.MONTH) + 1).toString(),
                    periodYear = today.get(Calendar.YEAR).toString(),
                    amountDue = agreement?.agreedRent?.toString().orEmpty(),
                )
            }
        }
    }

    fun onPeriodMonthChange(value: String) = _uiState.update { it.copy(periodMonth = value, duplicateError = false) }
    fun onPeriodYearChange(value: String) = _uiState.update { it.copy(periodYear = value, duplicateError = false) }
    fun onAmountDueChange(value: String) = _uiState.update { it.copy(amountDue = value) }
    fun onDueDateChange(millis: Long) = _uiState.update { it.copy(dueDateMillis = millis) }

    fun save() {
        val agreementId = tenancyAgreementId ?: return
        val state = _uiState.value
        val month = state.periodMonth.toIntOrNull() ?: return
        val year = state.periodYear.toIntOrNull() ?: return
        val amount = state.amountDue.toDoubleOrNull() ?: return
        viewModelScope.launch {
            if (invoiceRepository.existsForPeriod(agreementId, month, year)) {
                _uiState.update { it.copy(duplicateError = true) }
                return@launch
            }
            invoiceRepository.create(agreementId, month, year, amount, state.dueDateMillis)
            _uiState.update { it.copy(saved = true) }
        }
    }
}
