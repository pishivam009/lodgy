package com.lodgy.app.ui.tenant

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.entity.Tenant
import com.lodgy.app.data.repository.CreditRepository
import com.lodgy.app.data.repository.TenancyAgreementRepository
import com.lodgy.app.data.repository.TenantRepository
import com.lodgy.app.media.PhotoStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PhotoField { PROFILE, ID_PROOF }

data class TenantFormUiState(
    val isEditing: Boolean = false,
    val name: String = "",
    val phone: String = "",
    val photoPath: String? = null,
    val idProofPhotoPath: String? = null,
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = "",
    val saved: Boolean = false,
    val savedTenantId: String? = null,
    /** Delete a tenant added in error (LODGY-64); confirmed first (LODGY-57), and blocked while the
     *  tenant has any tenancy or credit so their history is never broken (LODGY-63 pattern). */
    val pendingDelete: Boolean = false,
    val blockedDelete: Boolean = false,
    val deleted: Boolean = false,
) {
    val canSave: Boolean get() = name.isNotBlank() && phone.isNotBlank()
}

@HiltViewModel
class TenantFormViewModel @Inject constructor(
    private val tenantRepository: TenantRepository,
    private val tenancyAgreementRepository: TenancyAgreementRepository,
    private val creditRepository: CreditRepository,
    private val photoStorage: PhotoStorage,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val tenantId: String? = savedStateHandle["tenantId"]
    private var existingTenant: Tenant? = null

    private val _uiState = MutableStateFlow(TenantFormUiState(isEditing = tenantId != null))
    val uiState: StateFlow<TenantFormUiState> = _uiState.asStateFlow()

    init {
        val id = tenantId
        if (id != null) {
            viewModelScope.launch {
                val tenant = tenantRepository.getById(id) ?: return@launch
                existingTenant = tenant
                _uiState.update {
                    it.copy(
                        name = tenant.name,
                        phone = tenant.phone,
                        photoPath = tenant.photoPath,
                        idProofPhotoPath = tenant.idProofPhotoPath,
                        emergencyContactName = tenant.emergencyContactName,
                        emergencyContactPhone = tenant.emergencyContactPhone,
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value) }
    fun onPhoneChange(value: String) = _uiState.update { it.copy(phone = value) }
    fun onEmergencyNameChange(value: String) = _uiState.update { it.copy(emergencyContactName = value) }
    fun onEmergencyPhoneChange(value: String) = _uiState.update { it.copy(emergencyContactPhone = value) }

    fun createCameraOutputUri(): Uri = photoStorage.createCameraOutputUri()

    fun onPhotoPicked(field: PhotoField, uri: Uri) {
        viewModelScope.launch {
            val path = photoStorage.persist(uri)
            _uiState.update {
                when (field) {
                    PhotoField.PROFILE -> it.copy(photoPath = path)
                    PhotoField.ID_PROOF -> it.copy(idProofPhotoPath = path)
                }
            }
        }
    }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        viewModelScope.launch {
            val existing = existingTenant
            val savedId = if (existing != null) {
                tenantRepository.update(
                    existing,
                    state.name,
                    state.phone,
                    state.photoPath,
                    state.idProofPhotoPath,
                    state.emergencyContactName,
                    state.emergencyContactPhone,
                )
                existing.id
            } else {
                tenantRepository.create(
                    state.name,
                    state.phone,
                    state.photoPath,
                    state.idProofPhotoPath,
                    state.emergencyContactName,
                    state.emergencyContactPhone,
                ).id
            }
            _uiState.update { it.copy(saved = true, savedTenantId = savedId) }
        }
    }

    fun requestDelete() {
        val tenant = existingTenant ?: return
        viewModelScope.launch {
            val hasAgreements = tenancyAgreementRepository.observeByTenantId(tenant.id).first().isNotEmpty()
            val hasCredits = creditRepository.getByTenantId(tenant.id).first().isNotEmpty()
            _uiState.update {
                if (hasAgreements || hasCredits) it.copy(blockedDelete = true) else it.copy(pendingDelete = true)
            }
        }
    }

    fun dismissDelete() = _uiState.update { it.copy(pendingDelete = false, blockedDelete = false) }

    fun confirmDelete() {
        val tenant = existingTenant ?: return
        viewModelScope.launch {
            tenantRepository.delete(tenant)
            _uiState.update { it.copy(pendingDelete = false, deleted = true) }
        }
    }
}
