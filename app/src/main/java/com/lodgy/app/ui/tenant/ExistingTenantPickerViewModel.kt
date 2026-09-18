package com.lodgy.app.ui.tenant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.entity.Tenant
import com.lodgy.app.data.entity.TenantStatus
import com.lodgy.app.data.repository.TenantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Onboarding a second bed for a tenant who already lives here (LODGY-101): picks from active
 *  tenants rather than creating a duplicate record. */
@HiltViewModel
class ExistingTenantPickerViewModel @Inject constructor(
    tenantRepository: TenantRepository,
) : ViewModel() {

    private val _activeTenants = MutableStateFlow<List<Tenant>>(emptyList())
    val activeTenants: StateFlow<List<Tenant>> = _activeTenants.asStateFlow()

    init {
        viewModelScope.launch {
            tenantRepository.getAll()
                .map { tenants -> tenants.filter { it.status == TenantStatus.ACTIVE }.sortedBy { it.name } }
                .collect { _activeTenants.value = it }
        }
    }
}
