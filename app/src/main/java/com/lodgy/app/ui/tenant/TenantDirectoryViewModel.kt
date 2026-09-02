package com.lodgy.app.ui.tenant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.entity.Tenant
import com.lodgy.app.data.repository.TenantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class TenantDirectoryUiState(
    val query: String = "",
    val tenants: List<Tenant> = emptyList(),
)

@HiltViewModel
class TenantDirectoryViewModel @Inject constructor(
    tenantRepository: TenantRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val allTenants = MutableStateFlow<List<Tenant>>(emptyList())

    private val _uiState = MutableStateFlow(TenantDirectoryUiState())
    val uiState: StateFlow<TenantDirectoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            tenantRepository.getAll().collect { allTenants.value = it }
        }
        viewModelScope.launch {
            combine(allTenants, query) { tenants, q ->
                val filtered = if (q.isBlank()) {
                    tenants
                } else {
                    tenants.filter { it.name.contains(q, ignoreCase = true) || it.phone.contains(q) }
                }
                TenantDirectoryUiState(query = q, tenants = filtered)
            }.collect { _uiState.value = it }
        }
    }

    fun onQueryChange(value: String) {
        query.value = value
    }
}
