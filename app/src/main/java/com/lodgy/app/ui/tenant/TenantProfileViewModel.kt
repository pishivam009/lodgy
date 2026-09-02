package com.lodgy.app.ui.tenant

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.entity.Tenant
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
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val tenantId: String = checkNotNull(savedStateHandle["tenantId"])

    private val _tenant = MutableStateFlow<Tenant?>(null)
    val tenant: StateFlow<Tenant?> = _tenant.asStateFlow()

    init {
        viewModelScope.launch {
            tenantRepository.observeById(tenantId).collect { _tenant.value = it }
        }
    }
}
