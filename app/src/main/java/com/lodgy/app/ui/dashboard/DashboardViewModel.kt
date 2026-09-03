package com.lodgy.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.entity.AgreementStatus
import com.lodgy.app.data.entity.BedStatus
import com.lodgy.app.data.entity.InvoiceStatus
import com.lodgy.app.data.prefs.HostelPreferences
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.FloorRepository
import com.lodgy.app.data.repository.HostelRepository
import com.lodgy.app.data.repository.InvoiceRepository
import com.lodgy.app.data.repository.PaymentRepository
import com.lodgy.app.data.repository.RoomRepository
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

data class UpcomingMoveOut(val tenantName: String, val moveOutDateMillis: Long)

data class DashboardUiState(
    val loading: Boolean = true,
    val hasActiveHostel: Boolean = false,
    val hostelName: String = "",
    val todaysCollections: Double = 0.0,
    val overdueInvoiceCount: Int = 0,
    val vacantBedCount: Int = 0,
    val upcomingMoveOuts: List<UpcomingMoveOut> = emptyList(),
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val hostelPreferences: HostelPreferences,
    private val hostelRepository: HostelRepository,
    private val floorRepository: FloorRepository,
    private val roomRepository: RoomRepository,
    private val bedRepository: BedRepository,
    private val tenancyAgreementRepository: TenancyAgreementRepository,
    private val invoiceRepository: InvoiceRepository,
    private val paymentRepository: PaymentRepository,
    private val tenantRepository: TenantRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            hostelPreferences.selectedHostelId.collect { hostelId ->
                if (hostelId == null) {
                    _uiState.update { it.copy(loading = false, hasActiveHostel = false) }
                } else {
                    loadMetrics(hostelId)
                }
            }
        }
    }

    private suspend fun loadMetrics(hostelId: String) {
        val hostel = hostelRepository.getById(hostelId)

        val bedsInHostel = floorRepository.getByHostelId(hostelId).first()
            .flatMap { floor -> roomRepository.getByFloorId(floor.id).first() }
            .flatMap { room -> bedRepository.getByRoomId(room.id).first() }
        val bedIdsInHostel = bedsInHostel.map { it.id }.toSet()
        val vacantBedCount = bedsInHostel.count { it.status == BedStatus.VACANT }

        val agreementsInHostel = tenancyAgreementRepository.getAllActive()
            .filter { it.bedId in bedIdsInHostel }
        val agreementIds = agreementsInHostel.map { it.id }.toSet()

        val allInvoices = invoiceRepository.getAll().first()
        val invoicesInHostel = allInvoices.filter { it.tenancyAgreementId in agreementIds }
        val invoiceIdsInHostel = invoicesInHostel.map { it.id }.toSet()

        val now = Calendar.getInstance()
        val overdueCount = invoicesInHostel.count { it.status != InvoiceStatus.PAID && it.dueDate < startOfDay(now) }

        val allPayments = paymentRepository.getAll()
        val todaysCollections = allPayments
            .filter { it.invoiceId in invoiceIdsInHostel && isSameDay(it.paidOn, now) }
            .sumOf { it.amount }

        val upcomingMoveOuts = agreementsInHostel
            .filter { it.status == AgreementStatus.ACTIVE && it.moveOutDate != null && it.moveOutDate > now.timeInMillis }
            .sortedBy { it.moveOutDate }
            .map { agreement -> UpcomingMoveOut(tenantRepository.getById(agreement.tenantId)?.name.orEmpty(), agreement.moveOutDate!!) }

        _uiState.update {
            it.copy(
                loading = false,
                hasActiveHostel = true,
                hostelName = hostel?.name.orEmpty(),
                todaysCollections = todaysCollections,
                overdueInvoiceCount = overdueCount,
                vacantBedCount = vacantBedCount,
                upcomingMoveOuts = upcomingMoveOuts,
            )
        }
    }

    private fun startOfDay(calendar: Calendar): Long {
        val copy = calendar.clone() as Calendar
        copy.set(Calendar.HOUR_OF_DAY, 0)
        copy.set(Calendar.MINUTE, 0)
        copy.set(Calendar.SECOND, 0)
        copy.set(Calendar.MILLISECOND, 0)
        return copy.timeInMillis
    }

    private fun isSameDay(millis: Long, today: Calendar): Boolean {
        val other = Calendar.getInstance().apply { timeInMillis = millis }
        return other.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            other.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    }
}
