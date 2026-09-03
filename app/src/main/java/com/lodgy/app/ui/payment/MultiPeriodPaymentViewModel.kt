package com.lodgy.app.ui.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.OpenBalance
import com.lodgy.app.data.allocatePayment
import com.lodgy.app.data.effectiveAmountDue
import com.lodgy.app.data.entity.Invoice
import com.lodgy.app.data.entity.InvoiceStatus
import com.lodgy.app.data.entity.PaymentMode
import com.lodgy.app.data.repository.CreditRepository
import com.lodgy.app.data.repository.InvoiceRepository
import com.lodgy.app.data.repository.PaymentRepository
import com.lodgy.app.data.repository.TenancyAgreementRepository
import com.lodgy.app.data.repository.TenantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OpenInvoiceRow(val invoice: Invoice, val outstanding: Double)

data class MultiPeriodPaymentUiState(
    val loading: Boolean = true,
    val tenantName: String = "",
    val openInvoices: List<OpenInvoiceRow> = emptyList(),
    val amount: String = "",
    val mode: PaymentMode = PaymentMode.CASH,
    val paidOnMillis: Long = System.currentTimeMillis(),
    val saved: Boolean = false,
) {
    val totalOutstanding: Double get() = openInvoices.sumOf { it.outstanding }

    /** Live preview of the split, so the warden sees which months the money lands on. */
    val allocations: Map<String, Double>
        get() = allocatePayment(
            amount.toDoubleOrNull() ?: 0.0,
            openInvoices.map { OpenBalance(it.invoice.id, it.outstanding) },
        ).associate { it.invoiceId to it.amount }

    val canSave: Boolean
        get() = (amount.toDoubleOrNull() ?: 0.0) > 0.0 && openInvoices.size > 1
}

@HiltViewModel
class MultiPeriodPaymentViewModel @Inject constructor(
    private val invoiceRepository: InvoiceRepository,
    private val paymentRepository: PaymentRepository,
    private val creditRepository: CreditRepository,
    private val tenancyAgreementRepository: TenancyAgreementRepository,
    private val tenantRepository: TenantRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val tenantId: String = checkNotNull(savedStateHandle["tenantId"])

    private val _uiState = MutableStateFlow(MultiPeriodPaymentUiState())
    val uiState: StateFlow<MultiPeriodPaymentUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val agreement = tenancyAgreementRepository.getLatestByTenantId(tenantId)
            val rows = agreement
                ?.let { invoiceRepository.getByTenancyAgreementId(it.id).first() }
                .orEmpty()
                .filter { it.status != InvoiceStatus.PAID }
                .sortedWith(compareBy({ it.periodYear }, { it.periodMonth }))
                .map { invoice ->
                    val credits = creditRepository.getByInvoiceId(invoice.id).sumOf { it.amount }
                    val due = effectiveAmountDue(invoice.amountDue, credits)
                    OpenInvoiceRow(invoice, (due - paymentRepository.getTotalPaid(invoice.id)).coerceAtLeast(0.0))
                }
                .filter { it.outstanding > 0.0 }

            _uiState.update {
                it.copy(
                    loading = false,
                    tenantName = tenantRepository.getById(tenantId)?.name.orEmpty(),
                    openInvoices = rows,
                    amount = rows.sumOf { row -> row.outstanding }.takeIf { total -> total > 0 }?.toString().orEmpty(),
                )
            }
        }
    }

    fun onAmountChange(value: String) = _uiState.update { it.copy(amount = value) }
    fun onModeChange(value: PaymentMode) = _uiState.update { it.copy(mode = value) }
    fun onPaidOnChange(millis: Long) = _uiState.update { it.copy(paidOnMillis = millis) }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        val allocations = state.allocations
        if (allocations.isEmpty()) return

        viewModelScope.launch {
            // One id shared by every row this transaction writes - that shared id is what makes
            // the payment recognisable later as one lump sum rather than several ordinary ones.
            val groupId = UUID.randomUUID().toString()
            state.openInvoices.forEach { row ->
                val share = allocations[row.invoice.id] ?: return@forEach
                paymentRepository.create(
                    invoiceId = row.invoice.id,
                    amount = share,
                    mode = state.mode,
                    paidOn = state.paidOnMillis,
                    note = null,
                    multiPeriodGroupId = groupId,
                )
                val credits = creditRepository.getByInvoiceId(row.invoice.id).sumOf { it.amount }
                val due = effectiveAmountDue(row.invoice.amountDue, credits)
                val totalPaid = paymentRepository.getTotalPaid(row.invoice.id)
                invoiceRepository.updateStatus(
                    row.invoice,
                    when {
                        totalPaid >= due -> InvoiceStatus.PAID
                        totalPaid > 0.0 -> InvoiceStatus.PARTIAL
                        else -> InvoiceStatus.UNPAID
                    },
                )
            }
            _uiState.update { it.copy(saved = true) }
        }
    }
}
