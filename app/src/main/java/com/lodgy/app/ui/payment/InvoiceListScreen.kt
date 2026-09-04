package com.lodgy.app.ui.payment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lodgy.app.R
import com.lodgy.app.data.entity.Invoice
import com.lodgy.app.data.entity.InvoiceStatus
import com.lodgy.app.ui.common.FilterChipRow
import com.lodgy.app.ui.common.StatusBadge
import com.lodgy.app.ui.common.icon
import com.lodgy.app.ui.common.label
import com.lodgy.app.ui.common.level
import com.lodgy.app.ui.icons.CommonIcons
import com.lodgy.app.ui.icons.StatusIcons
import com.lodgy.app.ui.theme.LodgyStatus
import com.lodgy.app.ui.theme.StatusLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceListScreen(
    onRecordPayment: (Invoice) -> Unit,
    onSendReminder: (Invoice) -> Unit,
    onOpenReceipt: (Invoice) -> Unit,
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

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            ) {
                OutlinedTextField(
                    value = uiState.periodMonth,
                    onValueChange = viewModel::onPeriodMonthChange,
                    label = { Text(stringResource(R.string.manual_invoice_field_month)) },
                    placeholder = { Text(stringResource(R.string.period_filter_any)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = uiState.periodYear,
                    onValueChange = viewModel::onPeriodYearChange,
                    label = { Text(stringResource(R.string.manual_invoice_field_year)) },
                    placeholder = { Text(stringResource(R.string.period_filter_any)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }

            FilterChipRow(
                options = InvoiceSort.entries,
                selected = uiState.sort,
                onSelect = viewModel::onSortChange,
                label = {
                    stringResource(
                        if (it == InvoiceSort.DUE_DATE) R.string.invoice_sort_due_date else R.string.invoice_sort_amount,
                    )
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                leadingLabel = stringResource(R.string.filter_sort_by),
            )

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(uiState.filteredItems, key = { it.invoice.id }) { item ->
                    InvoiceRow(
                        item = item,
                        onRecordPayment = { onRecordPayment(item.invoice) },
                        onSendReminder = { onSendReminder(item.invoice) },
                        onOpenReceipt = { onOpenReceipt(item.invoice) },
                    )
                }
            }
        }
    }
}

@Composable
private fun InvoiceRow(
    item: InvoiceListItem,
    onRecordPayment: () -> Unit,
    onSendReminder: () -> Unit,
    onOpenReceipt: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    item.location?.let {
                        Text(
                            it.label(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(item.tenantName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.invoice_period, item.invoice.periodMonth, item.invoice.periodYear),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusChip(item.invoice.status)
            }
            if (item.partOfMultiPeriodPayment) {
                Text(
                    stringResource(R.string.multi_period_badge),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            if (item.creditTotal > 0.0) {
                Text(
                    stringResource(
                        R.string.credit_line_item,
                        stringResource(R.string.currency_amount, item.creditTotal),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (item.periodReconciled) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        StatusIcons.Check,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = LodgyStatus.colors[StatusLevel.GOOD].accent,
                    )
                    Text(
                        stringResource(R.string.reconciliation_badge),
                        style = MaterialTheme.typography.labelMedium,
                        color = LodgyStatus.colors[StatusLevel.GOOD].accent,
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (item.invoice.status == InvoiceStatus.PARTIAL) {
                        stringResource(R.string.invoice_amount_of, item.totalPaid, item.effectiveDue)
                    } else {
                        stringResource(R.string.currency_amount, item.effectiveDue)
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onOpenReceipt) { Text(stringResource(R.string.acknowledgement_action)) }
                    if (item.invoice.status != InvoiceStatus.PAID) {
                        OutlinedButton(onClick = onSendReminder) { Text(stringResource(R.string.invoice_send_reminder)) }
                        Button(onClick = onRecordPayment) { Text(stringResource(R.string.invoice_record_payment)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: InvoiceStatus) {
    StatusBadge(status.level, status.icon, status.label())
}
