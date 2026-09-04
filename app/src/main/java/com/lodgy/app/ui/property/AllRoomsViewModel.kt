package com.lodgy.app.ui.property

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.dao.RoomOccupancy
import com.lodgy.app.data.dao.RoomWithFloor
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.HostelRepository
import com.lodgy.app.data.repository.RoomRepository
import com.lodgy.app.ui.common.RoomFill
import com.lodgy.app.ui.common.roomFillOf
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AllRoomsItem(val room: RoomWithFloor, val totalBeds: Int, val occupiedBeds: Int) {
    val vacantBeds: Int get() = totalBeds - occupiedBeds
    val occupancy: RoomFill get() = roomFillOf(totalBeds, occupiedBeds)
}

data class AllRoomsUiState(
    val hostelName: String = "",
    val items: List<AllRoomsItem> = emptyList(),
    val loading: Boolean = true,
) {
    val emptyRooms: Int get() = items.count { it.occupancy == RoomFill.EMPTY }
    val partialRooms: Int get() = items.count { it.occupancy == RoomFill.PARTIAL }
    val fullRooms: Int get() = items.count { it.occupancy == RoomFill.FULL }
}

@HiltViewModel
class AllRoomsViewModel @Inject constructor(
    roomRepository: RoomRepository,
    bedRepository: BedRepository,
    hostelRepository: HostelRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val hostelId: String = checkNotNull(savedStateHandle["hostelId"])

    private val _uiState = MutableStateFlow(AllRoomsUiState())
    val uiState: StateFlow<AllRoomsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val hostel = hostelRepository.getById(hostelId)
            _uiState.update { it.copy(hostelName = hostel?.name.orEmpty()) }
        }
        viewModelScope.launch {
            combine(
                roomRepository.getByHostelIdWithFloor(hostelId),
                bedRepository.observeRoomOccupancyByHostel(hostelId),
            ) { rooms, occupancy ->
                val byRoomId = occupancy.associateBy(RoomOccupancy::roomId)
                rooms.map { room ->
                    val counts = byRoomId[room.roomId]
                    AllRoomsItem(room, counts?.totalBeds ?: 0, counts?.occupiedBeds ?: 0)
                }
            }.collect { items -> _uiState.update { it.copy(loading = false, items = items) } }
        }
    }
}
