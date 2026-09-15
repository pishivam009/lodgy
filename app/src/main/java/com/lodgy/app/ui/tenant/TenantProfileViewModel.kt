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

/** How long a tenant has been living here, from their latest tenancy - "living here since" while
 *  [active], "lived here for" once it has closed (LODGY-92). */
data class StayDuration(val fromMillis: Long, val toMillis: Long, val active: Boolean)

@HiltViewModel
class TenantProfileViewModel @Inject constructor(
    tenantRepository: TenantRepository,
    private val tenancyAgreementRepository: TenancyAgreementRepository,
    bedRepository: BedRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val tenantId: String = checkNotNull(savedStateHandle["tenantId"])

    private val _tenant = MutableStateFlow<Tenant?>(null)
    val tenant: StateFlow<Tenant?> = _tenant.asStateFlow()

    private val _location = MutableStateFlow<BedLocation?>(null)
    val location: StateFlow<BedLocation?> = _location.asStateFlow()

    private val _plannedMoveOut = MutableStateFlow<Long?>(null)
    val plannedMoveOut: StateFlow<Long?> = _plannedMoveOut.asStateFlow()

    /** Gates the forgone-rent action, which is meaningless on a paying tenancy (LODGY-84). */
    private val _nonRevenue = MutableStateFlow(false)
    val nonRevenue: StateFlow<Boolean> = _nonRevenue.asStateFlow()

    private val _stayDuration = MutableStateFlow<StayDuration?>(null)
    val stayDuration: StateFlow<StayDuration?> = _stayDuration.asStateFlow()

    init {
        viewModelScope.launch {
            tenantRepository.observeById(tenantId).collect { _tenant.value = it }
        }
        // Observed rather than read once: onboarding writes the agreement after the tenant row,
        // and a transfer changes only bedId, so a single read at init would leave the room/bed
        // label blank on onboarding and stale after a move.
        viewModelScope.launch {
            tenancyAgreementRepository.observeByTenantId(tenantId).collect { agreements ->
                val agreement = agreements.latest()
                _location.value = agreement?.let { bedRepository.getLocation(it.bedId) }
                val active = agreement?.takeIf { it.status == AgreementStatus.ACTIVE }
                _plannedMoveOut.value = active?.moveOutDate
                _nonRevenue.value = active?.nonRevenue == true
                _stayDuration.value = agreement?.let {
                    if (it.status == AgreementStatus.ACTIVE) {
                        StayDuration(it.moveInDate, System.currentTimeMillis(), active = true)
                    } else {
                        StayDuration(it.moveInDate, it.moveOutDate ?: it.moveInDate, active = false)
                    }
                }
            }
        }
    }

    fun setPlannedMoveOut(millis: Long?) {
        viewModelScope.launch {
            val agreement = tenancyAgreementRepository.getActiveByTenantId(tenantId) ?: return@launch
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
