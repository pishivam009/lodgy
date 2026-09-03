package com.lodgy.app.ui.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.dao.BedLocation
import com.lodgy.app.data.effectiveAmountDue
import com.lodgy.app.data.entity.Invoice
import com.lodgy.app.data.entity.InvoiceStatus
import com.lodgy.app.data.entity.PaymentMode
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.CreditRepository
import com.lodgy.app.data.repository.InvoiceRepository
import com.lodgy.app.data.repository.PaymentRepository
import com.lodgy.app.data.repository.TenancyAgreementRepository
import com.lodgy.app.data.repository.TenantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecordPaymentUiState(
    val loading: Boolean = true,
    val tenantName: String = "",
    val location: BedLocation? = null,
    val amountDue: Double = 0.0,
    val creditTotal: Double = 0.0,
    val alreadyPaid: Double = 0.0,
    val amount: String = "",
    val mode: PaymentMode = PaymentMode.CASH,
    val paidOnMillis: Long = System.currentTimeMillis(),
    val note: String = "",
    val saved: Boolean = false,
) {
    val effectiveDue: Double get() = effectiveAmountDue(amountDue, creditTotal)
    val canSave: Boolean get() = amount.toDoubleOrNull()?.let { it > 0 } == true
}

@HiltViewModel
class RecordPaymentViewModel @Inject constructor(
    private val invoiceRepository: InvoiceRepository,
    private val paymentRepository: PaymentRepository,
    private val tenancyAgreementRepository: TenancyAgreementRepository,
    private val tenantRepository: TenantRepository,
    private val bedRepository: BedRepository,
    private val creditRepository: CreditRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val invoiceId: String = checkNotNull(savedStateHandle["invoiceId"])
    private var invoice: Invoice? = null

    private val _uiState = MutableStateFlow(RecordPaymentUiState())
    val uiState: StateFlow<RecordPaymentUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val current = invoiceRepository.getById(invoiceId) ?: return@launch
            invoice = current
            val agreement = tenancyAgreementRepository.getById(current.tenancyAgreementId)
            val tenant = agreement?.let { tenantRepository.getById(it.tenantId) }
            val alreadyPaid = paymentRepository.getTotalPaid(invoiceId)
            val location = agreement?.let { bedRepository.getLocation(it.bedId) }
            val creditTotal = creditRepository.getByInvoiceId(invoiceId).sumOf { it.amount }
            val remaining = effectiveAmountDue(current.amountDue, creditTotal) - alreadyPaid
            _uiState.update {
                it.copy(
                    loading = false,
                    tenantName = tenant?.name.orEmpty(),
                    location = location,
                    amountDue = current.amountDue,
                    creditTotal = creditTotal,
                    alreadyPaid = alreadyPaid,
                    amount = if (remaining > 0) remaining.toString() else "",
                )
            }
        }
    }

    fun onAmountChange(value: String) = _uiState.update { it.copy(amount = value) }
    fun onModeChange(value: PaymentMode) = _uiState.update { it.copy(mode = value) }
    fun onPaidOnChange(millis: Long) = _uiState.update { it.copy(paidOnMillis = millis) }
    fun onNoteChange(value: String) = _uiState.update { it.copy(note = value) }

    fun save() {
        val current = invoice ?: return
        val state = _uiState.value
        val amount = state.amount.toDoubleOrNull() ?: return
        viewModelScope.launch {
            paymentRepository.create(invoiceId, amount, state.mode, state.paidOnMillis, state.note.ifBlank { null })
            val totalPaid = paymentRepository.getTotalPaid(invoiceId)
            val due = effectiveAmountDue(current.amountDue, creditRepository.getByInvoiceId(invoiceId).sumOf { it.amount })
            val newStatus = when {
                totalPaid >= due -> InvoiceStatus.PAID
                totalPaid > 0 -> InvoiceStatus.PARTIAL
                else -> InvoiceStatus.UNPAID
            }
            invoiceRepository.updateStatus(current, newStatus)
            _uiState.update { it.copy(saved = true) }
        }
    }
}
