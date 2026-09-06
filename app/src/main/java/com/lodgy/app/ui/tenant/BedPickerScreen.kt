package com.lodgy.app.ui.tenant

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lodgy.app.R
import com.lodgy.app.data.dao.VacantBedChoice
import com.lodgy.app.ui.common.label
import com.lodgy.app.ui.icons.CommonIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BedPickerScreen(
    onBack: () -> Unit,
    onBedSelected: (VacantBedChoice) -> Unit,
    viewModel: BedPickerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.bed_picker_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(CommonIcons.Back, contentDescription = null) }
                },
            )
        },
    ) { padding ->
        when {
            !uiState.loading && !uiState.hasAnyProperty -> EmptyMessage(padding, R.string.bed_picker_no_hostel)
            !uiState.loading && uiState.properties.isEmpty() -> EmptyMessage(padding, R.string.bed_picker_empty)
            else -> LazyColumn(contentPadding = PaddingValues(16.dp), modifier = Modifier.padding(padding)) {
                uiState.properties.forEach { property ->
                    item(key = property.hostelId) {
                        Text(
                            property.hostelName,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
                        )
                    }
                    property.floors.forEach { floor ->
                        // A single-unit property's floor is the placeholder its hierarchy carries,
                        // never something the warden named, so it is not shown (LODGY-79).
                        if (!property.isSingleUnit) {
                            item(key = "${property.hostelId}-${floor.label}") {
                                Text(
                                    floor.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 6.dp, bottom = 6.dp),
                                )
                            }
                        }
                        items(floor.choices, key = { it.bedId }) { choice ->
                            ChoiceCard(
                                choice = choice,
                                singleUnit = property.isSingleUnit,
                                onClick = { onBedSelected(choice) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChoiceCard(choice: VacantBedChoice, singleUnit: Boolean, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    // A shop, warehouse or flat IS the thing being let, so it is offered whole
                    // rather than as a bed inside a room inside itself (LODGY-79, LODGY-85).
                    if (singleUnit) {
                        stringResource(R.string.bed_picker_whole_unit)
                    } else {
                        stringResource(R.string.bed_picker_option, choice.roomNumber, choice.bedLabel)
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                if (singleUnit) {
                    Text(
                        choice.propertyType.label(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyMessage(padding: PaddingValues, message: Int) {
    Box(modifier = Modifier.padding(padding).fillMaxWidth().padding(32.dp)) {
        Text(
            stringResource(message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
