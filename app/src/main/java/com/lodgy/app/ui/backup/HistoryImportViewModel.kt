package com.lodgy.app.ui.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.backup.HistoryCsvReader
import com.lodgy.app.backup.HistoryRow
import com.lodgy.app.backup.HistoryRowError
import com.lodgy.app.backup.parseHistoryCsv
import com.lodgy.app.data.entity.InvoiceStatus
import com.lodgy.app.data.entity.PaymentMode
import com.lodgy.app.data.repository.InvoiceRepository
import com.lodgy.app.data.repository.PaymentRepository
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

data class HistoryImportUiState(
    val parsing: Boolean = false,
    val importing: Boolean = false,
    val rows: List<HistoryRow> = emptyList(),
    val errors: List<HistoryRowError> = emptyList(),
    /** Rows whose phone number matches no tenant in the app. */
    val unmatchedRows: List<HistoryRow> = emptyList(),
    val imported: Int? = null,
    val readFailed: Boolean = false,
) {
    val importableCount: Int get() = rows.size - unmatchedRows.size
    val canImport: Boolean get() = !importing && importableCount > 0
}

@HiltViewModel
class HistoryImportViewModel @Inject constructor(
    private val csvReader: HistoryCsvReader,
    private val tenantRepository: TenantRepository,
    private val tenancyAgreementRepository: TenancyAgreementRepository,
    private val invoiceRepository: InvoiceRepository,
    private val paymentRepository: PaymentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryImportUiState())
    val uiState: StateFlow<HistoryImportUiState> = _uiState.asStateFlow()

    fun onFilePicked(uri: Uri) {
        _uiState.update { HistoryImportUiState(parsing = true) }
        viewModelScope.launch {
            val text = csvReader.read(uri)
            if (text == null) {
                _uiState.update { HistoryImportUiState(readFailed = true) }
                return@launch
            }
            val parsed = parseHistoryCsv(text)
            val knownPhones = tenantRepository.getAll().first().map { it.phone }.toSet()
            _uiState.update {
                HistoryImportUiState(
                    rows = parsed.rows,
                    errors = parsed.errors,
                    unmatchedRows = parsed.rows.filter { row -> row.phone !in knownPhones },
                )
            }
        }
    }

    /**
     * Writes each matched row as a real invoice plus, where something was paid, a real payment -
     * the same records the app would have held if the month had been entered at the time, so
     * reports and balances treat imported history no differently.
     */
    fun import() {
        val state = _uiState.value
        if (!state.canImport) return
        _uiState.update { it.copy(importing = true) }
        viewModelScope.launch {
            val tenantsByPhone = tenantRepository.getAll().first().associateBy { it.phone }
            var written = 0

            state.rows.forEach { row ->
                val tenant = tenantsByPhone[row.phone] ?: return@forEach
                val agreement = tenancyAgreementRepository.getLatestByTenantId(tenant.id) ?: return@forEach
                // Never overwrite a period the app already knows about - re-running an import
                // must not double-bill a tenant.
                if (invoiceRepository.existsForPeriod(agreement.id, row.periodMonth, row.periodYear)) {
                    return@forEach
                }

                val dueDate = Calendar.getInstance().apply {
                    set(row.periodYear, row.periodMonth - 1, agreement.billingCycleDay, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val invoice = invoiceRepository.create(
                    tenancyAgreementId = agreement.id,
                    periodMonth = row.periodMonth,
                    periodYear = row.periodYear,
                    amountDue = row.amountDue,
                    dueDate = dueDate,
                )
                if (row.amountPaid > 0.0) {
                    paymentRepository.create(invoice.id, row.amountPaid, PaymentMode.OTHER, dueDate, null)
                }
                invoiceRepository.updateStatus(
                    invoice,
                    when {
                        row.amountPaid >= row.amountDue -> InvoiceStatus.PAID
                        row.amountPaid > 0.0 -> InvoiceStatus.PARTIAL
                        else -> InvoiceStatus.UNPAID
                    },
                )
                written++
            }

            _uiState.update { it.copy(importing = false, imported = written) }
        }
    }
}
