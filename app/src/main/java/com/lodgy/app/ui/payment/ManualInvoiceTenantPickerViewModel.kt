package com.lodgy.app.ui.payment

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

@HiltViewModel
class ManualInvoiceTenantPickerViewModel @Inject constructor(
    tenantRepository: TenantRepository,
) : ViewModel() {

    private val _activeTenants = MutableStateFlow<List<Tenant>>(emptyList())
    val activeTenants: StateFlow<List<Tenant>> = _activeTenants.asStateFlow()

    init {
        viewModelScope.launch {
            tenantRepository.getAll()
                .map { tenants -> tenants.filter { it.status == TenantStatus.ACTIVE } }
                .collect { _activeTenants.value = it }
        }
    }
}
