package com.lodgy.app.ui.property

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.entity.Room
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.FloorRepository
import com.lodgy.app.data.repository.RoomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RoomListUiState(
    val floorLabel: String = "",
    val rooms: List<Room> = emptyList(),
    val blockedDeleteRoom: Room? = null,
)

@HiltViewModel
class RoomListViewModel @Inject constructor(
    private val roomRepository: RoomRepository,
    private val bedRepository: BedRepository,
    floorRepository: FloorRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val floorId: String = checkNotNull(savedStateHandle["floorId"])

    private val _uiState = MutableStateFlow(RoomListUiState())
    val uiState: StateFlow<RoomListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val floor = floorRepository.getById(floorId)
            _uiState.update { it.copy(floorLabel = floor?.label.orEmpty()) }
        }
        viewModelScope.launch {
            roomRepository.getByFloorId(floorId).collect { rooms ->
                _uiState.update { it.copy(rooms = rooms) }
            }
        }
    }

    fun requestDelete(room: Room) {
        viewModelScope.launch {
            if (bedRepository.hasOccupiedBed(room.id)) {
                _uiState.update { it.copy(blockedDeleteRoom = room) }
            } else {
                roomRepository.delete(room)
            }
        }
    }

    fun dismissBlockedDelete() {
        _uiState.update { it.copy(blockedDeleteRoom = null) }
    }
}
