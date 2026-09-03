package com.lodgy.app.ui.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lodgy.app.R
import com.lodgy.app.backup.HISTORY_CSV_HEADER
import com.lodgy.app.ui.TrustedActivityLaunch
import com.lodgy.app.ui.icons.CommonIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryImportScreen(onBack: () -> Unit, viewModel: HistoryImportViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val pickLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::onFilePicked) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_import_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(CommonIcons.Back, contentDescription = null) }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.history_import_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.history_import_format), style = MaterialTheme.typography.labelLarge)
                    Text(HISTORY_CSV_HEADER, style = MaterialTheme.typography.bodySmall)
                    Text(
                        stringResource(R.string.history_import_format_example),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    TrustedActivityLaunch.expectOne()
                    pickLauncher.launch(arrayOf("*/*"))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.history_import_pick))
            }

            if (uiState.readFailed) {
                Text(
                    stringResource(R.string.history_import_read_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (uiState.rows.isNotEmpty() || uiState.errors.isNotEmpty()) {
                Text(
                    stringResource(R.string.history_import_summary, uiState.importableCount, uiState.rows.size),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (uiState.unmatchedRows.isNotEmpty()) {
                    Text(
                        stringResource(R.string.history_import_unmatched, uiState.unmatchedRows.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                uiState.errors.take(5).forEach { error ->
                    Text(
                        stringResource(R.string.history_import_bad_line, error.lineNumber, error.line),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            uiState.imported?.let { count ->
                Text(
                    stringResource(R.string.history_import_done, count),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Button(
                onClick = viewModel::import,
                enabled = uiState.canImport,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.history_import_action))
            }
        }
    }
}
