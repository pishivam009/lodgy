package com.lodgy.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lodgy.app.R
import com.lodgy.app.ui.common.label
import com.lodgy.app.ui.common.BedFilter
import com.lodgy.app.ui.common.StatusBadge
import com.lodgy.app.ui.common.icon
import com.lodgy.app.ui.property.BedFilterChips
import com.lodgy.app.ui.common.label
import com.lodgy.app.ui.common.level
import com.lodgy.app.ui.icons.CommonIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VacantViewScreen(onBack: () -> Unit, viewModel: VacantViewViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.vacant_view_title)) },
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

        Column(modifier = Modifier.padding(padding)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                FilterChip(
                    selected = uiState.selectedFloorId == null,
                    onClick = { viewModel.onFloorFilterChange(null) },
                    label = { Text(stringResource(R.string.vacant_view_all_floors)) },
                )
                uiState.floors.forEach { floor ->
                    FilterChip(
                        selected = uiState.selectedFloorId == floor.id,
                        onClick = { viewModel.onFloorFilterChange(floor.id) },
                        label = { Text(floor.label) },
                    )
                }
            }

            BedFilterChips(
                selected = uiState.statusFilter,
                onSelect = viewModel::onStatusFilterChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            )

            if (!uiState.loading && uiState.filteredItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp)) {
                    Text(
                        stringResource(
                            if (uiState.statusFilter == BedFilter.VACANT) {
                                R.string.vacant_view_empty
                            } else {
                                R.string.filter_no_match
                            },
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                val grouped = uiState.filteredItems.groupBy { it.floorLabel }
                LazyColumn(contentPadding = PaddingValues(16.dp)) {
                    grouped.forEach { (floorLabel, items) ->
                        item {
                            Text(
                                stringResource(R.string.vacant_view_floor_count, floorLabel, items.size),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp, bottom = 6.dp),
                            )
                        }
                        items(items) { item -> VacantBedRow(item) }
                    }
                }
            }
        }
    }
}

@Composable
private fun VacantBedRow(item: VacantBedItem) {
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                stringResource(R.string.vacant_view_room_bed, item.roomNumber, item.bedLabel),
                style = MaterialTheme.typography.titleMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                AssistChip(onClick = {}, label = { Text(item.roomType.label()) })
                StatusBadge(item.status.level, item.status.icon, item.status.label())
            }
        }
    }
}
