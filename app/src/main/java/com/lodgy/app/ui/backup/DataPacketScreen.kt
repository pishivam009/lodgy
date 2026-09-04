package com.lodgy.app.ui.backup

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import com.lodgy.app.pdf.LodgyPdfRenderer
import com.lodgy.app.pdf.PacketFloor
import com.lodgy.app.pdf.PacketHostel
import com.lodgy.app.pdf.PacketLabels
import com.lodgy.app.pdf.PacketTenancy
import com.lodgy.app.pdf.buildDataPacket
import com.lodgy.app.ui.TrustedActivityLaunch
import com.lodgy.app.ui.common.FilterChipRow
import com.lodgy.app.ui.icons.CommonIcons
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataPacketScreen(onBack: () -> Unit, viewModel: DataPacketViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val exportSuccessMessage = stringResource(R.string.monthly_report_export_success)
    val exportFailedMessage = stringResource(R.string.monthly_report_export_failed)

    val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
    val activeLabel = stringResource(R.string.tenant_status_active)
    val vacatedLabel = stringResource(R.string.tenant_status_vacated)

    val labels = PacketLabels(
        title = stringResource(R.string.packet_title),
        address = stringResource(R.string.packet_address),
        beds = stringResource(R.string.packet_beds),
        phone = stringResource(R.string.tenant_field_phone),
        status = stringResource(R.string.packet_status),
        rent = stringResource(R.string.agreement_field_rent),
        movedIn = stringResource(R.string.packet_moved_in),
        movedOut = stringResource(R.string.packet_moved_out),
        noticeGiven = stringResource(R.string.packet_notice_given),
        invoicesHeading = stringResource(R.string.packet_invoices),
        columnPeriod = stringResource(R.string.acknowledgement_period),
        columnDue = stringResource(R.string.acknowledgement_amount_due),
        columnPaid = stringResource(R.string.acknowledgement_total_paid),
        columnStatus = stringResource(R.string.packet_status),
        noInvoices = stringResource(R.string.acknowledgement_no_payments),
        noTenants = stringResource(R.string.packet_no_tenants),
        generatedOn = stringResource(R.string.packet_generated_on),
    )

    val hostels = uiState.hostels.map { hostel ->
        PacketHostel(
            hostelName = hostel.hostelName,
            address = hostel.address,
            bedSummary = context.getString(
                R.string.floor_bed_summary,
                hostel.totalBeds - hostel.occupiedBeds,
                hostel.occupiedBeds,
            ),
            floors = hostel.floors.map { floor ->
                PacketFloor(
                    floorLabel = floor.floorLabel,
                    tenancies = floor.tenancies.map { tenancy ->
                        PacketTenancy(
                            tenantName = tenancy.tenantName,
                            phone = tenancy.phone,
                            roomAndBed = context.getString(
                                R.string.bed_location,
                                tenancy.roomNumber,
                                tenancy.bedLabel,
                            ),
                            status = if (tenancy.active) activeLabel else vacatedLabel,
                            agreedRent = context.getString(R.string.currency_amount, tenancy.agreedRent),
                            moveInDate = dateFormat.format(Date(tenancy.moveInDate)),
                            moveOutDate = tenancy.moveOutDate?.let { dateFormat.format(Date(it)) },
                            moveOutIsPlanned = tenancy.active,
                            invoiceRows = tenancy.invoices.map { invoice ->
                                listOf(
                                    context.getString(
                                        R.string.invoice_period,
                                        invoice.periodMonth,
                                        invoice.periodYear,
                                    ),
                                    context.getString(R.string.currency_amount, invoice.amountDue),
                                    context.getString(R.string.currency_amount, invoice.paid),
                                    invoice.status.name,
                                )
                            },
                        )
                    },
                )
            },
        )
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            val content = buildDataPacket(hostels, labels, dateFormat.format(Date()))
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
                title = { Text(stringResource(R.string.packet_title)) },
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
            Text(
                stringResource(R.string.packet_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            FilterChipRow(
                options = PacketScope.entries,
                selected = uiState.scope,
                onSelect = viewModel::onScopeChange,
                label = {
                    stringResource(
                        if (it == PacketScope.CURRENT_HOSTEL) {
                            R.string.packet_scope_current
                        } else {
                            R.string.packet_scope_all
                        },
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                stringResource(R.string.packet_summary, hostels.size, hostels.sumOf { hostel -> hostel.floors.sumOf { it.tenancies.size } }),
                style = MaterialTheme.typography.bodyMedium,
            )

            Button(
                onClick = {
                    val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    TrustedActivityLaunch.expectOne()
                    exportLauncher.launch("lodgy-records-$stamp.pdf")
                },
                enabled = !uiState.loading && hostels.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.packet_export_action))
            }
        }
    }
}
