package com.lodgy.app.ui.tenant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lodgy.app.R
import com.lodgy.app.ui.icons.CommonIcons
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgreementFormScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: AgreementFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.agreement_form_title)) },
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
                value = uiState.agreedRent,
                onValueChange = viewModel::onAgreedRentChange,
                label = { Text(stringResource(R.string.agreement_field_rent)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = uiState.advanceDeposit,
                onValueChange = viewModel::onAdvanceDepositChange,
                label = { Text(stringResource(R.string.agreement_field_deposit)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = uiState.billingCycleDay,
                onValueChange = viewModel::onBillingCycleDayChange,
                label = { Text(stringResource(R.string.agreement_field_billing_day)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = uiState.billingCycleDay.isNotEmpty() && !uiState.billingCycleDayValid,
                supportingText = { Text(stringResource(R.string.agreement_field_billing_day_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = dateFormat.format(Date(uiState.moveInDateMillis)),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.agreement_field_move_in_date)) },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(CommonIcons.Edit, contentDescription = null)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.agreement_non_revenue),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        stringResource(R.string.agreement_non_revenue_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = uiState.nonRevenue, onCheckedChange = viewModel::onNonRevenueChange)
            }

            if (!uiState.nonRevenue) {
                OutlinedTextField(
                    value = uiState.openingBalance,
                onValueChange = viewModel::onOpeningBalanceChange,
                label = { Text(stringResource(R.string.agreement_field_opening_balance)) },
                supportingText = { Text(stringResource(R.string.agreement_field_opening_balance_hint)) },
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
                Text(stringResource(R.string.agreement_form_save))
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.moveInDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let(viewModel::onMoveInDateChange)
                    showDatePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
