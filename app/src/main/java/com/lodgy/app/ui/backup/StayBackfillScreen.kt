package com.lodgy.app.ui.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lodgy.app.R
import com.lodgy.app.backup.STAY_CSV_HEADER
import com.lodgy.app.data.dao.BackfilledStayRow
import com.lodgy.app.data.dao.BedChoice
import com.lodgy.app.data.entity.Tenant
import com.lodgy.app.ui.TrustedActivityLaunch
import com.lodgy.app.ui.icons.CommonIcons
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
private fun BedChoice.label(): String = if (propertyType.isSingleUnit) {
    hostelName
} else {
    "$hostelName — " + stringResource(R.string.bed_location, roomNumber, bedLabel)
}

@Composable
private fun BackfilledStayRow.bedLabel(): String = if (propertyType.isSingleUnit) {
    hostelName
} else {
    "$hostelName — " + stringResource(R.string.bed_location, roomNumber, bedLabel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StayBackfillScreen(onBack: () -> Unit, viewModel: StayBackfillViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }

    var showTenantPicker by remember { mutableStateOf(false) }
    var showBedPicker by remember { mutableStateOf(false) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    val pickLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::onFilePicked) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stay_backfill_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(CommonIcons.Back, contentDescription = null) }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.stay_backfill_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = uiState.selectedTenantName.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.stay_backfill_field_tenant)) },
                        trailingIcon = {
                            IconButton(onClick = { showTenantPicker = true }) {
                                Icon(CommonIcons.Edit, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = uiState.selectedBed?.label().orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.stay_backfill_field_bed)) },
                        trailingIcon = {
                            IconButton(onClick = { showBedPicker = true }) {
                                Icon(CommonIcons.Edit, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = uiState.startDateMillis?.let { dateFormat.format(Date(it)) }.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.stay_backfill_field_start)) },
                        trailingIcon = {
                            IconButton(onClick = { showStartPicker = true }) {
                                Icon(CommonIcons.Edit, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = uiState.endDateMillis?.let { dateFormat.format(Date(it)) }.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.stay_backfill_field_end)) },
                        trailingIcon = {
                            IconButton(onClick = { showEndPicker = true }) {
                                Icon(CommonIcons.Edit, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (uiState.overlapError) {
                        Text(
                            stringResource(R.string.stay_backfill_overlap_error),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    if (uiState.saved) {
                        Text(
                            stringResource(R.string.stay_backfill_saved),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        if (uiState.editingPeriodId != null) {
                            OutlinedButton(onClick = viewModel::cancelEdit, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.stay_backfill_cancel_edit))
                            }
                        }
                        Button(onClick = viewModel::save, enabled = uiState.canSave, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                stringResource(
                                    if (uiState.editingPeriodId != null) {
                                        R.string.stay_backfill_save_edit
                                    } else {
                                        R.string.stay_backfill_save
                                    },
                                ),
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            Text(stringResource(R.string.stay_backfill_bulk_title), style = MaterialTheme.typography.titleMedium)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.stay_backfill_format), style = MaterialTheme.typography.labelLarge)
                    Text(STAY_CSV_HEADER, style = MaterialTheme.typography.bodySmall)
                    Text(
                        stringResource(R.string.stay_backfill_format_example),
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
                Text(stringResource(R.string.stay_backfill_pick_csv))
            }

            if (uiState.readFailed) {
                Text(
                    stringResource(R.string.stay_backfill_read_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (uiState.csvRows.isNotEmpty() || uiState.csvErrors.isNotEmpty()) {
                Text(
                    stringResource(R.string.stay_backfill_summary, uiState.importableCount, uiState.csvRows.size),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (uiState.unmatchedRows.isNotEmpty()) {
                    Text(
                        stringResource(R.string.stay_backfill_unmatched, uiState.unmatchedRows.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                uiState.csvErrors.take(5).forEach { error ->
                    Text(
                        stringResource(R.string.stay_backfill_bad_line, error.lineNumber, error.line),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            uiState.imported?.let { count ->
                Text(
                    stringResource(R.string.stay_backfill_done, count) +
                        if (uiState.skippedOverlap > 0) {
                            " " + stringResource(R.string.stay_backfill_done_overlap, uiState.skippedOverlap)
                        } else {
                            ""
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Button(
                onClick = viewModel::importCsv,
                enabled = uiState.canImportCsv,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.stay_backfill_import_action))
            }

            HorizontalDivider()

            Text(stringResource(R.string.stay_backfill_list_title), style = MaterialTheme.typography.titleMedium)
            if (uiState.stays.isEmpty()) {
                Text(
                    stringResource(R.string.stay_backfill_list_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                uiState.stays.forEach { stay ->
                    StayRowCard(
                        stay = stay,
                        dateFormat = dateFormat,
                        onEdit = { viewModel.startEdit(stay) },
                        onDelete = { viewModel.delete(stay) },
                    )
                }
            }
        }
    }

    if (showTenantPicker) {
        TenantPickerDialog(
            tenants = uiState.tenants,
            onSelect = { viewModel.onTenantSelected(it.id); showTenantPicker = false },
            onDismiss = { showTenantPicker = false },
        )
    }

    if (showBedPicker) {
        BedPickerDialog(
            beds = uiState.beds,
            onSelect = { viewModel.onBedSelected(it.bedId); showBedPicker = false },
            onDismiss = { showBedPicker = false },
        )
    }

    if (showStartPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = uiState.startDateMillis)
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let(viewModel::onStartDateChange)
                    showStartPicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { showStartPicker = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(state = state) }
    }

    if (showEndPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = uiState.endDateMillis)
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let(viewModel::onEndDateChange)
                    showEndPicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { showEndPicker = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun StayRowCard(
    stay: BackfilledStayRow,
    dateFormat: SimpleDateFormat,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(stay.tenantName, style = MaterialTheme.typography.titleMedium)
                Text(stay.bedLabel(), style = MaterialTheme.typography.bodySmall)
                Text(
                    "${dateFormat.format(Date(stay.startDate))} – " +
                        (stay.endDate?.let { dateFormat.format(Date(it)) } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row {
                IconButton(onClick = onEdit) { Icon(CommonIcons.Edit, contentDescription = null) }
                IconButton(onClick = onDelete) { Icon(CommonIcons.Trash, contentDescription = null) }
            }
        }
    }
}

@Composable
private fun TenantPickerDialog(tenants: List<Tenant>, onSelect: (Tenant) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.stay_backfill_pick_tenant)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                tenants.forEach { tenant ->
                    Text(
                        tenant.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth()
                            .clickable { onSelect(tenant) }
                            .padding(vertical = 10.dp),
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun BedPickerDialog(beds: List<BedChoice>, onSelect: (BedChoice) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.stay_backfill_pick_bed)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                beds.forEach { bed ->
                    Text(
                        bed.label(),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth()
                            .clickable { onSelect(bed) }
                            .padding(vertical = 10.dp),
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
