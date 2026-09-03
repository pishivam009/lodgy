package com.lodgy.app.ui.payment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
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
import com.lodgy.app.ui.common.FilterChipRow
import com.lodgy.app.ui.icons.CommonIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditFormScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: CreditFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.credit_form_title))
                        Text(uiState.tenantName, style = MaterialTheme.typography.bodySmall)
                    }
                },
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
            OutlinedTextField(
                value = uiState.amount,
                onValueChange = viewModel::onAmountChange,
                label = { Text(stringResource(R.string.credit_field_amount)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = uiState.reason,
                onValueChange = viewModel::onReasonChange,
                label = { Text(stringResource(R.string.credit_field_reason)) },
                supportingText = { Text(stringResource(R.string.credit_reason_hint)) },
                modifier = Modifier.fillMaxWidth(),
            )

            Text(stringResource(R.string.credit_apply_to), style = MaterialTheme.typography.labelLarge)
            FilterChipRow(
                options = listOf(null) + uiState.openInvoices.map { it.id },
                selected = uiState.selectedInvoiceId,
                onSelect = viewModel::onInvoiceSelected,
                label = { invoiceId ->
                    val invoice = uiState.openInvoices.firstOrNull { it.id == invoiceId }
                    if (invoice == null) {
                        stringResource(R.string.credit_apply_next)
                    } else {
                        stringResource(R.string.invoice_period, invoice.periodMonth, invoice.periodYear)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = viewModel::save,
                enabled = uiState.canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.credit_form_save))
            }
        }
    }
}
