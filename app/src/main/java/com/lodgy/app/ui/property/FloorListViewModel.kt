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

/** Bed-level counts. Kept at bed level rather than room level so a warden is not reading
 *  rooms on one screen and beds on another. */
data class FloorListItem(val floor: Floor, val totalBeds: Int, val occupiedBeds: Int) {
    val vacantBeds: Int get() = totalBeds - occupiedBeds
}

data class FloorListUiState(
    val hostelName: String = "",
    val items: List<FloorListItem> = emptyList(),
    /** Set when a delete was refused because tenants still live under the floor, with their names
     *  so the block can say who is in the way rather than failing generically. */
    val blockedDelete: BlockedFloorDelete? = null,
    val pendingDeleteFloor: Floor? = null,
) {
    val floors: List<Floor> get() = items.map { it.floor }
}

data class BlockedFloorDelete(val floor: Floor, val tenantNames: List<String>)

@HiltViewModel
class FloorListViewModel @Inject constructor(
    private val floorRepository: FloorRepository,
    private val bedRepository: BedRepository,
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
    /** Deleting a floor cascades to its rooms and beds, but Bed <- TenancyAgreement is NO ACTION,
     *  so removing a bed an agreement still points at makes SQLite reject the whole delete and the
     *  app dies. Blocked here rather than caught afterwards: by the time the constraint fires the
     *  warden has already confirmed an action that was never going to complete. */
    fun requestDelete(floor: Floor) {
        viewModelScope.launch {
            val tenants = bedRepository.activeTenantNamesOnFloor(floor.id)
            _uiState.update {
                if (tenants.isEmpty()) {
                    it.copy(pendingDeleteFloor = floor)
                } else {
                    it.copy(blockedDelete = BlockedFloorDelete(floor, tenants))
                }
            }
        }
    }

    fun confirmDelete() {
        val floor = _uiState.value.pendingDeleteFloor ?: return
        viewModelScope.launch {
            floorRepository.delete(floor)
            _uiState.update { it.copy(pendingDeleteFloor = null) }
        }
    }

    fun dismissPendingDelete() = _uiState.update { it.copy(pendingDeleteFloor = null) }

    fun dismissBlockedDelete() = _uiState.update { it.copy(blockedDelete = null) }
}
