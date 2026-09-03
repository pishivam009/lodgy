package com.lodgy.app.ui.property

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.dao.FloorOccupancy
import com.lodgy.app.data.entity.Floor
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.FloorRepository
import com.lodgy.app.data.repository.HostelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Bed-level counts, matching how VacantViewScreen counts - a warden comparing the two
 *  screens should not be reading rooms in one and beds in the other. */
data class FloorListItem(val floor: Floor, val totalBeds: Int, val occupiedBeds: Int) {
    val vacantBeds: Int get() = totalBeds - occupiedBeds
}

data class FloorListUiState(
    val hostelName: String = "",
    val items: List<FloorListItem> = emptyList(),
) {
    val floors: List<Floor> get() = items.map { it.floor }
}

@HiltViewModel
class FloorListViewModel @Inject constructor(
    private val floorRepository: FloorRepository,
    bedRepository: BedRepository,
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
            combine(
                floorRepository.getByHostelId(hostelId),
                bedRepository.observeOccupancyByHostel(hostelId),
            ) { floors, occupancy ->
                val byFloorId = occupancy.associateBy(FloorOccupancy::floorId)
                floors.sortedBy(Floor::sortOrder).map { floor ->
                    val counts = byFloorId[floor.id]
                    FloorListItem(floor, counts?.totalBeds ?: 0, counts?.occupiedBeds ?: 0)
                }
            }.collect { items -> _uiState.update { it.copy(items = items) } }
        }
    }

    fun moveUp(floor: Floor) = viewModelScope.launch { floorRepository.moveUp(floor, hostelId) }
    fun moveDown(floor: Floor) = viewModelScope.launch { floorRepository.moveDown(floor, hostelId) }
    fun delete(floor: Floor) = viewModelScope.launch { floorRepository.delete(floor) }
}
