package com.lodgy.app.ui.payment

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lodgy.app.R
import com.lodgy.app.pdf.AcknowledgementData
import com.lodgy.app.pdf.AcknowledgementLabels
import com.lodgy.app.pdf.AcknowledgementPaymentLine
import com.lodgy.app.pdf.LodgyPdfRenderer
import com.lodgy.app.pdf.buildInvoiceAcknowledgement
import com.lodgy.app.ui.common.label
import com.lodgy.app.ui.icons.CommonIcons
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcknowledgementScreen(onBack: () -> Unit, viewModel: AcknowledgementViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val exportSuccessMessage = stringResource(R.string.monthly_report_export_success)
    val exportFailedMessage = stringResource(R.string.monthly_report_export_failed)

    val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

    val labels = AcknowledgementLabels(
        title = stringResource(R.string.acknowledgement_title),
        tenant = stringResource(R.string.acknowledgement_tenant),
        roomAndBed = stringResource(R.string.acknowledgement_room),
        period = stringResource(R.string.acknowledgement_period),
        invoiceAmount = stringResource(R.string.acknowledgement_invoice_amount),
        credit = stringResource(R.string.acknowledgement_credit),
        amountDue = stringResource(R.string.acknowledgement_amount_due),
        totalPaid = stringResource(R.string.acknowledgement_total_paid),
        balance = stringResource(R.string.acknowledgement_balance),
        paymentsHeading = stringResource(R.string.acknowledgement_payments),
        columnDate = stringResource(R.string.acknowledgement_column_date),
        columnMode = stringResource(R.string.acknowledgement_column_mode),
        columnAmount = stringResource(R.string.acknowledgement_column_amount),
        noPayments = stringResource(R.string.acknowledgement_no_payments),
        issuedOn = stringResource(R.string.acknowledgement_issued_on),
    )

    val data = AcknowledgementData(
        hostelName = uiState.hostelName,
        tenantName = uiState.tenantName,
        roomAndBed = uiState.location?.label().orEmpty(),
        period = stringResource(R.string.invoice_period, uiState.periodMonth, uiState.periodYear),
        invoiceAmount = stringResource(R.string.currency_amount, uiState.invoiceAmount),
        creditAmount = if (uiState.creditTotal > 0.0) {
            stringResource(R.string.currency_amount, uiState.creditTotal)
        } else {
            null
        },
        amountDue = stringResource(R.string.currency_amount, uiState.amountDue),
        totalPaid = stringResource(R.string.currency_amount, uiState.totalPaid),
        balance = stringResource(R.string.currency_amount, uiState.balance),
        payments = uiState.payments.map {
            AcknowledgementPaymentLine(
                date = dateFormat.format(Date(it.paidOn)),
                mode = it.paymentMode.label(),
                amount = stringResource(R.string.currency_amount, it.amount),
            )
        },
        issuedOn = dateFormat.format(Date()),
    )

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            val content = buildInvoiceAcknowledgement(data, labels)
            val success = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { LodgyPdfRenderer().render(content, it) }
                }.isSuccess
            }
            Toast.makeText(context, if (success) exportSuccessMessage else exportFailedMessage, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.acknowledgement_title)) },
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
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    PreviewRow(labels.tenant, data.tenantName)
                    PreviewRow(labels.roomAndBed, data.roomAndBed)
                    PreviewRow(labels.period, data.period)
                    PreviewRow(labels.invoiceAmount, data.invoiceAmount)
                    data.creditAmount?.let { PreviewRow(labels.credit, it) }
                    PreviewRow(labels.totalPaid, data.totalPaid)
                    PreviewRow(labels.balance, data.balance)
                }
            }

            Button(
                onClick = {
                    exportLauncher.launch("lodgy-receipt-${uiState.periodMonth}-${uiState.periodYear}.pdf")
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.acknowledgement_export))
            }
        }
    }
}

@Composable
private fun PreviewRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
