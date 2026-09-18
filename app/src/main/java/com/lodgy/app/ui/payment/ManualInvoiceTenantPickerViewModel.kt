package com.lodgy.app.ui.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.dao.BedLocation
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.TenancyAgreementRepository
import com.lodgy.app.data.repository.TenantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** One row per active tenancy, not per tenant - a tenant holding two beds must be billed on the
 *  one this manual invoice is actually for (LODGY-101). [location] is formatted at the Composable
 *  layer, since BedLocation.label() reads string resources. */
data class ManualInvoiceTenantRow(val tenantId: String, val bedId: String, val tenantName: String, val location: BedLocation?)

@HiltViewModel
class ManualInvoiceTenantPickerViewModel @Inject constructor(
    private val tenancyAgreementRepository: TenancyAgreementRepository,
    private val tenantRepository: TenantRepository,
    private val bedRepository: BedRepository,
) : ViewModel() {

    private val _rows = MutableStateFlow<List<ManualInvoiceTenantRow>>(emptyList())
    val rows: StateFlow<List<ManualInvoiceTenantRow>> = _rows.asStateFlow()

    init {
        viewModelScope.launch {
            val active = tenancyAgreementRepository.getAllActive()
            _rows.value = active.mapNotNull { agreement ->
                val tenant = tenantRepository.getById(agreement.tenantId) ?: return@mapNotNull null
                val location = bedRepository.getLocation(agreement.bedId)
                ManualInvoiceTenantRow(
                    tenantId = tenant.id,
                    bedId = agreement.bedId,
                    tenantName = tenant.name,
                    location = location,
                )
            }.sortedBy { it.tenantName }
        }
    }
}
