package com.lodgy.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.entity.BedStatus
import com.lodgy.app.data.entity.Floor
import com.lodgy.app.data.entity.RoomType
import com.lodgy.app.data.prefs.HostelPreferences
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.FloorRepository
import com.lodgy.app.data.repository.RoomRepository
import com.lodgy.app.ui.common.BedFilter
import com.lodgy.app.ui.common.matches
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VacantBedItem(
    val floorId: String,
    val floorLabel: String,
    val roomNumber: String,
    val roomType: RoomType,
    val bedLabel: String,
    val status: BedStatus,
)

data class VacantViewUiState(
    val loading: Boolean = true,
    val hasActiveHostel: Boolean = false,
    val floors: List<Floor> = emptyList(),
    val items: List<VacantBedItem> = emptyList(),
    val selectedFloorId: String? = null,
    /** Defaults to VACANT so the screen still opens as the vacant-beds view it was built as;
     *  the other two options turn it into a full bed browser without a second screen. */
    val statusFilter: BedFilter = BedFilter.VACANT,
) {
    val filteredItems: List<VacantBedItem>
        get() = items
            .filter { selectedFloorId == null || it.floorId == selectedFloorId }
            .filter { statusFilter.matches(it.status) }
}

@HiltViewModel
class VacantViewViewModel @Inject constructor(
    private val hostelPreferences: HostelPreferences,
    private val floorRepository: FloorRepository,
    private val roomRepository: RoomRepository,
    private val bedRepository: BedRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VacantViewUiState())
    val uiState: StateFlow<VacantViewUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val hostelId = hostelPreferences.selectedHostelId.first()
            if (hostelId == null) {
                _uiState.update { it.copy(loading = false, hasActiveHostel = false) }
                return@launch
            }

            val floors = floorRepository.getByHostelId(hostelId).first().sortedBy { it.sortOrder }
            val roomsWithFloor = floors.flatMap { floor ->
                roomRepository.getByFloorId(floor.id).first().map { room -> floor to room }
            }

            _uiState.update { it.copy(hasActiveHostel = true, floors = floors) }

            if (roomsWithFloor.isEmpty()) {
                _uiState.update { it.copy(loading = false) }
                return@launch
            }

            val bedFlows = roomsWithFloor.map { (_, room) -> bedRepository.getByRoomId(room.id) }
            combine(bedFlows) { bedsPerRoom ->
                bedsPerRoom.flatMapIndexed { index, beds ->
                    val (floor, room) = roomsWithFloor[index]
                    beds.sortedBy { it.label }
                        .map { bed ->
                            VacantBedItem(floor.id, floor.label, room.roomNumber, room.type, bed.label, bed.status)
                        }
                }
            }.collect { items -> _uiState.update { it.copy(loading = false, items = items) } }
        }
    }

    fun onFloorFilterChange(floorId: String?) = _uiState.update { it.copy(selectedFloorId = floorId) }

    fun onStatusFilterChange(filter: BedFilter) = _uiState.update { it.copy(statusFilter = filter) }
}
