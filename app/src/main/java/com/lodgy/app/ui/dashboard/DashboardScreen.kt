package com.lodgy.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lodgy.app.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class StatTile(val value: String, val labelRes: Int, val onClick: (() -> Unit)? = null)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onOpenVacantBeds: () -> Unit = {}, viewModel: DashboardViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.hasActiveHostel) uiState.hostelName else stringResource(R.string.dashboard_title),
                    )
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
            val tiles = listOf(
                StatTile(stringResource(R.string.currency_amount, uiState.todaysCollections), R.string.dashboard_collections_today),
                StatTile(uiState.overdueInvoiceCount.toString(), R.string.dashboard_overdue_invoices),
                StatTile(uiState.vacantBedCount.toString(), R.string.dashboard_vacant_beds, onOpenVacantBeds),
                StatTile(uiState.upcomingMoveOuts.size.toString(), R.string.dashboard_upcoming_move_outs),
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(tiles) { tile -> StatCard(tile) }
            }

            if (uiState.upcomingMoveOuts.isNotEmpty()) {
                Text(stringResource(R.string.dashboard_upcoming_move_outs), style = MaterialTheme.typography.labelLarge)
                val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.upcomingMoveOuts.forEach { moveOut ->
                            Column {
                                Text(moveOut.tenantName, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    dateFormat.format(Date(moveOut.moveOutDateMillis)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(tile: StatTile) {
    val onClick = tile.onClick
    if (onClick != null) {
        Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) { StatCardContent(tile) }
    } else {
        Card(modifier = Modifier.fillMaxWidth()) { StatCardContent(tile) }
    }
}

@Composable
private fun StatCardContent(tile: StatTile) {
    Column(modifier = Modifier.padding(14.dp)) {
        Text(tile.value, style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(tile.labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
