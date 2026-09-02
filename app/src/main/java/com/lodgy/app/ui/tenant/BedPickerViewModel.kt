package com.lodgy.app.ui.tenant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.entity.Bed
import com.lodgy.app.data.entity.BedStatus
import com.lodgy.app.data.prefs.HostelPreferences
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.FloorRepository
import com.lodgy.app.data.repository.RoomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VacantBedOption(val bed: Bed, val roomNumber: String, val floorLabel: String)

data class BedPickerUiState(
    val loading: Boolean = true,
    val hasActiveHostel: Boolean = true,
    val options: List<VacantBedOption> = emptyList(),
)

@HiltViewModel
class BedPickerViewModel @Inject constructor(
    private val hostelPreferences: HostelPreferences,
    private val floorRepository: FloorRepository,
    private val roomRepository: RoomRepository,
    private val bedRepository: BedRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BedPickerUiState())
    val uiState: StateFlow<BedPickerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val hostelId = hostelPreferences.selectedHostelId.first()
            if (hostelId == null) {
                _uiState.update { it.copy(loading = false, hasActiveHostel = false) }
                return@launch
            }

            val options = mutableListOf<VacantBedOption>()
            val floors = floorRepository.getByHostelId(hostelId).first().sortedBy { it.sortOrder }
            for (floor in floors) {
                val rooms = roomRepository.getByFloorId(floor.id).first()
                for (room in rooms) {
                    val beds = bedRepository.getByRoomId(room.id).first()
                        .filter { it.status == BedStatus.VACANT }
                        .sortedBy { it.label }
                    beds.forEach { bed -> options.add(VacantBedOption(bed, room.roomNumber, floor.label)) }
                }
            }
            _uiState.update { it.copy(loading = false, options = options) }
        }
    }
}
