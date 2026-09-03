package com.lodgy.app.ui.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.dao.BedLocation
import com.lodgy.app.data.effectiveAmountDue
import com.lodgy.app.data.entity.Payment
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

    private val _uiState = MutableStateFlow(AcknowledgementUiState())
    val uiState: StateFlow<AcknowledgementUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val invoice = invoiceRepository.getById(invoiceId)
            if (invoice == null) {
                _uiState.update { it.copy(loading = false, found = false) }
                return@launch
            }
            val agreement = tenancyAgreementRepository.getById(invoice.tenancyAgreementId)
            val tenant = agreement?.let { tenantRepository.getById(it.tenantId) }
            val bed = agreement?.let { bedRepository.getById(it.bedId) }
            val room = bed?.let { roomRepository.getById(it.roomId) }
            val floor = room?.let { floorRepository.getById(it.floorId) }
            val hostel = floor?.let { hostelRepository.getById(it.hostelId) }
            val payments = paymentRepository.getByInvoiceId(invoiceId).first().sortedBy { it.paidOn }

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
                    creditTotal = creditRepository.getByInvoiceId(invoiceId).sumOf { credit -> credit.amount },
                    totalPaid = payments.sumOf { payment -> payment.amount },
                    payments = payments,
                )
            }
        }
    }
}
