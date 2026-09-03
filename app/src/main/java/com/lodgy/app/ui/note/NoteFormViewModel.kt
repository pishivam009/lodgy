package com.lodgy.app.ui.note

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.entity.NoteType
import com.lodgy.app.data.entity.TenantNote
import com.lodgy.app.data.repository.TenantNoteRepository
import com.lodgy.app.media.PhotoStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NoteFormUiState(
    val isEditing: Boolean = false,
    val type: NoteType = NoteType.GENERAL,
    val text: String = "",
    val photoPath: String? = null,
    val occurredOnMillis: Long = System.currentTimeMillis(),
    val saved: Boolean = false,
    val deleted: Boolean = false,
) {
    val canSave: Boolean get() = text.isNotBlank()
}

@HiltViewModel
class NoteFormViewModel @Inject constructor(
    private val tenantNoteRepository: TenantNoteRepository,
    private val photoStorage: PhotoStorage,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val tenantId: String = checkNotNull(savedStateHandle["tenantId"])
    private val noteId: String? = savedStateHandle["noteId"]
    private var existingNote: TenantNote? = null

    private val _uiState = MutableStateFlow(NoteFormUiState(isEditing = noteId != null))
    val uiState: StateFlow<NoteFormUiState> = _uiState.asStateFlow()

    init {
        val id = noteId
        if (id != null) {
            viewModelScope.launch {
                val note = tenantNoteRepository.getById(id) ?: return@launch
                existingNote = note
                _uiState.update {
                    it.copy(
                        type = note.type,
                        text = note.text,
                        photoPath = note.photoPath,
                        occurredOnMillis = note.occurredOn,
                    )
                }
            }
        }
    }

    fun onTypeChange(value: NoteType) = _uiState.update { it.copy(type = value) }
    fun onTextChange(value: String) = _uiState.update { it.copy(text = value) }
    fun onOccurredOnChange(millis: Long) = _uiState.update { it.copy(occurredOnMillis = millis) }

    fun createCameraOutputUri(): Uri = photoStorage.createCameraOutputUri()

    fun onPhotoPicked(uri: Uri) {
        viewModelScope.launch {
            val path = photoStorage.persist(uri)
            _uiState.update { it.copy(photoPath = path) }
        }
    }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        viewModelScope.launch {
            val existing = existingNote
            if (existing != null) {
                tenantNoteRepository.update(existing, state.type, state.text, state.photoPath, state.occurredOnMillis)
            } else {
                tenantNoteRepository.create(tenantId, state.type, state.text, state.photoPath, state.occurredOnMillis)
            }
            _uiState.update { it.copy(saved = true) }
        }
    }

    fun delete() {
        val existing = existingNote ?: return
        viewModelScope.launch {
            tenantNoteRepository.delete(existing)
            _uiState.update { it.copy(deleted = true) }
        }
    }
}
