package com.lodgy.app.ui.property

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.entity.Room
import com.lodgy.app.data.entity.RoomType
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.RoomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RoomFormUiState(
    val isEditing: Boolean = false,
    val roomNumber: String = "",
    val type: RoomType = RoomType.SINGLE,
    val pricePerBed: String = "",
    val amenities: String = "",
    val saved: Boolean = false,
    val showTypeChangeConfirm: Boolean = false,
) {
    val canSave: Boolean get() = roomNumber.isNotBlank() && pricePerBed.toDoubleOrNull() != null
}

@HiltViewModel
class RoomFormViewModel @Inject constructor(
    private val roomRepository: RoomRepository,
    private val bedRepository: BedRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val floorId: String = checkNotNull(savedStateHandle["floorId"])
    private val roomId: String? = savedStateHandle["roomId"]
    private var existingRoom: Room? = null

    private val _uiState = MutableStateFlow(RoomFormUiState(isEditing = roomId != null))
    val uiState: StateFlow<RoomFormUiState> = _uiState.asStateFlow()

    init {
        val id = roomId
        if (id != null) {
            viewModelScope.launch {
                val room = roomRepository.getById(id) ?: return@launch
                existingRoom = room
                _uiState.update {
                    it.copy(
                        roomNumber = room.roomNumber,
                        type = room.type,
                        pricePerBed = room.pricePerBed.toString(),
                        amenities = room.amenities,
                    )
                }
            }
        }
    }

    fun onRoomNumberChange(value: String) = _uiState.update { it.copy(roomNumber = value) }
    fun onTypeChange(value: RoomType) = _uiState.update { it.copy(type = value) }
    fun onPriceChange(value: String) = _uiState.update { it.copy(pricePerBed = value) }
    fun onAmenitiesChange(value: String) = _uiState.update { it.copy(amenities = value) }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        viewModelScope.launch {
            val existing = existingRoom
            if (existing != null && state.type != existing.type && bedRepository.hasOccupiedBed(existing.id)) {
                _uiState.update { it.copy(showTypeChangeConfirm = true) }
            } else {
                persist()
            }
        }
    }

    fun confirmTypeChange() {
        _uiState.update { it.copy(showTypeChangeConfirm = false) }
        viewModelScope.launch { persist() }
    }

    fun dismissTypeChangeConfirm() {
        _uiState.update { it.copy(showTypeChangeConfirm = false) }
    }

    private suspend fun persist() {
        val state = _uiState.value
        val price = state.pricePerBed.toDoubleOrNull() ?: return
        val existing = existingRoom
        if (existing != null) {
            roomRepository.update(existing, state.roomNumber, state.type, price, state.amenities)
        } else {
            val room = roomRepository.create(floorId, state.roomNumber, state.type, price, state.amenities)
            bedRepository.generateForRoom(room.id, bedCountFor(state.type))
        }
        _uiState.update { it.copy(saved = true) }
    }

    private fun bedCountFor(type: RoomType): Int = when (type) {
        RoomType.SINGLE -> 1
        RoomType.DOUBLE -> 2
        RoomType.TRIPLE -> 3
    }
}
