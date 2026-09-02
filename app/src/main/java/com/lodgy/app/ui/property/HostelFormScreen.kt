package com.lodgy.app.ui.property

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lodgy.app.R
import com.lodgy.app.ui.icons.strokeIcon

private val BackIcon: ImageVector = strokeIcon("HostelFormBack", "M15,18 L9,12 L15,6")

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
                        Icon(BackIcon, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
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

            Button(
                onClick = viewModel::save,
                enabled = uiState.name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.hostel_form_save))
            }
        }
    }
}
