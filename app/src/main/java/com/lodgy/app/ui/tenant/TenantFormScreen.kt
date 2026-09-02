package com.lodgy.app.ui.tenant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lodgy.app.R
import com.lodgy.app.ui.icons.CommonIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantFormScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: TenantFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (uiState.isEditing) R.string.tenant_form_title_edit else R.string.tenant_form_title_add)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(CommonIcons.Back, contentDescription = null) }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PhotoPickerField(
                    label = stringResource(R.string.tenant_field_photo),
                    photoPath = uiState.photoPath,
                    onPhotoPicked = { uri -> viewModel.onPhotoPicked(PhotoField.PROFILE, uri) },
                    createCameraOutputUri = viewModel::createCameraOutputUri,
                    modifier = Modifier.weight(1f),
                )
                PhotoPickerField(
                    label = stringResource(R.string.tenant_field_id_proof),
                    photoPath = uiState.idProofPhotoPath,
                    onPhotoPicked = { uri -> viewModel.onPhotoPicked(PhotoField.ID_PROOF, uri) },
                    createCameraOutputUri = viewModel::createCameraOutputUri,
                    modifier = Modifier.weight(1f),
                )
            }

            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.tenant_field_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = uiState.phone,
                onValueChange = viewModel::onPhoneChange,
                label = { Text(stringResource(R.string.tenant_field_phone)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Text(
                stringResource(R.string.tenant_emergency_contact_section),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = uiState.emergencyContactName,
                onValueChange = viewModel::onEmergencyNameChange,
                label = { Text(stringResource(R.string.tenant_field_emergency_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = uiState.emergencyContactPhone,
                onValueChange = viewModel::onEmergencyPhoneChange,
                label = { Text(stringResource(R.string.tenant_field_emergency_phone)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Button(
                onClick = viewModel::save,
                enabled = uiState.canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.tenant_form_save))
            }
        }
    }
}
