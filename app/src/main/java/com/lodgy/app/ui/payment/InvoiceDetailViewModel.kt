package com.lodgy.app.ui.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.dao.BedLocation
import com.lodgy.app.data.effectiveAmountDue
import com.lodgy.app.data.isOverdue
import com.lodgy.app.data.entity.InvoiceStatus
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.CreditRepository
import com.lodgy.app.data.repository.InvoiceRepository
import com.lodgy.app.data.repository.PaymentRepository
import com.lodgy.app.data.repository.ReconciliationRepository
import com.lodgy.app.data.repository.TenancyAgreementRepository
import com.lodgy.app.data.repository.TenantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InvoiceDetailUiState(
    val loading: Boolean = true,
    val found: Boolean = false,
    val tenantName: String = "",
    val location: BedLocation? = null,
    val periodMonth: Int = 0,
    val periodYear: Int = 0,
    val dueDateMillis: Long = 0L,
    val status: InvoiceStatus = InvoiceStatus.UNPAID,
    val amountDue: Double = 0.0,
    val creditTotal: Double = 0.0,
    val totalPaid: Double = 0.0,
    /** Same exceptional-pattern flag as the list row (LODGY-42). */
    val partOfMultiPeriodPayment: Boolean = false,
    /** The warden has attested this invoice's period against their paper register (LODGY-43). */
    val periodReconciled: Boolean = false,
    val isOverdue: Boolean = false,
) {
    val effectiveDue: Double get() = effectiveAmountDue(amountDue, creditTotal)
}

/** The hub a tapped invoice card opens (LODGY-108): the same figures the list row already shows,
 *  plus the due date, and the actions that used to live only as inline buttons on that row - now
 *  reachable from one screen, matching how TenantProfileScreen hubs a tenant's actions. */
@HiltViewModel
class InvoiceDetailViewModel @Inject constructor(
    private val invoiceRepository: InvoiceRepository,
    private val tenancyAgreementRepository: TenancyAgreementRepository,
    private val tenantRepository: TenantRepository,
    private val paymentRepository: PaymentRepository,
    private val bedRepository: BedRepository,
    private val creditRepository: CreditRepository,
    private val reconciliationRepository: ReconciliationRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val invoiceId: String = checkNotNull(savedStateHandle["invoiceId"])

    private val _uiState = MutableStateFlow(InvoiceDetailUiState())
    val uiState: StateFlow<InvoiceDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val invoice = invoiceRepository.getById(invoiceId)
            if (invoice == null) {
                _uiState.update { it.copy(loading = false, found = false) }
                return@launch
            }
            val agreement = tenancyAgreementRepository.getById(invoice.tenancyAgreementId)
            val tenant = agreement?.let { tenantRepository.getById(it.tenantId) }
            val hostelId = agreement?.let { bedRepository.getHostelId(it.bedId) }
            val payments = paymentRepository.getByInvoiceId(invoiceId).first()
            val creditTotal = creditRepository.getByInvoiceId(invoiceId).sumOf { it.amount }
            val reconciled = hostelId != null &&
                reconciliationRepository.getForPeriod(hostelId, invoice.periodMonth, invoice.periodYear) != null

            _uiState.update {
                it.copy(
                    loading = false,
                    found = true,
                    tenantName = tenant?.name.orEmpty(),
                    location = agreement?.let { a -> bedRepository.getLocation(a.bedId) },
                    periodMonth = invoice.periodMonth,
                    periodYear = invoice.periodYear,
                    dueDateMillis = invoice.dueDate,
                    status = invoice.status,
                    amountDue = invoice.amountDue,
                    creditTotal = creditTotal,
                    totalPaid = payments.sumOf { payment -> payment.amount },
                    partOfMultiPeriodPayment = payments.any { payment -> payment.multiPeriodGroupId != null },
                    periodReconciled = reconciled,
                    isOverdue = isOverdue(invoice.status, invoice.dueDate, startOfToday()),
                )
            }
        }
    }
}

/** Local midnight, the same day boundary the Payments list uses for its own overdue filter. */
private fun startOfToday(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis
