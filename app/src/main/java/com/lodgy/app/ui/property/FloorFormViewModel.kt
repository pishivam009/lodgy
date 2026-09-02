package com.lodgy.app.ui.property

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.entity.Floor
import com.lodgy.app.data.repository.FloorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FloorFormUiState(
    val isEditing: Boolean = false,
    val label: String = "",
    val saved: Boolean = false,
)

@HiltViewModel
class FloorFormViewModel @Inject constructor(
    private val floorRepository: FloorRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val hostelId: String = checkNotNull(savedStateHandle["hostelId"])
    private val floorId: String? = savedStateHandle["floorId"]
    private var existingFloor: Floor? = null

    private val _uiState = MutableStateFlow(FloorFormUiState(isEditing = floorId != null))
    val uiState: StateFlow<FloorFormUiState> = _uiState.asStateFlow()

    init {
        val id = floorId
        if (id != null) {
            viewModelScope.launch {
                val floor = floorRepository.getById(id) ?: return@launch
                existingFloor = floor
                _uiState.update { it.copy(label = floor.label) }
            }
        }
    }

    fun onLabelChange(value: String) = _uiState.update { it.copy(label = value) }

    fun save() {
        val label = _uiState.value.label
        if (label.isBlank()) return
        viewModelScope.launch {
            val existing = existingFloor
            if (existing != null) {
                floorRepository.rename(existing, label)
            } else {
                floorRepository.create(hostelId, label)
            }
            _uiState.update { it.copy(saved = true) }
        }
    }
}
