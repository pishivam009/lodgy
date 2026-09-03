package com.lodgy.app.ui.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.effectiveAmountDue
import com.lodgy.app.data.entity.AgreementStatus
import com.lodgy.app.data.entity.BedStatus
import com.lodgy.app.data.entity.InvoiceStatus
import com.lodgy.app.data.prefs.HostelPreferences
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

/** Raw values; the screen formats them so currency and dates follow the warden's locale. */
data class PacketInvoiceData(
    val periodMonth: Int,
    val periodYear: Int,
    val amountDue: Double,
    val paid: Double,
    val status: InvoiceStatus,
)

data class PacketTenancyData(
    val tenantName: String,
    val phone: String,
    val roomNumber: String,
    val bedLabel: String,
    val active: Boolean,
    val agreedRent: Double,
    val moveInDate: Long,
    val moveOutDate: Long?,
    val invoices: List<PacketInvoiceData>,
)

data class PacketFloorData(val floorLabel: String, val tenancies: List<PacketTenancyData>)

data class PacketHostelData(
    val hostelName: String,
    val address: String,
    val totalBeds: Int,
    val occupiedBeds: Int,
    val floors: List<PacketFloorData>,
)

enum class PacketScope { CURRENT_HOSTEL, ALL_HOSTELS }

data class DataPacketUiState(
    val loading: Boolean = true,
    val scope: PacketScope = PacketScope.CURRENT_HOSTEL,
    val hostels: List<PacketHostelData> = emptyList(),
)

@HiltViewModel
class DataPacketViewModel @Inject constructor(
    private val hostelPreferences: HostelPreferences,
    private val hostelRepository: HostelRepository,
    private val floorRepository: FloorRepository,
    private val roomRepository: RoomRepository,
    private val bedRepository: BedRepository,
    private val tenancyAgreementRepository: TenancyAgreementRepository,
    private val tenantRepository: TenantRepository,
    private val invoiceRepository: InvoiceRepository,
    private val paymentRepository: PaymentRepository,
    private val creditRepository: CreditRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DataPacketUiState())
    val uiState: StateFlow<DataPacketUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun onScopeChange(scope: PacketScope) {
        _uiState.update { it.copy(scope = scope, loading = true) }
        refresh()
    }

    private fun refresh() {
        viewModelScope.launch {
            val scope = _uiState.value.scope
            val allHostels = hostelRepository.getAll().first()
            val selectedId = hostelPreferences.selectedHostelId.first()
            val hostels = when (scope) {
                PacketScope.ALL_HOSTELS -> allHostels
                PacketScope.CURRENT_HOSTEL -> allHostels.filter { it.id == selectedId }
            }

            val agreements = tenancyAgreementRepository.getAll()
            val payments = paymentRepository.getAll().groupBy { it.invoiceId }
            val credits = creditRepository.getAllOnce().groupBy { it.invoiceId }

            val packet = hostels.map { hostel ->
                var totalBeds = 0
                var occupiedBeds = 0
                val floors = floorRepository.getByHostelId(hostel.id).first()
                    .sortedBy { it.sortOrder }
                    .map { floor ->
                        val tenancies = roomRepository.getByFloorId(floor.id).first()
                            .sortedBy { it.roomNumber }
                            .flatMap { room ->
                                val beds = bedRepository.getByRoomId(room.id).first().sortedBy { it.label }
                                totalBeds += beds.size
                                occupiedBeds += beds.count { it.status == BedStatus.OCCUPIED }
                                beds.mapNotNull { bed ->
                                    // Latest agreement for the bed, so a bed whose tenant has
                                    // moved out still reports that tenancy rather than vanishing.
                                    val agreement = agreements
                                        .filter { it.bedId == bed.id }
                                        .maxByOrNull { it.moveInDate }
                                        ?: return@mapNotNull null
                                    val tenant = tenantRepository.getById(agreement.tenantId)
                                        ?: return@mapNotNull null
                                    val invoices = invoiceRepository.getByTenancyAgreementId(agreement.id)
                                        .first()
                                        .sortedWith(compareBy({ it.periodYear }, { it.periodMonth }))
                                        .map { invoice ->
                                            PacketInvoiceData(
                                                periodMonth = invoice.periodMonth,
                                                periodYear = invoice.periodYear,
                                                amountDue = effectiveAmountDue(
                                                    invoice.amountDue,
                                                    credits[invoice.id].orEmpty().sumOf { it.amount },
                                                ),
                                                paid = payments[invoice.id].orEmpty().sumOf { it.amount },
                                                status = invoice.status,
                                            )
                                        }
                                    PacketTenancyData(
                                        tenantName = tenant.name,
                                        phone = tenant.phone,
                                        roomNumber = room.roomNumber,
                                        bedLabel = bed.label,
                                        active = agreement.status == AgreementStatus.ACTIVE,
                                        agreedRent = agreement.agreedRent,
                                        moveInDate = agreement.moveInDate,
                                        moveOutDate = agreement.moveOutDate,
                                        invoices = invoices,
                                    )
                                }
                            }
                        PacketFloorData(floor.label, tenancies)
                    }
                PacketHostelData(hostel.name, hostel.address, totalBeds, occupiedBeds, floors)
            }

            _uiState.update { it.copy(loading = false, hostels = packet) }
        }
    }
}
