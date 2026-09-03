package com.lodgy.app.ui.tenant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lodgy.app.R
import com.lodgy.app.ui.common.label
import com.lodgy.app.ui.icons.CommonIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: TransferViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onDone()
    }

    val currentLabel = uiState.currentLocation?.label().orEmpty()
    val selected = uiState.selectedOption
    val targetLabel = selected?.let { stringResource(R.string.bed_location, it.roomNumber, it.bedLabel) }.orEmpty()
    val noteText = stringResource(R.string.transfer_note, currentLabel, targetLabel)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.transfer_title))
                        Text(uiState.tenantName, style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(CommonIcons.Back, contentDescription = null) }
                },
            )
        },
    ) { padding ->
        if (!uiState.loading && !uiState.hasActiveAgreement) {
            Box(modifier = Modifier.padding(padding).fillMaxWidth().padding(32.dp)) {
                Text(
                    stringResource(R.string.transfer_no_agreement),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }

        Column(modifier = Modifier.padding(padding).fillMaxWidth()) {
            Text(
                stringResource(R.string.transfer_current, currentLabel),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            if (!uiState.loading && uiState.options.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp)) {
                    Text(
                        stringResource(R.string.bed_picker_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                return@Scaffold
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(uiState.options, key = { it.bedId }) { option ->
                    Card(
                        onClick = { viewModel.onBedSelected(option.bedId) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = if (option.bedId == uiState.selectedBedId) {
                            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        } else {
                            CardDefaults.cardColors()
                        },
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    stringResource(R.string.bed_location, option.roomNumber, option.bedLabel),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    option.floorLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(stringResource(R.string.room_price_per_bed, option.pricePerBed))
                        }
                    }
                }
            }

            OutlinedTextField(
                value = uiState.rent,
                onValueChange = viewModel::onRentChange,
                label = { Text(stringResource(R.string.agreement_field_rent)) },
                supportingText = { Text(stringResource(R.string.transfer_rent_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )

            Button(
                onClick = { viewModel.confirmTransfer(noteText) },
                enabled = uiState.canSave,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(stringResource(R.string.transfer_confirm))
            }
        }
    }
}
