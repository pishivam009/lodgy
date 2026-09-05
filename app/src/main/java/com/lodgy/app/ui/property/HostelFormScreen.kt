package com.lodgy.app.ui.property

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lodgy.app.R
import com.lodgy.app.data.entity.PropertyType
import com.lodgy.app.ui.common.FilterChipRow
import com.lodgy.app.ui.common.label
import com.lodgy.app.ui.icons.CommonIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostelFormScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: HostelFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(if (uiState.isEditing) R.string.hostel_form_title_edit else R.string.hostel_form_title_add),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(CommonIcons.Back, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Asked first, and only when creating: it decides whether the warden is setting up a
            // building of floors and beds or a single thing they let as a whole, which changes what
            // the rest of the app shows them (LODGY-79). It cannot be changed afterwards, because
            // the implicit floor and unit are already built underneath.
            if (!uiState.isEditing) {
                Text(
                    stringResource(R.string.property_type_label),
                    style = MaterialTheme.typography.labelLarge,
                )
                FilterChipRow(
                    options = PropertyType.entries,
                    selected = uiState.propertyType,
                    onSelect = viewModel::onPropertyTypeChange,
                    label = { it.label() },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(
                        if (uiState.isSingleUnit) {
                            R.string.property_type_hint_single_unit
                        } else {
                            R.string.property_type_hint_hostel
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(0.dp))
            }

            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.hostel_field_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = uiState.address,
                onValueChange = viewModel::onAddressChange,
                label = { Text(stringResource(R.string.hostel_field_address)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
            OutlinedTextField(
                value = uiState.contactPhone,
                onValueChange = viewModel::onContactPhoneChange,
                label = { Text(stringResource(R.string.hostel_field_phone)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            // The property IS the rentable unit, so its rent belongs here - there is no room form
            // to put it on, because the warden never sees one.
            if (uiState.isSingleUnit) {
                OutlinedTextField(
                    value = uiState.monthlyRent,
                    onValueChange = viewModel::onMonthlyRentChange,
                    label = { Text(stringResource(R.string.property_field_monthly_rent)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            Button(
                onClick = viewModel::save,
                enabled = uiState.canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.hostel_form_save))
            }
        }
    }
}
