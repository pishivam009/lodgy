package com.lodgy.app.ui.property

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.entity.Hostel
import com.lodgy.app.data.repository.HostelRepository
import com.lodgy.app.data.repository.WardenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HostelFormUiState(
    val isEditing: Boolean = false,
    val name: String = "",
    val address: String = "",
    val contactPhone: String = "",
    val saved: Boolean = false,
)

@HiltViewModel
class HostelFormViewModel @Inject constructor(
    private val hostelRepository: HostelRepository,
    private val wardenRepository: WardenRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val hostelId: String? = savedStateHandle["hostelId"]
    private var existingHostel: Hostel? = null

    private val _uiState = MutableStateFlow(HostelFormUiState(isEditing = hostelId != null))
    val uiState: StateFlow<HostelFormUiState> = _uiState.asStateFlow()

    init {
        val id = hostelId
        if (id != null) {
            viewModelScope.launch {
                val hostel = hostelRepository.getById(id) ?: return@launch
                existingHostel = hostel
                _uiState.update {
                    it.copy(name = hostel.name, address = hostel.address, contactPhone = hostel.contactPhone)
                }
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value) }
    fun onAddressChange(value: String) = _uiState.update { it.copy(address = value) }
    fun onContactPhoneChange(value: String) = _uiState.update { it.copy(contactPhone = value) }

    fun save() {
        val state = _uiState.value
        if (state.name.isBlank()) return
        viewModelScope.launch {
            val existing = existingHostel
            if (existing != null) {
                hostelRepository.update(existing, state.name, state.address, state.contactPhone)
            } else {
                val warden = wardenRepository.getWarden() ?: return@launch
                hostelRepository.create(warden.id, state.name, state.address, state.contactPhone)
            }
            _uiState.update { it.copy(saved = true) }
        }
    }
}
