package com.lodgy.app.ui.property

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.lodgy.app.data.entity.RoomType
import com.lodgy.app.ui.common.label
import com.lodgy.app.ui.icons.CommonIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomFormScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: RoomFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (uiState.isEditing) R.string.room_form_title_edit else R.string.room_form_title_add)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(CommonIcons.Back, contentDescription = null) }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = uiState.roomNumber,
                onValueChange = viewModel::onRoomNumberChange,
                label = { Text(stringResource(R.string.room_field_number)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RoomType.entries.forEach { type ->
                    FilterChip(
                        selected = uiState.type == type,
                        onClick = { viewModel.onTypeChange(type) },
                        label = { Text(type.label()) },
                    )
                }
            }

            OutlinedTextField(
                value = uiState.pricePerBed,
                onValueChange = viewModel::onPriceChange,
                label = { Text(stringResource(R.string.room_field_price)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = uiState.amenities,
                onValueChange = viewModel::onAmenitiesChange,
                label = { Text(stringResource(R.string.room_field_amenities)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )

            Button(
                onClick = viewModel::save,
                enabled = uiState.canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.room_form_save))
            }
        }
    }

    if (uiState.showTypeChangeConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::dismissTypeChangeConfirm,
            title = { Text(stringResource(R.string.room_type_change_confirm_title)) },
            text = { Text(stringResource(R.string.room_type_change_confirm_body)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmTypeChange) {
                    Text(stringResource(R.string.room_type_change_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissTypeChangeConfirm) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}
