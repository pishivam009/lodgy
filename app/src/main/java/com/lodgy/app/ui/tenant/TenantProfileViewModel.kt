package com.lodgy.app.ui.tenant

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.dao.BedLocation
import com.lodgy.app.data.entity.AgreementStatus
import com.lodgy.app.data.entity.Tenant
import com.lodgy.app.data.entity.TenancyAgreement
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.TenancyAgreementRepository
import com.lodgy.app.data.repository.TenantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** How long a tenant has been living here, from a tenancy - "living here since" while
 *  [active], "lived here for" once it has closed (LODGY-92). */
data class StayDuration(val fromMillis: Long, val toMillis: Long, val active: Boolean)

/** One tenancy's worth of what the profile screen shows and acts on - a tenant with beds in two
 *  rooms has two of these, each independently checked out, transferred or notified (LODGY-101).
 *  bedId rather than the agreement id is what nav routes carry, since getActiveByBedId is the
 *  already-unambiguous lookup every action screen resolves against. */
data class TenancyEntry(
    val bedId: String,
    val location: BedLocation?,
    val plannedMoveOut: Long?,
    val nonRevenue: Boolean,
    val stayDuration: StayDuration,
)

@HiltViewModel
class TenantProfileViewModel @Inject constructor(
    tenantRepository: TenantRepository,
    private val tenancyAgreementRepository: TenancyAgreementRepository,
    private val bedRepository: BedRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val tenantId: String = checkNotNull(savedStateHandle["tenantId"])

    private val _tenant = MutableStateFlow<Tenant?>(null)
    val tenant: StateFlow<Tenant?> = _tenant.asStateFlow()

    /** Every currently active tenancy for this tenant - normally one, but genuinely more for a
     *  tenant holding several beds. Empty once every tenancy has closed. */
    private val _activeTenancies = MutableStateFlow<List<TenancyEntry>>(emptyList())
    val activeTenancies: StateFlow<List<TenancyEntry>> = _activeTenancies.asStateFlow()

    /** Shown only when [activeTenancies] is empty, so a checked-out tenant still resolves to the
     *  room/bed and dates they last had, same as before this ticket for the common case. */
    private val _lastClosedTenancy = MutableStateFlow<TenancyEntry?>(null)
    val lastClosedTenancy: StateFlow<TenancyEntry?> = _lastClosedTenancy.asStateFlow()

    init {
        viewModelScope.launch {
            tenantRepository.observeById(tenantId).collect { _tenant.value = it }
        }
        // Observed rather than read once: onboarding writes the agreement after the tenant row,
        // and a transfer changes only bedId, so a single read at init would leave the room/bed
        // label blank on onboarding and stale after a move.
        viewModelScope.launch {
            tenancyAgreementRepository.observeByTenantId(tenantId).collect { agreements ->
                val active = agreements.filter { it.status == AgreementStatus.ACTIVE }
                if (active.isNotEmpty()) {
                    _activeTenancies.value = active
                        .sortedByDescending { it.moveInDate }
                        .map { it.toEntry(active = true) }
                    _lastClosedTenancy.value = null
                } else {
                    _activeTenancies.value = emptyList()
                    _lastClosedTenancy.value = agreements.latest()?.toEntry(active = false)
                }
            }
        }
    }

    private suspend fun TenancyAgreement.toEntry(active: Boolean): TenancyEntry = TenancyEntry(
        bedId = bedId,
        location = bedRepository.getLocation(bedId),
        plannedMoveOut = if (active) moveOutDate else null,
        nonRevenue = nonRevenue,
        stayDuration = if (active) {
            StayDuration(moveInDate, System.currentTimeMillis(), active = true)
        } else {
            StayDuration(moveInDate, moveOutDate ?: moveInDate, active = false)
        },
    )

    fun setPlannedMoveOut(bedId: String, millis: Long?) {
        viewModelScope.launch {
            val agreement = tenancyAgreementRepository.getActiveByBedId(bedId) ?: return@launch
            tenancyAgreementRepository.setPlannedMoveOut(agreement, millis)
        }
    }
}

/** Same rule as TenancyAgreementDao.getLatestByTenantId: the active agreement if there is one,
 *  else the most recent closed one, so a vacated tenant still resolves to their last bed. */
internal fun List<TenancyAgreement>.latest(): TenancyAgreement? =
    minWithOrNull(
        compareBy({ it.status != AgreementStatus.ACTIVE }, { -it.moveInDate }),
    )
