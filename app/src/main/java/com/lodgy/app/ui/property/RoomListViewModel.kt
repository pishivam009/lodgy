package com.lodgy.app.ui.property

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.dao.RoomOccupancy
import com.lodgy.app.data.entity.Room
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.FloorRepository
import com.lodgy.app.data.repository.RoomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A room "has space" if any of its beds is vacant, and is "full" only when every bed is taken -
 *  that is the question a warden is actually asking when scanning a floor. */
enum class RoomFilter { ALL, HAS_SPACE, FULL }

data class RoomListItem(val room: Room, val totalBeds: Int, val occupiedBeds: Int) {
    val vacantBeds: Int get() = totalBeds - occupiedBeds
    val isFull: Boolean get() = totalBeds > 0 && vacantBeds == 0
}

data class RoomListUiState(
    val floorLabel: String = "",
    val items: List<RoomListItem> = emptyList(),
    val filter: RoomFilter = RoomFilter.ALL,
    val blockedDeleteRoom: BlockedRoomDelete? = null,
    val pendingDeleteRoom: Room? = null,
) {
    val filteredItems: List<RoomListItem>
        get() = when (filter) {
            RoomFilter.ALL -> items
            RoomFilter.HAS_SPACE -> items.filter { it.vacantBeds > 0 }
            RoomFilter.FULL -> items.filter { it.isFull }
        }
}

data class BlockedRoomDelete(val room: Room, val tenantNames: List<String>)

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
            combine(
                roomRepository.getByFloorId(floorId),
                bedRepository.observeOccupancyByFloor(floorId),
            ) { rooms, occupancy ->
                val byRoomId = occupancy.associateBy(RoomOccupancy::roomId)
                rooms.map { room ->
                    val counts = byRoomId[room.id]
                    RoomListItem(room, counts?.totalBeds ?: 0, counts?.occupiedBeds ?: 0)
                }
            }.collect { items -> _uiState.update { it.copy(items = items) } }
        }
    }

    fun onFilterChange(filter: RoomFilter) = _uiState.update { it.copy(filter = filter) }

    fun requestDelete(room: Room) {
        viewModelScope.launch {
            val tenants = bedRepository.activeTenantNamesInRoom(room.id)
            if (tenants.isNotEmpty()) {
                _uiState.update { it.copy(blockedDeleteRoom = BlockedRoomDelete(room, tenants)) }
            } else {
                _uiState.update { it.copy(pendingDeleteRoom = room) }
            }
        }
    }

    fun confirmDelete() {
        val room = _uiState.value.pendingDeleteRoom ?: return
        viewModelScope.launch {
            roomRepository.delete(room)
            _uiState.update { it.copy(pendingDeleteRoom = null) }
        }
    }

    fun dismissPendingDelete() {
        _uiState.update { it.copy(pendingDeleteRoom = null) }
    }

    fun dismissBlockedDelete() {
        _uiState.update { it.copy(blockedDeleteRoom = null) }
    }
}
