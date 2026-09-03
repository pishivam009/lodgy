package com.lodgy.app.ui.payment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
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
import com.lodgy.app.data.entity.Invoice
import com.lodgy.app.data.entity.InvoiceStatus
import com.lodgy.app.ui.icons.CommonIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceListScreen(
    onRecordPayment: (Invoice) -> Unit,
    onAddManualInvoice: () -> Unit,
    viewModel: InvoiceListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.invoice_list_title)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddManualInvoice) {
                Icon(CommonIcons.Plus, contentDescription = stringResource(R.string.manual_invoice_add))
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                FilterChip(
                    selected = uiState.filter == InvoiceFilter.ALL,
                    onClick = { viewModel.onFilterChange(InvoiceFilter.ALL) },
                    label = { Text(stringResource(R.string.invoice_filter_all)) },
                )
                FilterChip(
                    selected = uiState.filter == InvoiceFilter.UNPAID,
                    onClick = { viewModel.onFilterChange(InvoiceFilter.UNPAID) },
                    label = { Text(stringResource(R.string.invoice_status_unpaid)) },
                )
                FilterChip(
                    selected = uiState.filter == InvoiceFilter.PARTIAL,
                    onClick = { viewModel.onFilterChange(InvoiceFilter.PARTIAL) },
                    label = { Text(stringResource(R.string.invoice_status_partial)) },
                )
                FilterChip(
                    selected = uiState.filter == InvoiceFilter.PAID,
                    onClick = { viewModel.onFilterChange(InvoiceFilter.PAID) },
                    label = { Text(stringResource(R.string.invoice_status_paid)) },
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(uiState.filteredItems, key = { it.invoice.id }) { item ->
                    InvoiceRow(item = item, onRecordPayment = { onRecordPayment(item.invoice) })
                }
            }
        }
    }
}

@Composable
private fun InvoiceRow(item: InvoiceListItem, onRecordPayment: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(item.tenantName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.invoice_period, item.invoice.periodMonth, item.invoice.periodYear),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusChip(item.invoice.status)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (item.invoice.status == InvoiceStatus.PARTIAL) {
                        stringResource(R.string.invoice_amount_of, item.totalPaid, item.invoice.amountDue)
                    } else {
                        stringResource(R.string.currency_amount, item.invoice.amountDue)
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                if (item.invoice.status != InvoiceStatus.PAID) {
                    Button(onClick = onRecordPayment) { Text(stringResource(R.string.invoice_record_payment)) }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: InvoiceStatus) {
    val labelRes = when (status) {
        InvoiceStatus.UNPAID -> R.string.invoice_status_unpaid
        InvoiceStatus.PARTIAL -> R.string.invoice_status_partial
        InvoiceStatus.PAID -> R.string.invoice_status_paid
    }
    SuggestionChip(onClick = {}, label = { Text(stringResource(labelRes)) }, colors = SuggestionChipDefaults.suggestionChipColors())
}
