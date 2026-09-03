package com.lodgy.app.ui.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.dao.BedLocation
import com.lodgy.app.data.effectiveAmountDue
import com.lodgy.app.data.entity.Invoice
import com.lodgy.app.data.entity.InvoiceStatus
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class InvoiceFilter { ALL, UNPAID, PARTIAL, PAID }

enum class InvoiceSort { DUE_DATE, AMOUNT }

data class InvoiceListItem(
    val invoice: Invoice,
    val tenantName: String,
    val location: BedLocation?,
    val totalPaid: Double,
    val creditTotal: Double = 0.0,
) {
    val effectiveDue: Double get() = effectiveAmountDue(invoice.amountDue, creditTotal)
}

data class InvoiceListUiState(
    val items: List<InvoiceListItem> = emptyList(),
    val filter: InvoiceFilter = InvoiceFilter.ALL,
    /** Blank means "any period" - a warden opening the tab should see everything, not
     *  silently only this month. */
    val periodMonth: String = "",
    val periodYear: String = "",
    val sort: InvoiceSort = InvoiceSort.DUE_DATE,
) {
    val filteredItems: List<InvoiceListItem>
        get() {
            val byStatus = when (filter) {
                InvoiceFilter.ALL -> items
                InvoiceFilter.UNPAID -> items.filter { it.invoice.status == InvoiceStatus.UNPAID }
                InvoiceFilter.PARTIAL -> items.filter { it.invoice.status == InvoiceStatus.PARTIAL }
                InvoiceFilter.PAID -> items.filter { it.invoice.status == InvoiceStatus.PAID }
            }
            val month = periodMonth.toIntOrNull()
            val year = periodYear.toIntOrNull()
            val byPeriod = byStatus.filter {
                (month == null || it.invoice.periodMonth == month) &&
                    (year == null || it.invoice.periodYear == year)
            }
            return when (sort) {
                InvoiceSort.DUE_DATE -> byPeriod.sortedByDescending { it.invoice.dueDate }
                InvoiceSort.AMOUNT -> byPeriod.sortedByDescending { it.effectiveDue }
            }
        }
}

@HiltViewModel
class InvoiceListViewModel @Inject constructor(
    private val invoiceRepository: InvoiceRepository,
    private val tenancyAgreementRepository: TenancyAgreementRepository,
    private val tenantRepository: TenantRepository,
    private val paymentRepository: PaymentRepository,
    private val bedRepository: BedRepository,
    private val creditRepository: CreditRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InvoiceListUiState())
    val uiState: StateFlow<InvoiceListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            invoiceRepository.getAll()
                .map { invoices -> invoices.sortedByDescending { it.createdAt }.map { invoice -> enrich(invoice) } }
                .collect { items -> _uiState.update { it.copy(items = items) } }
        }
    }

    fun onFilterChange(filter: InvoiceFilter) = _uiState.update { it.copy(filter = filter) }

    fun onPeriodMonthChange(value: String) = _uiState.update { it.copy(periodMonth = value.filter(Char::isDigit)) }

    fun onPeriodYearChange(value: String) = _uiState.update { it.copy(periodYear = value.filter(Char::isDigit)) }

    fun onSortChange(sort: InvoiceSort) = _uiState.update { it.copy(sort = sort) }

    private suspend fun enrich(invoice: Invoice): InvoiceListItem {
        val agreement = tenancyAgreementRepository.getById(invoice.tenancyAgreementId)
        val tenant = agreement?.let { tenantRepository.getById(it.tenantId) }
        val totalPaid = paymentRepository.getTotalPaid(invoice.id)
        return InvoiceListItem(
            invoice = invoice,
            tenantName = tenant?.name.orEmpty(),
            location = agreement?.let { bedRepository.getLocation(it.bedId) },
            totalPaid = totalPaid,
            creditTotal = creditRepository.getByInvoiceId(invoice.id).sumOf { it.amount },
        )
    }
}
