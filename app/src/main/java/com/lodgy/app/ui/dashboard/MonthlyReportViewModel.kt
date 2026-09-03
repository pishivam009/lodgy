package com.lodgy.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.entity.BedStatus
import com.lodgy.app.data.entity.InvoiceStatus
import com.lodgy.app.data.effectiveAmountDue
import com.lodgy.app.data.prefs.HostelPreferences
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.CreditRepository
import com.lodgy.app.data.repository.ExpenseRepository
import com.lodgy.app.data.repository.FloorRepository
import com.lodgy.app.data.repository.HostelRepository
import com.lodgy.app.data.repository.InvoiceRepository
import com.lodgy.app.data.repository.PaymentRepository
import com.lodgy.app.data.repository.ReconciliationRepository
import com.lodgy.app.data.repository.RoomRepository
import com.lodgy.app.data.repository.TenancyAgreementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MonthlyReportUiState(
    val loading: Boolean = true,
    val hasActiveHostel: Boolean = false,
    val hostelName: String = "",
    val month: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val year: Int = Calendar.getInstance().get(Calendar.YEAR),
    val totalCollected: Double = 0.0,
    val totalDues: Double = 0.0,
    val occupancyPercent: Int = 0,
    val totalExpense: Double = 0.0,
    val totalCredits: Double = 0.0,
    /** Warden's attestation that this period was checked against the paper register. */
    val reconciled: Boolean = false,
) {
    val netIncome: Double get() = totalCollected - totalExpense

    /** Occupancy is measured off the hostel's beds as they stand right now - the schema keeps no
     *  bed-state history to reconstruct a past month from (LODGY-52). Say so when the warden is
     *  looking at a period that has already closed, rather than letting the figure read as
     *  historical. */
    val occupancyIsCurrentStateOnly: Boolean
        get() {
            val now = Calendar.getInstance()
            return year < now.get(Calendar.YEAR) ||
                (year == now.get(Calendar.YEAR) && month < now.get(Calendar.MONTH) + 1)
        }
}

@HiltViewModel
class MonthlyReportViewModel @Inject constructor(
    private val hostelPreferences: HostelPreferences,
    private val hostelRepository: HostelRepository,
    private val floorRepository: FloorRepository,
    private val roomRepository: RoomRepository,
    private val bedRepository: BedRepository,
    private val tenancyAgreementRepository: TenancyAgreementRepository,
    private val invoiceRepository: InvoiceRepository,
    private val paymentRepository: PaymentRepository,
    private val expenseRepository: ExpenseRepository,
    private val creditRepository: CreditRepository,
    private val reconciliationRepository: ReconciliationRepository,
) : ViewModel() {

    private var hostelId: String? = null

    private val _uiState = MutableStateFlow(MonthlyReportUiState())
    val uiState: StateFlow<MonthlyReportUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            hostelPreferences.selectedHostelId.collect { id ->
                hostelId = id
                if (id == null) {
                    _uiState.update { it.copy(loading = false, hasActiveHostel = false) }
                } else {
                    val hostel = hostelRepository.getById(id)
                    _uiState.update { it.copy(hasActiveHostel = true, hostelName = hostel?.name.orEmpty()) }
                    refresh()
                }
            }
        }
    }

    fun onMonthChange(month: Int) {
        _uiState.update { it.copy(month = month) }
        viewModelScope.launch { refresh() }
    }

    fun onYearChange(year: Int) {
        _uiState.update { it.copy(year = year) }
        viewModelScope.launch { refresh() }
    }

    private suspend fun refresh() {
        val id = hostelId ?: return
        val state = _uiState.value

        val bedsInHostel = floorRepository.getByHostelId(id).first()
            .flatMap { floor -> roomRepository.getByFloorId(floor.id).first() }
            .flatMap { room -> bedRepository.getByRoomId(room.id).first() }
        val bedIdsInHostel = bedsInHostel.map { it.id }.toSet()
        val occupancyPercent = if (bedsInHostel.isEmpty()) {
            0
        } else {
            (bedsInHostel.count { it.status == BedStatus.OCCUPIED } * 100) / bedsInHostel.size
        }

        val agreementIds = tenancyAgreementRepository.getAll()
            .filter { it.bedId in bedIdsInHostel }
            .map { it.id }
            .toSet()

        val invoicesForPeriod = invoiceRepository.getAll().first()
            .filter { it.tenancyAgreementId in agreementIds && it.periodMonth == state.month && it.periodYear == state.year }
        val invoiceIdsForPeriod = invoicesForPeriod.map { it.id }.toSet()

        val allPayments = paymentRepository.getAll()
        val totalCollected = allPayments
            .filter { it.invoiceId in invoiceIdsForPeriod }
            .sumOf { it.amount }

        // Summed from the invoice and credit rows themselves - no pre-adjusted total is cached
        // anywhere, so a credit recorded after the fact is reflected the next time this is read.
        val creditsByInvoice = creditRepository.getAllOnce()
            .filter { it.invoiceId in invoiceIdsForPeriod }
            .groupBy { it.invoiceId }
        val totalCredits = invoicesForPeriod.sumOf { invoice ->
            invoice.amountDue - effectiveAmountDue(
                invoice.amountDue,
                creditsByInvoice[invoice.id].orEmpty().sumOf { it.amount },
            )
        }
        val totalDues = invoicesForPeriod
            .filter { it.status != InvoiceStatus.PAID }
            .sumOf { invoice ->
                val due = effectiveAmountDue(
                    invoice.amountDue,
                    creditsByInvoice[invoice.id].orEmpty().sumOf { it.amount },
                )
                (due - allPayments.filter { it.invoiceId == invoice.id }.sumOf { it.amount }).coerceAtLeast(0.0)
            }

        val totalExpense = expenseRepository.getByHostelId(id).first()
            .filter { expense ->
                val cal = Calendar.getInstance().apply { timeInMillis = expense.incurredOn }
                (cal.get(Calendar.MONTH) + 1) == state.month && cal.get(Calendar.YEAR) == state.year
            }
            .sumOf { it.amount }

        val reconciled = reconciliationRepository.getForPeriod(id, state.month, state.year) != null

        _uiState.update {
            it.copy(
                loading = false,
                reconciled = reconciled,
                occupancyPercent = occupancyPercent,
                totalCollected = totalCollected,
                totalDues = totalDues,
                totalCredits = totalCredits,
                totalExpense = totalExpense,
            )
        }
    }

    /** A manual attestation only: nothing is diffed, nothing is locked, and it can be taken back. */
    fun onReconciledChange(reconciled: Boolean) {
        val id = hostelId ?: return
        val state = _uiState.value
        viewModelScope.launch {
            if (reconciled) {
                reconciliationRepository.mark(id, state.month, state.year, note = null)
            } else {
                reconciliationRepository.unmark(id, state.month, state.year)
            }
            _uiState.update { it.copy(reconciled = reconciled) }
        }
    }
}
