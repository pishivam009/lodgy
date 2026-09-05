package com.lodgy.app.ui.property

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.entity.Hostel
import com.lodgy.app.data.repository.RoomRepository
import com.lodgy.app.data.prefs.HostelPreferences
import com.lodgy.app.data.repository.HostelRepository
import com.lodgy.app.data.repository.WardenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class HostelListUiState(
    val hostels: List<Hostel> = emptyList(),
    val selectedHostelId: String? = null,
)

/** Where tapping a property should go. A hostel opens its floors; a single-unit property skips
 *  straight to the unit itself, because its floor and room exist only to keep the schema whole and
 *  mean nothing to the warden (LODGY-79). */
sealed interface PropertyDestination {
    data class Floors(val hostelId: String) : PropertyDestination
    data class Unit(val roomId: String) : PropertyDestination
}

@HiltViewModel
class HostelListViewModel @Inject constructor(
    private val wardenRepository: WardenRepository,
    private val hostelRepository: HostelRepository,
    private val hostelPreferences: HostelPreferences,
    private val roomRepository: RoomRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HostelListUiState())
    val uiState: StateFlow<HostelListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val warden = wardenRepository.getWarden() ?: return@launch
            combine(
                hostelRepository.getByWardenId(warden.id),
                hostelPreferences.selectedHostelId,
            ) { hostels, selectedId ->
                HostelListUiState(
                    hostels = hostels,
                    selectedHostelId = selectedId ?: hostels.firstOrNull()?.id,
                )
            }.collect { _uiState.value = it }
        }
    }

    fun selectHostel(id: String) {
        viewModelScope.launch { hostelPreferences.setSelectedHostelId(id) }
    }

    /** Resolved on tap rather than held in state: a single-unit property has exactly one room, and
     *  looking it up here keeps the list query unchanged. Falls back to the floor list if that room
     *  is somehow missing, so a tap can never dead-end. */
    fun openProperty(hostel: Hostel, onOpen: (PropertyDestination) -> Unit) {
        viewModelScope.launch {
            val destination = if (hostel.propertyType.isSingleUnit) {
                roomRepository.getFirstRoomIdByHostel(hostel.id)
                    ?.let(PropertyDestination::Unit)
                    ?: PropertyDestination.Floors(hostel.id)
            } else {
                PropertyDestination.Floors(hostel.id)
            }
            onOpen(destination)
        }
    }
}
