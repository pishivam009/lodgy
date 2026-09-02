package com.lodgy.app.ui.property

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.entity.Bed
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.RoomRepository
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
    val beds: List<Bed> = emptyList(),
)

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
            _uiState.update { it.copy(roomNumber = room?.roomNumber.orEmpty(), roomType = room?.type?.name.orEmpty()) }
        }
        viewModelScope.launch {
            bedRepository.getByRoomId(roomId).collect { beds ->
                _uiState.update { it.copy(beds = beds.sortedBy(Bed::label)) }
            }
        }
    }
}
