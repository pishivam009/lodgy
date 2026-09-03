package com.lodgy.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lodgy.app.R
import com.lodgy.app.ui.icons.CommonIcons

private data class ReportTile(val value: String, val labelRes: Int, val isPositive: Boolean? = null)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyReportScreen(onBack: () -> Unit, viewModel: MonthlyReportViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.monthly_report_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(CommonIcons.Back, contentDescription = null) }
                },
            )
        },
    ) { padding ->
        if (!uiState.loading && !uiState.hasActiveHostel) {
            Box(modifier = Modifier.padding(padding).fillMaxWidth().padding(32.dp)) {
                Text(
                    stringResource(R.string.dashboard_no_hostel),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }

        Column(modifier = Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(uiState.hostelName, style = MaterialTheme.typography.titleMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = uiState.month.toString(),
                    onValueChange = { value -> value.toIntOrNull()?.let(viewModel::onMonthChange) },
                    label = { Text(stringResource(R.string.manual_invoice_field_month)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = uiState.year.toString(),
                    onValueChange = { value -> value.toIntOrNull()?.let(viewModel::onYearChange) },
                    label = { Text(stringResource(R.string.manual_invoice_field_year)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }

            val tiles = listOf(
                ReportTile(stringResource(R.string.currency_amount, uiState.totalCollected), R.string.monthly_report_collected, true),
                ReportTile(stringResource(R.string.currency_amount, uiState.totalDues), R.string.monthly_report_dues, false),
                ReportTile("${uiState.occupancyPercent}%", R.string.monthly_report_occupancy, null),
                ReportTile(
                    stringResource(R.string.currency_amount, uiState.netIncome),
                    R.string.monthly_report_net_income,
                    uiState.netIncome >= 0,
                ),
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(tiles) { tile -> ReportStatCard(tile) }
            }

            Text(
                stringResource(R.string.monthly_report_expense_line, uiState.totalExpense),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReportStatCard(tile: ReportTile) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                tile.value,
                style = MaterialTheme.typography.headlineSmall,
                color = when (tile.isPositive) {
                    true -> MaterialTheme.colorScheme.primary
                    false -> MaterialTheme.colorScheme.error
                    null -> MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                stringResource(tile.labelRes),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
