package com.lodgy.app.ui.tenant

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.dao.BedLocation
import com.lodgy.app.data.entity.AgreementStatus
import com.lodgy.app.data.entity.Tenant
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.TenancyAgreementRepository
import com.lodgy.app.data.repository.TenantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    init {
        viewModelScope.launch {
            tenantRepository.observeById(tenantId).collect { _tenant.value = it }
        }
        viewModelScope.launch {
            val agreement = tenancyAgreementRepository.getLatestByTenantId(tenantId) ?: return@launch
            _location.value = bedRepository.getLocation(agreement.bedId)
            if (agreement.status == AgreementStatus.ACTIVE) {
                _plannedMoveOut.value = agreement.moveOutDate
            }
        }
    }

    fun setPlannedMoveOut(millis: Long?) {
        viewModelScope.launch {
            val agreement = tenancyAgreementRepository.getActiveByTenantId(tenantId) ?: return@launch
            tenancyAgreementRepository.setPlannedMoveOut(agreement, millis)
            _plannedMoveOut.value = millis
        }
    }
}
