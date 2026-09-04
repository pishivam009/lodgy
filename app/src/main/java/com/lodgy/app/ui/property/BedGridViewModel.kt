package com.lodgy.app.ui.property

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.entity.Bed
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.RoomRepository
import com.lodgy.app.ui.common.BedFilter
import com.lodgy.app.ui.common.matches
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BedGridUiState(
    val roomNumber: String = "",
    val roomType: String = "",
    val pricePerBed: Double = 0.0,
    /** Free text as the warden typed it. Captured since LODGY-8 and, until LODGY-71, readable
     *  nowhere except the edit form - so checking what a room had meant opening a screen whose
     *  purpose is changing it. */
    val amenities: String = "",
    val beds: List<Bed> = emptyList(),
    val filter: BedFilter = BedFilter.ALL,
) {
    val filteredBeds: List<Bed> get() = beds.filter { filter.matches(it.status) }
}

@HiltViewModel
class BedGridViewModel @Inject constructor(
    bedRepository: BedRepository,
    roomRepository: RoomRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val roomId: String = checkNotNull(savedStateHandle["roomId"])

    private val _uiState = MutableStateFlow(BedGridUiState())
    val uiState: StateFlow<BedGridUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val room = roomRepository.getById(roomId)
            _uiState.update {
                it.copy(
                    roomNumber = room?.roomNumber.orEmpty(),
                    roomType = room?.type?.name.orEmpty(),
                    pricePerBed = room?.pricePerBed ?: 0.0,
                    amenities = room?.amenities.orEmpty(),
                )
            }
        }
        viewModelScope.launch {
            bedRepository.getByRoomId(roomId).collect { beds ->
                _uiState.update { it.copy(beds = beds.sortedBy(Bed::label)) }
            }
        }
    }

    fun onFilterChange(filter: BedFilter) = _uiState.update { it.copy(filter = filter) }
}
