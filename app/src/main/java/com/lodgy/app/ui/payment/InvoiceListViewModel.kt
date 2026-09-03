package com.lodgy.app.ui.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.entity.Invoice
import com.lodgy.app.data.entity.InvoiceStatus
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

data class InvoiceListItem(
    val invoice: Invoice,
    val tenantName: String,
    val totalPaid: Double,
)

data class InvoiceListUiState(
    val items: List<InvoiceListItem> = emptyList(),
    val filter: InvoiceFilter = InvoiceFilter.ALL,
) {
    val filteredItems: List<InvoiceListItem>
        get() = when (filter) {
            InvoiceFilter.ALL -> items
            InvoiceFilter.UNPAID -> items.filter { it.invoice.status == InvoiceStatus.UNPAID }
            InvoiceFilter.PARTIAL -> items.filter { it.invoice.status == InvoiceStatus.PARTIAL }
            InvoiceFilter.PAID -> items.filter { it.invoice.status == InvoiceStatus.PAID }
        }
}

@HiltViewModel
class InvoiceListViewModel @Inject constructor(
    private val invoiceRepository: InvoiceRepository,
    private val tenancyAgreementRepository: TenancyAgreementRepository,
    private val tenantRepository: TenantRepository,
    private val paymentRepository: PaymentRepository,
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

    private suspend fun enrich(invoice: Invoice): InvoiceListItem {
        val agreement = tenancyAgreementRepository.getById(invoice.tenancyAgreementId)
        val tenant = agreement?.let { tenantRepository.getById(it.tenantId) }
        val totalPaid = paymentRepository.getTotalPaid(invoice.id)
        return InvoiceListItem(invoice = invoice, tenantName = tenant?.name.orEmpty(), totalPaid = totalPaid)
    }
}
