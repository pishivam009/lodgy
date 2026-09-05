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

data class AllRoomsHostel(val id: String, val name: String)

/** Rooms the warden can still put someone in. Deliberately EMPTY plus PARTIAL rather than EMPTY
 *  alone: a partly filled room does contain a vacant bed, and hiding it would answer "where is
 *  there space" wrongly (LODGY-72). */
enum class RoomSpaceFilter { ALL, HAS_SPACE }

data class AllRoomsUiState(
    /** Null means every hostel - the default, since this screen exists to survey the whole estate. */
    val filterHostelId: String? = null,
    val spaceFilter: RoomSpaceFilter = RoomSpaceFilter.ALL,
    val hostels: List<AllRoomsHostel> = emptyList(),
    val items: List<AllRoomsItem> = emptyList(),
    val loading: Boolean = true,
) {
    val filterHostelName: String?
        get() = filterHostelId?.let { id -> hostels.firstOrNull { it.id == id }?.name }

    val visibleItems: List<AllRoomsItem>
        get() = items
            .filter { filterHostelId == null || it.room.hostelId == filterHostelId }
            .filter { spaceFilter == RoomSpaceFilter.ALL || it.vacantBeds > 0 }

    /** Counted over what is on screen, so the summary never contradicts the tiles below it. */
    val emptyRooms: Int get() = visibleItems.count { it.occupancy == RoomFill.EMPTY }
    val partialRooms: Int get() = visibleItems.count { it.occupancy == RoomFill.PARTIAL }
    val fullRooms: Int get() = visibleItems.count { it.occupancy == RoomFill.FULL }

    /** The tile counts ROOMS while the dashboard tile that can lead here counts BEDS, and 5 vacant
     *  beds can be 3 rooms. Surfaced so the two never look like they disagree (LODGY-72). */
    val vacantBedsInView: Int get() = visibleItems.sumOf { it.vacantBeds }
}

@HiltViewModel
class AllRoomsViewModel @Inject constructor(
    roomRepository: RoomRepository,
    bedRepository: BedRepository,
    hostelRepository: HostelRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AllRoomsUiState())
    val uiState: StateFlow<AllRoomsUiState> = _uiState.asStateFlow()

    init {
        // Both optional: arriving from a hostel pre-filters to it, and arriving from the Home
        // vacant-beds tile pre-filters to rooms with space.
        val hostelId: String? = savedStateHandle["hostelId"]
        val hasSpaceOnly: Boolean = savedStateHandle["hasSpace"] ?: false
        _uiState.update {
            it.copy(
                filterHostelId = hostelId,
                spaceFilter = if (hasSpaceOnly) RoomSpaceFilter.HAS_SPACE else RoomSpaceFilter.ALL,
            )
        }

        viewModelScope.launch {
            combine(
                roomRepository.getAllWithFloor(),
                bedRepository.observeRoomOccupancy(),
                hostelRepository.getAll(),
            ) { rooms, occupancy, hostels ->
                val byRoomId = occupancy.associateBy(RoomOccupancy::roomId)
                val items = rooms.map { room ->
                    val counts = byRoomId[room.roomId]
                    AllRoomsItem(room, counts?.totalBeds ?: 0, counts?.occupiedBeds ?: 0)
                }
                items to hostels.map { AllRoomsHostel(it.id, it.name) }
            }.collect { (items, hostels) ->
                _uiState.update { it.copy(loading = false, items = items, hostels = hostels) }
            }
        }
    }

    fun onHostelFilterChange(hostelId: String?) = _uiState.update { it.copy(filterHostelId = hostelId) }

    fun onSpaceFilterChange(filter: RoomSpaceFilter) = _uiState.update { it.copy(spaceFilter = filter) }
}
