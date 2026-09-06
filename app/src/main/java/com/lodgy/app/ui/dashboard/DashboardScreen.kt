package com.lodgy.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.text.format.DateUtils
import com.lodgy.app.R
import com.lodgy.app.backup.BackupHealth
import com.lodgy.app.ui.icons.CommonIcons
import com.lodgy.app.ui.icons.StatusIcons
import com.lodgy.app.ui.theme.LodgyStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class StatTile(val value: String, val labelRes: Int, val onClick: (() -> Unit)? = null)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    // Both take the hostel currently filtered on Home (null = all), so the destination shows the
    // same properties the number was counted from (LODGY-89).
    onOpenVacantBeds: (String?) -> Unit = {},
    onOpenOverdue: (String?) -> Unit = {},
    onOpenMonthlyReport: () -> Unit = {},
    onOpenBackup: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose {}
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.dashboard_title))
                        if (uiState.hasActiveHostel) {
                            // The scope caption is not decoration: an unlabelled money total that
                            // silently means "all properties" is worse than one that silently means
                            // "this property", because it is larger and it looks right.
                            Text(
                                uiState.filterHostelName ?: stringResource(R.string.dashboard_scope_all),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                },
                actions = {
                    if (uiState.hasActiveHostel) {
                        IconButton(onClick = onOpenMonthlyReport) {
                            Icon(CommonIcons.Report, contentDescription = stringResource(R.string.dashboard_view_report))
                        }
                    }
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
            if (uiState.hostels.size > 1) {
                HostelFilterRow(uiState, viewModel::onHostelFilterChange)
            }
            val tiles = listOf(
                StatTile(stringResource(R.string.currency_amount, uiState.todaysCollections), R.string.dashboard_collections_today),
                StatTile(
                    uiState.overdueInvoiceCount.toString(),
                    R.string.dashboard_overdue_invoices,
                ) { onOpenOverdue(uiState.filterHostelId) },
                StatTile(
                    uiState.vacantBedCount.toString(),
                    R.string.dashboard_vacant_beds,
                ) { onOpenVacantBeds(uiState.filterHostelId) },
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

            BackupTile(
                health = uiState.backupHealth,
                lastBackupTime = uiState.lastBackupTime,
                running = uiState.backupRunning,
                onBackupNow = viewModel::backupNow,
                onSetUp = onOpenBackup,
            )

            if (uiState.upcomingMoveOuts.isNotEmpty()) {
                Text(stringResource(R.string.dashboard_upcoming_move_outs), style = MaterialTheme.typography.labelLarge)
                val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.upcomingMoveOuts.forEach { moveOut ->
                            Column {
                                Text(moveOut.tenantName, style = MaterialTheme.typography.bodyMedium)
                        if (uiState.filterHostelId == null && moveOut.hostelName.isNotBlank()) {
                            Text(
                                moveOut.hostelName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
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

/**
 * The automatic-backup status, as prominent when it is broken as when it is fine (LODGY-68). The
 * RAG container tint is the LODGY-36 status token, so a failed or stale backup does not read like a
 * working one. The icon on the right runs a backup immediately when a folder is set, or leads to
 * the folder setup when one is not.
 */
@Composable
private fun BackupTile(
    health: BackupHealth,
    lastBackupTime: Long?,
    running: Boolean,
    onBackupNow: () -> Unit,
    onSetUp: () -> Unit,
) {
    val palette = LodgyStatus.colors[health.level]
    // A backup that has never worked or has failed is fixed by (re-)choosing the folder, not by
    // retrying against the folder that is gone. So those two states send the icon to the picker;
    // a set-up, working backup runs on the spot (LODGY-68, AC7).
    val actionIsChooseFolder = health == BackupHealth.NOT_CONFIGURED || health == BackupHealth.FAILED

    val statusText = when (health) {
        BackupHealth.NOT_CONFIGURED -> stringResource(R.string.dashboard_backup_not_set_up)
        BackupHealth.NEVER -> stringResource(R.string.dashboard_backup_never)
        BackupHealth.FAILED -> stringResource(R.string.dashboard_backup_failed)
        BackupHealth.STALE, BackupHealth.RECENT -> {
            val relative = lastBackupTime?.let {
                DateUtils.getRelativeTimeSpanString(
                    it, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
                ).toString()
            }.orEmpty()
            stringResource(R.string.dashboard_backup_last, relative)
        }
    }

    // The whole card is the tap target, not just the icon. The status line says "tap to choose a
    // folder", and in UAT a warden tapped those words, then the card, and got nothing - concluding
    // the feature was broken. An instruction has to be tappable where it is written, and this is the
    // one tile whose job is to stop a lost phone costing the warden everything (LODGY-68).
    Card(
        onClick = if (actionIsChooseFolder) onSetUp else onBackupNow,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = palette.container),
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(28.dp).background(palette.accent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (health.level == com.lodgy.app.ui.theme.StatusLevel.GOOD) StatusIcons.Check else StatusIcons.Alert,
                        contentDescription = null,
                        tint = palette.container,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Column {
                    Text(
                        stringResource(R.string.dashboard_backup_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.onContainer,
                    )
                    Text(statusText, style = MaterialTheme.typography.bodySmall, color = palette.onContainer)
                }
            }
            if (running) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = palette.accent)
            } else {
                IconButton(onClick = { if (actionIsChooseFolder) onSetUp() else onBackupNow() }) {
                    Icon(
                        if (actionIsChooseFolder) CommonIcons.Edit else CommonIcons.Export,
                        contentDescription = stringResource(
                            if (health == BackupHealth.FAILED) R.string.dashboard_backup_repick
                            else if (actionIsChooseFolder) R.string.dashboard_backup_set_up_action
                            else R.string.dashboard_backup_now,
                        ),
                        tint = palette.onContainer,
                    )
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

/**
 * A chevron marks the tiles that lead somewhere. Before this, one tile of four was tappable and
 * looked identical to the three that were not, so a warden learned the tiles were buttons from the
 * one that worked and then met three that did not - which reads as broken rather than as limited
 * (LODGY-89).
 */
@Composable
private fun StatCardContent(tile: StatTile) {
    Column(modifier = Modifier.padding(14.dp)) {
        Text(tile.value, style = MaterialTheme.typography.headlineSmall)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(tile.labelRes),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (tile.onClick != null) {
                Icon(
                    CommonIcons.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

/** Only shown when there is more than one property - a single-hostel warden sees no change. */
@Composable
private fun HostelFilterRow(uiState: DashboardUiState, onSelect: (String?) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        item {
            FilterChip(
                selected = uiState.filterHostelId == null,
                onClick = { onSelect(null) },
                label = { Text(stringResource(R.string.dashboard_scope_all)) },
            )
        }
        items(uiState.hostels) { hostel ->
            FilterChip(
                selected = uiState.filterHostelId == hostel.id,
                onClick = { onSelect(hostel.id) },
                label = { Text(hostel.name) },
            )
        }
    }
}
