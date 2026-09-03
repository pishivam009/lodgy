package com.lodgy.app.ui.backup

import android.net.Uri
import com.lodgy.app.R
import com.lodgy.app.backup.BackupManager
import com.lodgy.app.backup.ImportResult
import com.lodgy.app.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

class BackupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val backupManager: BackupManager = mockk()
    private val viewModel = BackupViewModel(backupManager)

    @Test
    fun `export success reports the success message`() {
        val destination: Uri = mockk()
        coEvery { backupManager.export(destination) } returns true

        viewModel.onExportDestinationPicked(destination)

        val state = viewModel.uiState.value
        assertFalse(state.exporting)
        assertEquals(R.string.backup_export_success, state.exportMessage)
    }

    @Test
    fun `export failure reports the failure message`() {
        val destination: Uri = mockk()
        coEvery { backupManager.export(destination) } returns false

        viewModel.onExportDestinationPicked(destination)

        assertEquals(R.string.backup_export_failed, viewModel.uiState.value.exportMessage)
    }

    @Test
    fun `a valid backup asks for overwrite confirmation before restoring`() {
        val source: Uri = mockk()
        val staged = File("/tmp/staging")
        coEvery { backupManager.stageImport(source) } returns (ImportResult.Success to staged)

        viewModel.onImportFilePicked(source)

        val state = viewModel.uiState.value
        assertFalse(state.importing)
        assertTrue(state.showOverwriteConfirm)
        assertNull(state.importError)
    }

    @Test
    fun `a file that isn't a lodgy backup reports the invalid-backup error`() {
        val source: Uri = mockk()
        coEvery { backupManager.stageImport(source) } returns (ImportResult.NotALodgyBackup to null)

        viewModel.onImportFilePicked(source)

        assertEquals(R.string.backup_import_invalid, viewModel.uiState.value.importError)
        assertFalse(viewModel.uiState.value.showOverwriteConfirm)
    }

    @Test
    fun `a failed staging attempt reports the generic import-failed error`() {
        val source: Uri = mockk()
        coEvery { backupManager.stageImport(source) } returns (ImportResult.Failed("disk full") to null)

        viewModel.onImportFilePicked(source)

        assertEquals(R.string.backup_import_failed, viewModel.uiState.value.importError)
    }

    @Test
    fun `dismissOverwriteConfirm deletes the staged files and hides the dialog`() {
        val source: Uri = mockk()
        val staged: File = mockk(relaxed = true)
        coEvery { backupManager.stageImport(source) } returns (ImportResult.Success to staged)
        viewModel.onImportFilePicked(source)

        viewModel.dismissOverwriteConfirm()

        io.mockk.verify { staged.deleteRecursively() }
        assertFalse(viewModel.uiState.value.showOverwriteConfirm)
    }

    @Test
    fun `confirmImport applies the staged backup and reports restore complete`() {
        val source: Uri = mockk()
        val staged = File("/tmp/staging")
        coEvery { backupManager.stageImport(source) } returns (ImportResult.Success to staged)
        coEvery { backupManager.applyStaged(staged) } returns Unit
        viewModel.onImportFilePicked(source)

        viewModel.confirmImport()

        coVerify { backupManager.applyStaged(staged) }
        assertTrue(viewModel.uiState.value.restoreComplete)
        assertFalse(viewModel.uiState.value.showOverwriteConfirm)
    }

    @Test
    fun `confirmImport without a staged backup does nothing`() {
        viewModel.confirmImport()

        coVerify(exactly = 0) { backupManager.applyStaged(any()) }
    }
}
