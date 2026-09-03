package com.lodgy.app.ui.backup

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.R
import com.lodgy.app.backup.BackupManager
import com.lodgy.app.backup.ImportResult
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BackupUiState(
    val exporting: Boolean = false,
    @param:StringRes val exportMessage: Int? = null,
    val importing: Boolean = false,
    @param:StringRes val importError: Int? = null,
    val showOverwriteConfirm: Boolean = false,
    val restoreComplete: Boolean = false,
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupManager: BackupManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    private var pendingStagingDir: File? = null

    fun onExportDestinationPicked(destination: Uri) {
        _uiState.update { it.copy(exporting = true, exportMessage = null) }
        viewModelScope.launch {
            val success = backupManager.export(destination)
            _uiState.update {
                it.copy(
                    exporting = false,
                    exportMessage = if (success) R.string.backup_export_success else R.string.backup_export_failed,
                )
            }
        }
    }

    fun onImportFilePicked(source: Uri) {
        _uiState.update { it.copy(importing = true, importError = null) }
        viewModelScope.launch {
            val (result, staged) = backupManager.stageImport(source)
            when (result) {
                is ImportResult.Success -> {
                    pendingStagingDir = staged
                    _uiState.update { it.copy(importing = false, showOverwriteConfirm = true) }
                }
                is ImportResult.NotALodgyBackup -> {
                    _uiState.update { it.copy(importing = false, importError = R.string.backup_import_invalid) }
                }
                is ImportResult.Failed -> {
                    _uiState.update { it.copy(importing = false, importError = R.string.backup_import_failed) }
                }
            }
        }
    }

    fun dismissOverwriteConfirm() {
        pendingStagingDir?.deleteRecursively()
        pendingStagingDir = null
        _uiState.update { it.copy(showOverwriteConfirm = false) }
    }

    fun confirmImport() {
        val staged = pendingStagingDir ?: return
        _uiState.update { it.copy(showOverwriteConfirm = false, importing = true) }
        viewModelScope.launch {
            backupManager.applyStaged(staged)
            pendingStagingDir = null
            _uiState.update { it.copy(importing = false, restoreComplete = true) }
        }
    }
}
