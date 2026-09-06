package com.lodgy.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.backup.AutoBackup
import com.lodgy.app.backup.BackupHealth
import com.lodgy.app.backup.backupHealth
import com.lodgy.app.data.entity.AgreementStatus
import com.lodgy.app.data.entity.BedStatus
import com.lodgy.app.data.entity.InvoiceStatus
import com.lodgy.app.data.prefs.BackupPreferences
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

data class UpcomingMoveOut(
    val tenantName: String,
    val moveOutDateMillis: Long,
    /** Which property this tenant is in. Shown because two hostels can both have a Room 101, so a
     *  bare name is ambiguous once the dashboard spans every property (LODGY-81). */
    val hostelName: String = "",
)

data class HostelOption(val id: String, val name: String)

data class DashboardUiState(
    val loading: Boolean = true,
    val hasActiveHostel: Boolean = false,
    /** Null means every hostel - the default. The figures below always describe whatever this
     *  says, and the UI must state which, because an unlabelled total that silently means "all
     *  properties" is worse than one that silently means "this property": it is larger and it
     *  looks right. */
    val filterHostelId: String? = null,
    val hostels: List<HostelOption> = emptyList(),
    val todaysCollections: Double = 0.0,
    val overdueInvoiceCount: Int = 0,
    val vacantBedCount: Int = 0,
    val upcomingMoveOuts: List<UpcomingMoveOut> = emptyList(),
    /** Automatic-backup status for the home tile (LODGY-68). Scoped to nothing - a backup covers the
     *  whole app, so it does not follow the hostel filter. */
    val backupHealth: BackupHealth = BackupHealth.NOT_CONFIGURED,
    val lastBackupTime: Long? = null,
    val backupRunning: Boolean = false,
) {
    val filterHostelName: String?
        get() = filterHostelId?.let { id -> hostels.firstOrNull { it.id == id }?.name }

    /** A configured folder is what turns the one-tap icon into "back up now" rather than "set up". */
    val backupConfigured: Boolean get() = backupHealth != BackupHealth.NOT_CONFIGURED
}

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
    private val backupPreferences: BackupPreferences,
    private val autoBackup: AutoBackup,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { loadMetrics(_uiState.value.filterHostelId) }
        viewModelScope.launch { loadBackupHealth() }
    }

    /** Narrow to one hostel, or pass null for every hostel. The selected-hostel preference is left
     *  alone: it still drives the property screens, and the dashboard no longer follows it. */
    fun onHostelFilterChange(hostelId: String?) {
        _uiState.update { it.copy(filterHostelId = hostelId) }
        viewModelScope.launch { loadMetrics(hostelId) }
    }

    /** Bottom-nav keeps this ViewModel alive across tab switches, so metrics computed from
     *  one-shot fetches go stale the moment something changes elsewhere (a checkout, a payment)
     *  without the selected hostel itself changing. Call this when the screen re-enters view. */
    fun refresh() {
        viewModelScope.launch { loadMetrics(_uiState.value.filterHostelId) }
        viewModelScope.launch { loadBackupHealth() }
    }

    /** The dashboard's one-tap backup. Forces a run even if nothing has changed - a warden pressing
     *  it wants a file now - then refreshes the tile from what the run recorded. */
    fun backupNow() {
        if (_uiState.value.backupRunning) return
        _uiState.update { it.copy(backupRunning = true) }
        viewModelScope.launch {
            autoBackup.run(force = true)
            loadBackupHealth()
            _uiState.update { it.copy(backupRunning = false) }
        }
    }

    private suspend fun loadBackupHealth() {
        val folder = backupPreferences.folderUri.first()
        val lastSuccess = backupPreferences.lastSuccessTime.first()
        val failed = backupPreferences.lastAttemptFailed.first()
        _uiState.update {
            it.copy(
                backupHealth = backupHealth(
                    folderConfigured = folder != null,
                    lastSuccessTime = lastSuccess,
                    lastAttemptFailed = failed,
                    now = System.currentTimeMillis(),
                ),
                lastBackupTime = lastSuccess,
            )
        }
    }

    /** [hostelId] null means every hostel, which is the default a multi-property warden wants. */
    private suspend fun loadMetrics(hostelId: String?) {
        val allHostels = hostelRepository.getAll().first()
        val hostelOptions = allHostels.map { HostelOption(it.id, it.name) }
        if (allHostels.isEmpty()) {
            // Nothing to aggregate, and no reason to walk the repositories for it.
            _uiState.update { it.copy(loading = false, hasActiveHostel = false, hostels = emptyList()) }
            return
        }
        val scope = if (hostelId == null) allHostels else allHostels.filter { it.id == hostelId }

        // Bed -> hostel, kept so move-outs can name the property a tenant is in.
        val hostelNameByBedId = mutableMapOf<String, String>()
        val bedsInHostel = scope.flatMap { hostel ->
            floorRepository.getByHostelId(hostel.id).first()
                .flatMap { floor -> roomRepository.getByFloorId(floor.id).first() }
                .flatMap { room -> bedRepository.getByRoomId(room.id).first() }
                .onEach { hostelNameByBedId[it.id] = hostel.name }
        }
        val bedIdsInHostel = bedsInHostel.map { it.id }.toSet()
        val vacantBedCount = bedsInHostel.count { it.status == BedStatus.VACANT }

        val agreementsInHostel = tenancyAgreementRepository.getAll()
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
            .map { agreement ->
                UpcomingMoveOut(
                    tenantName = tenantRepository.getById(agreement.tenantId)?.name.orEmpty(),
                    moveOutDateMillis = agreement.moveOutDate!!,
                    hostelName = hostelNameByBedId[agreement.bedId].orEmpty(),
                )
            }

        _uiState.update {
            it.copy(
                loading = false,
                hasActiveHostel = allHostels.isNotEmpty(),
                hostels = hostelOptions,
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
