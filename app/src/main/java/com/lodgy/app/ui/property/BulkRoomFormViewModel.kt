package com.lodgy.app.ui.property

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

data class BulkRoomFormUiState(
    val startNumber: String = "",
    val count: String = "",
    val type: RoomType = RoomType.SINGLE,
    val pricePerBed: String = "",
    val amenities: String = "",
    val saved: Boolean = false,
) {
    val canSave: Boolean
        get() = startNumber.toIntOrNull() != null &&
            (count.toIntOrNull() ?: 0) > 0 &&
            pricePerBed.toDoubleOrNull() != null
}

@HiltViewModel
class BulkRoomFormViewModel @Inject constructor(
    private val roomRepository: RoomRepository,
    private val bedRepository: BedRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val floorId: String = checkNotNull(savedStateHandle["floorId"])

    private val _uiState = MutableStateFlow(BulkRoomFormUiState())
    val uiState: StateFlow<BulkRoomFormUiState> = _uiState.asStateFlow()

    fun onStartNumberChange(value: String) = _uiState.update { it.copy(startNumber = value) }
    fun onCountChange(value: String) = _uiState.update { it.copy(count = value) }
    fun onTypeChange(value: RoomType) = _uiState.update { it.copy(type = value) }
    fun onPriceChange(value: String) = _uiState.update { it.copy(pricePerBed = value) }
    fun onAmenitiesChange(value: String) = _uiState.update { it.copy(amenities = value) }

    fun save() {
        val state = _uiState.value
        val startNumber = state.startNumber.toIntOrNull() ?: return
        val count = state.count.toIntOrNull() ?: return
        val price = state.pricePerBed.toDoubleOrNull() ?: return
        if (count <= 0) return

        viewModelScope.launch {
            repeat(count) { index ->
                val room = roomRepository.create(
                    floorId,
                    (startNumber + index).toString(),
                    state.type,
                    price,
                    state.amenities,
                )
                bedRepository.generateForRoom(room.id, bedCountFor(state.type))
            }
            _uiState.update { it.copy(saved = true) }
        }
    }

    private fun bedCountFor(type: RoomType): Int = when (type) {
        RoomType.SINGLE -> 1
        RoomType.DOUBLE -> 2
        RoomType.TRIPLE -> 3
    }
}
