package com.lodgy.app.ui.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.dao.BedLocation
import com.lodgy.app.data.effectiveAmountDue
import com.lodgy.app.data.entity.Credit
import com.lodgy.app.data.entity.Invoice
import com.lodgy.app.data.entity.Payment
import com.lodgy.app.data.invoiceStatusFor
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.CreditRepository
import com.lodgy.app.data.repository.FloorRepository
import com.lodgy.app.data.repository.HostelRepository
import com.lodgy.app.data.repository.InvoiceRepository
import com.lodgy.app.data.repository.PaymentRepository
import com.lodgy.app.data.repository.RoomRepository
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

data class AcknowledgementUiState(
    val loading: Boolean = true,
    val found: Boolean = false,
    val hostelName: String = "",
    val tenantName: String = "",
    val location: BedLocation? = null,
    val periodMonth: Int = 0,
    val periodYear: Int = 0,
    val invoiceAmount: Double = 0.0,
    val creditTotal: Double = 0.0,
    val totalPaid: Double = 0.0,
    val payments: List<Payment> = emptyList(),
    val credits: List<Credit> = emptyList(),
    /** Correction actions (LODGY-64). Each is confirmed before it runs (LODGY-57). */
    val pendingDeletePayment: Payment? = null,
    val pendingDeleteCredit: Credit? = null,
    val pendingDeleteInvoice: Boolean = false,
    /** An invoice with payments or credits still on it is blocked from deletion, not orphaned. */
    val blockedDeleteInvoice: Boolean = false,
    /** The invoice is gone; the caller should leave this screen. */
    val invoiceDeleted: Boolean = false,
) {
    val amountDue: Double get() = effectiveAmountDue(invoiceAmount, creditTotal)

    /** What the tenant still owes; never negative, an overpayment reads as settled. */
    val balance: Double get() = (amountDue - totalPaid).coerceAtLeast(0.0)
}

@HiltViewModel
class AcknowledgementViewModel @Inject constructor(
    private val invoiceRepository: InvoiceRepository,
    private val paymentRepository: PaymentRepository,
    private val creditRepository: CreditRepository,
    private val tenancyAgreementRepository: TenancyAgreementRepository,
    private val tenantRepository: TenantRepository,
    private val bedRepository: BedRepository,
    private val roomRepository: RoomRepository,
    private val floorRepository: FloorRepository,
    private val hostelRepository: HostelRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val invoiceId: String = checkNotNull(savedStateHandle["invoiceId"])
    private var invoice: Invoice? = null

    private val _uiState = MutableStateFlow(AcknowledgementUiState())
    val uiState: StateFlow<AcknowledgementUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val invoice = invoiceRepository.getById(invoiceId)
        this.invoice = invoice
        if (invoice == null) {
            _uiState.update { it.copy(loading = false, found = false) }
            return
        }
        val agreement = tenancyAgreementRepository.getById(invoice.tenancyAgreementId)
        val tenant = agreement?.let { tenantRepository.getById(it.tenantId) }
        val bed = agreement?.let { bedRepository.getById(it.bedId) }
        val room = bed?.let { roomRepository.getById(it.roomId) }
        val floor = room?.let { floorRepository.getById(it.floorId) }
        val hostel = floor?.let { hostelRepository.getById(it.hostelId) }
        val payments = paymentRepository.getByInvoiceId(invoiceId).first().sortedBy { it.paidOn }
        val credits = creditRepository.getByInvoiceId(invoiceId).sortedBy { it.createdAt }

        _uiState.update {
            it.copy(
                loading = false,
                found = true,
                hostelName = hostel?.name.orEmpty(),
                tenantName = tenant?.name.orEmpty(),
                location = agreement?.let { a -> bedRepository.getLocation(a.bedId) },
                periodMonth = invoice.periodMonth,
                periodYear = invoice.periodYear,
                invoiceAmount = invoice.amountDue,
                creditTotal = credits.sumOf { credit -> credit.amount },
                totalPaid = payments.sumOf { payment -> payment.amount },
                payments = payments,
                credits = credits,
            )
        }
    }

    // --- Payment delete: removes the money AND corrects the invoice status in the same run, so an
    // invoice can never be left reading PAID with nothing behind it (LODGY-64, AC3).
    fun requestDeletePayment(payment: Payment) = _uiState.update { it.copy(pendingDeletePayment = payment) }
    fun dismissDeletePayment() = _uiState.update { it.copy(pendingDeletePayment = null) }

    fun confirmDeletePayment() {
        val payment = _uiState.value.pendingDeletePayment ?: return
        viewModelScope.launch {
            paymentRepository.delete(payment)
            recalculateInvoiceStatus()
            _uiState.update { it.copy(pendingDeletePayment = null) }
            load()
        }
    }

    // --- Credit delete: a credit lowers what is owed, so removing it can push an invoice back to
    // unpaid; recalculate the status the same way (LODGY-64).
    fun requestDeleteCredit(credit: Credit) = _uiState.update { it.copy(pendingDeleteCredit = credit) }
    fun dismissDeleteCredit() = _uiState.update { it.copy(pendingDeleteCredit = null) }

    fun confirmDeleteCredit() {
        val credit = _uiState.value.pendingDeleteCredit ?: return
        viewModelScope.launch {
            creditRepository.delete(credit)
            recalculateInvoiceStatus()
            _uiState.update { it.copy(pendingDeleteCredit = null) }
            load()
        }
    }

    // --- Invoice delete: blocked while any payment or credit still points at it (AC4).
    fun requestDeleteInvoice() {
        val state = _uiState.value
        if (state.payments.isNotEmpty() || state.credits.isNotEmpty()) {
            _uiState.update { it.copy(blockedDeleteInvoice = true) }
        } else {
            _uiState.update { it.copy(pendingDeleteInvoice = true) }
        }
    }

    fun dismissDeleteInvoice() = _uiState.update { it.copy(pendingDeleteInvoice = false, blockedDeleteInvoice = false) }

    fun confirmDeleteInvoice() {
        val invoice = invoice ?: return
        viewModelScope.launch {
            invoiceRepository.delete(invoice)
            _uiState.update { it.copy(pendingDeleteInvoice = false, invoiceDeleted = true) }
        }
    }

    private suspend fun recalculateInvoiceStatus() {
        val invoice = invoice ?: return
        val totalPaid = paymentRepository.getTotalPaid(invoiceId)
        val due = effectiveAmountDue(
            invoice.amountDue,
            creditRepository.getByInvoiceId(invoiceId).sumOf { it.amount },
        )
        invoiceRepository.updateStatus(invoice, invoiceStatusFor(due, totalPaid))
    }
}
