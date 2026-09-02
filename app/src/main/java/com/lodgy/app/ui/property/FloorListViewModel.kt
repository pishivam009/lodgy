package com.lodgy.app.ui.property

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.entity.Floor
import com.lodgy.app.data.repository.FloorRepository
import com.lodgy.app.data.repository.HostelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FloorListUiState(
    val hostelName: String = "",
    val floors: List<Floor> = emptyList(),
)

@HiltViewModel
class FloorListViewModel @Inject constructor(
    private val floorRepository: FloorRepository,
    hostelRepository: HostelRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val hostelId: String = checkNotNull(savedStateHandle["hostelId"])

    private val _uiState = MutableStateFlow(FloorListUiState())
    val uiState: StateFlow<FloorListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val hostel = hostelRepository.getById(hostelId)
            _uiState.update { it.copy(hostelName = hostel?.name.orEmpty()) }
        }
        viewModelScope.launch {
            floorRepository.getByHostelId(hostelId).collect { floors ->
                _uiState.update { it.copy(floors = floors.sortedBy(Floor::sortOrder)) }
            }
        }
    }

    fun moveUp(floor: Floor) = viewModelScope.launch { floorRepository.moveUp(floor, hostelId) }
    fun moveDown(floor: Floor) = viewModelScope.launch { floorRepository.moveDown(floor, hostelId) }
    fun delete(floor: Floor) = viewModelScope.launch { floorRepository.delete(floor) }
}
