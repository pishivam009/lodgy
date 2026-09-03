package com.lodgy.app.ui.tenant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.dao.BedLocation
import com.lodgy.app.data.entity.Tenant
import com.lodgy.app.data.entity.TenantStatus
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.TenancyAgreementRepository
import com.lodgy.app.data.repository.TenantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class TenantDirectoryItem(
    val tenant: Tenant,
    val location: BedLocation?,
)

enum class TenantFilter { ACTIVE, ALL }

enum class TenantSort { NAME, ROOM }

data class TenantDirectoryUiState(
    val query: String = "",
    val filter: TenantFilter = TenantFilter.ACTIVE,
    val sort: TenantSort = TenantSort.NAME,
    val items: List<TenantDirectoryItem> = emptyList(),
    /** Surfaced in the UI so an Active-only list never looks like the whole list. */
    val hiddenByFilter: Int = 0,
)

/** "204" before "1005": a plain string sort puts 1005 first, which is not how a warden walks a
 *  corridor. Rooms with a leading number sort numerically, then text-labelled ones like "G-2",
 *  then tenants with no room at all. */
private fun roomSortKey(location: BedLocation?): String {
    if (location == null) return "2"
    val tail = "${location.roomNumber}${location.bedLabel}"
    val leadingDigits = location.roomNumber.takeWhile { it.isDigit() }
    return if (leadingDigits.isEmpty()) "1$tail" else "0${leadingDigits.padStart(6, '0')}$tail"
}

@HiltViewModel
class TenantDirectoryViewModel @Inject constructor(
    tenantRepository: TenantRepository,
    private val tenancyAgreementRepository: TenancyAgreementRepository,
    private val bedRepository: BedRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(TenantFilter.ACTIVE)
    private val sort = MutableStateFlow(TenantSort.NAME)
    private val allItems = MutableStateFlow<List<TenantDirectoryItem>>(emptyList())

    private val _uiState = MutableStateFlow(TenantDirectoryUiState())
    val uiState: StateFlow<TenantDirectoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            tenantRepository.getAll().collect { tenants ->
                allItems.value = tenants.map { TenantDirectoryItem(it, locationOf(it.id)) }
            }
        }
        viewModelScope.launch {
            combine(allItems, query, filter, sort) { items, q, activeFilter, activeSort ->
                val matchingQuery = if (q.isBlank()) {
                    items
                } else {
                    items.filter {
                        it.tenant.name.contains(q, ignoreCase = true) || it.tenant.phone.contains(q)
                    }
                }
                val visible = when (activeFilter) {
                    TenantFilter.ALL -> matchingQuery
                    TenantFilter.ACTIVE -> matchingQuery.filter { it.tenant.status == TenantStatus.ACTIVE }
                }
                val sorted = when (activeSort) {
                    TenantSort.NAME -> visible.sortedBy { it.tenant.name.lowercase() }
                    TenantSort.ROOM -> visible.sortedBy { roomSortKey(it.location) }
                }
                TenantDirectoryUiState(
                    query = q,
                    filter = activeFilter,
                    sort = activeSort,
                    items = sorted,
                    hiddenByFilter = matchingQuery.size - visible.size,
                )
            }.collect { _uiState.value = it }
        }
    }

    private suspend fun locationOf(tenantId: String): BedLocation? {
        val agreement = tenancyAgreementRepository.getLatestByTenantId(tenantId) ?: return null
        return bedRepository.getLocation(agreement.bedId)
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onFilterChange(value: TenantFilter) {
        filter.value = value
    }

    fun onSortChange(value: TenantSort) {
        sort.value = value
    }
}
