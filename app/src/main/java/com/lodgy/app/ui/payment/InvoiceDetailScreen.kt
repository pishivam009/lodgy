package com.lodgy.app.ui.payment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lodgy.app.R
import com.lodgy.app.data.entity.InvoiceStatus
import com.lodgy.app.ui.common.StatusBadge
import com.lodgy.app.ui.common.icon
import com.lodgy.app.ui.common.label
import com.lodgy.app.ui.common.level
import com.lodgy.app.ui.icons.CommonIcons
import com.lodgy.app.ui.icons.StatusIcons
import com.lodgy.app.ui.theme.LodgyStatus
import com.lodgy.app.ui.theme.StatusLevel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailScreen(
    onBack: () -> Unit,
    onRecordPayment: () -> Unit,
    onSendReminder: () -> Unit,
    onOpenReceipt: () -> Unit,
    viewModel: InvoiceDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.invoice_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(CommonIcons.Back, contentDescription = null) }
                },
            )
        },
    ) { padding ->
        if (!uiState.loading && !uiState.found) {
            Box(modifier = Modifier.padding(padding).fillMaxWidth().padding(32.dp)) {
                Text(
                    stringResource(R.string.acknowledgement_not_found),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    uiState.location?.let {
                        Text(it.label(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    Text(uiState.tenantName, style = MaterialTheme.typography.titleLarge)
                }
                StatusBadge(uiState.status.level, uiState.status.icon, uiState.status.label())
            }

            if (uiState.partOfMultiPeriodPayment) {
                Text(
                    stringResource(R.string.multi_period_badge),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            if (uiState.periodReconciled) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
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

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    DetailRow(stringResource(R.string.acknowledgement_period), stringResource(R.string.invoice_period, uiState.periodMonth, uiState.periodYear))
                    DetailRow(
                        stringResource(R.string.invoice_due_date_label),
                        dateFormat.format(Date(uiState.dueDateMillis)),
                        valueColor = if (uiState.isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    )
                    DetailRow(stringResource(R.string.acknowledgement_invoice_amount), stringResource(R.string.currency_amount, uiState.amountDue))
                    if (uiState.creditTotal > 0.0) {
                        DetailRow(stringResource(R.string.acknowledgement_credit), stringResource(R.string.currency_amount, uiState.creditTotal))
                    }
                    DetailRow(stringResource(R.string.acknowledgement_total_paid), stringResource(R.string.currency_amount, uiState.totalPaid))
                    DetailRow(stringResource(R.string.acknowledgement_amount_due), stringResource(R.string.currency_amount, (uiState.effectiveDue - uiState.totalPaid).coerceAtLeast(0.0)))
                }
            }

            OutlinedButton(onClick = onOpenReceipt, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.acknowledgement_action))
            }
            if (uiState.status != InvoiceStatus.PAID) {
                OutlinedButton(onClick = onSendReminder, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.invoice_send_reminder))
                }
                Button(onClick = onRecordPayment, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.invoice_record_payment))
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor)
    }
}
