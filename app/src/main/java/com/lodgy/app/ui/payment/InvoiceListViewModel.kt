package com.lodgy.app.ui.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.dao.BedLocation
import androidx.lifecycle.SavedStateHandle
import com.lodgy.app.data.effectiveAmountDue
import com.lodgy.app.data.isOverdue
import com.lodgy.app.data.entity.Invoice
import com.lodgy.app.data.entity.InvoiceStatus
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.CreditRepository
import com.lodgy.app.data.repository.HostelRepository
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** OVERDUE is not a status - it is "past due and not settled", which is what the Home tile counts
 *  and therefore what the list must show when a warden taps it (LODGY-89). */
enum class InvoiceFilter { ALL, OVERDUE, UNPAID, PARTIAL, PAID }

enum class InvoiceSort { DUE_DATE, AMOUNT }

data class InvoiceListItem(
    val invoice: Invoice,
    val tenantName: String,
    /** Kept so the list can be narrowed to one property when Home hands it a filter, and so the
     *  count on the tile and the rows on the list agree (LODGY-89). */
    val hostelId: String? = null,
    val location: BedLocation?,
    val totalPaid: Double,
    val creditTotal: Double = 0.0,
    /** True when some of this invoice's money arrived as part of one lump sum covering several
     *  months - the exceptional pattern LODGY-42 exists to make visible. */
    val partOfMultiPeriodPayment: Boolean = false,
    /** The warden has attested this invoice's period against their paper register (LODGY-43). */
    val periodReconciled: Boolean = false,
) {
    val effectiveDue: Double get() = effectiveAmountDue(invoice.amountDue, creditTotal)
}

data class InvoiceListUiState(
    val items: List<InvoiceListItem> = emptyList(),
    val filter: InvoiceFilter = InvoiceFilter.ALL,
    /** Null means every property, which is what the Payments tab itself shows. */
    val hostelId: String? = null,
    val hostelName: String = "",
    /** Blank means "any period" - a warden opening the tab should see everything, not
     *  silently only this month. */
    val periodMonth: String = "",
    val periodYear: String = "",
    val sort: InvoiceSort = InvoiceSort.DUE_DATE,
) {
    val filteredItems: List<InvoiceListItem>
        get() {
            val inScope = if (hostelId == null) items else items.filter { it.hostelId == hostelId }
            val byStatus = when (filter) {
                InvoiceFilter.ALL -> inScope
                InvoiceFilter.OVERDUE -> {
                    val startOfToday = startOfToday()
                    inScope.filter { isOverdue(it.invoice.status, it.invoice.dueDate, startOfToday) }
                }
                InvoiceFilter.UNPAID -> inScope.filter { it.invoice.status == InvoiceStatus.UNPAID }
                InvoiceFilter.PARTIAL -> inScope.filter { it.invoice.status == InvoiceStatus.PARTIAL }
                InvoiceFilter.PAID -> inScope.filter { it.invoice.status == InvoiceStatus.PAID }
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
    private val reconciliationRepository: ReconciliationRepository,
    private val hostelRepository: HostelRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // The Payments tab passes nothing and shows everything; Home passes both when a warden taps a
    // tile, so the list opens already answering the question the number raised (LODGY-89).
    private val scopedHostelId: String? = savedStateHandle["hostelId"]

    private val _uiState = MutableStateFlow(
        InvoiceListUiState(
            filter = runCatching {
                InvoiceFilter.valueOf(savedStateHandle.get<String>("filter") ?: "")
            }.getOrDefault(InvoiceFilter.ALL),
            hostelId = scopedHostelId,
        ),
    )
    val uiState: StateFlow<InvoiceListUiState> = _uiState.asStateFlow()

    init {
        scopedHostelId?.let { id ->
            viewModelScope.launch {
                val name = hostelRepository.getById(id)?.name.orEmpty()
                _uiState.update { it.copy(hostelName = name) }
            }
        }
        viewModelScope.launch {
            combine(
                invoiceRepository.getAll(),
                reconciliationRepository.observeAll(),
            ) { invoices, marks ->
                val reconciled = marks.map { Triple(it.hostelId, it.periodMonth, it.periodYear) }.toSet()
                invoices.sortedByDescending { it.createdAt }.map { enrich(it, reconciled) }
            }.collect { items -> _uiState.update { it.copy(items = items) } }
        }
    }

    fun onFilterChange(filter: InvoiceFilter) = _uiState.update { it.copy(filter = filter) }

    fun onPeriodMonthChange(value: String) = _uiState.update { it.copy(periodMonth = value.filter(Char::isDigit)) }

    fun onPeriodYearChange(value: String) = _uiState.update { it.copy(periodYear = value.filter(Char::isDigit)) }

    fun onSortChange(sort: InvoiceSort) = _uiState.update { it.copy(sort = sort) }

    private suspend fun enrich(
        invoice: Invoice,
        reconciledPeriods: Set<Triple<String, Int, Int>>,
    ): InvoiceListItem {
        val agreement = tenancyAgreementRepository.getById(invoice.tenancyAgreementId)
        val tenant = agreement?.let { tenantRepository.getById(it.tenantId) }
        val payments = paymentRepository.getByInvoiceId(invoice.id).first()
        val totalPaid = payments.sumOf { it.amount }
        val hostelId = agreement?.let { bedRepository.getHostelId(it.bedId) }
        return InvoiceListItem(
            invoice = invoice,
            hostelId = hostelId,
            tenantName = tenant?.name.orEmpty(),
            location = agreement?.let { bedRepository.getLocation(it.bedId) },
            totalPaid = totalPaid,
            creditTotal = creditRepository.getByInvoiceId(invoice.id).sumOf { it.amount },
            partOfMultiPeriodPayment = payments.any { it.multiPeriodGroupId != null },
            periodReconciled = hostelId != null &&
                Triple(hostelId, invoice.periodMonth, invoice.periodYear) in reconciledPeriods,
        )
    }
}

/** Local midnight, the same day boundary the dashboard's overdue count uses. */
private fun startOfToday(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis
