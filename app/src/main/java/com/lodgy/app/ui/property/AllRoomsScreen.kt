package com.lodgy.app.ui.property

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lodgy.app.R
import com.lodgy.app.ui.common.RoomFill
import com.lodgy.app.ui.common.icon
import com.lodgy.app.ui.common.label
import com.lodgy.app.ui.common.level
import com.lodgy.app.ui.icons.CommonIcons
import com.lodgy.app.ui.theme.LodgyStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllRoomsScreen(
    onBack: () -> Unit,
    onOpenRoom: (String) -> Unit,
    viewModel: AllRoomsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.all_rooms_title))
                        // Says which properties these tiles cover, so the counts are never
                        // ambiguous once the screen can span the whole estate.
                        Text(
                            uiState.filterHostelName ?: stringResource(R.string.dashboard_scope_all),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(CommonIcons.Back, contentDescription = null) }
                },
            )
        },
    ) { padding ->
        if (!uiState.loading && uiState.items.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxWidth().padding(32.dp)) {
                Text(
                    stringResource(R.string.all_rooms_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }

        Column(modifier = Modifier.padding(padding)) {
            if (uiState.hostels.size > 1) {
                HostelFilterRow(uiState, viewModel::onHostelFilterChange)
            }
            SpaceFilterRow(uiState, viewModel::onSpaceFilterChange)
            OccupancySummary(uiState)
            // Adaptive rather than a fixed column count so a small phone still gets two per row
            // and a large one gets more, instead of tiles stretching to fill the width.
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 108.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(uiState.visibleItems, key = { it.room.roomId }) { item ->
                    RoomTile(item = item, showHostel = uiState.filterHostelId == null, onClick = { onOpenRoom(item.room.roomId) })
                }
            }
        }
    }
}

@Composable
private fun OccupancySummary(uiState: AllRoomsUiState) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SummaryPart(RoomFill.EMPTY, uiState.emptyRooms)
        SummaryPart(RoomFill.PARTIAL, uiState.partialRooms)
        SummaryPart(RoomFill.FULL, uiState.fullRooms)
    }
}

@Composable
private fun SummaryPart(occupancy: RoomFill, count: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            occupancy.icon,
            contentDescription = null,
            modifier = Modifier.size(15.dp),
            tint = LodgyStatus.colors[occupancy.level].accent,
        )
        Text(
            stringResource(R.string.all_rooms_summary_part, count, occupancy.label()),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RoomTile(item: AllRoomsItem, showHostel: Boolean, onClick: () -> Unit) {
    val palette = LodgyStatus.colors[item.occupancy.level]
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = palette.container),
    ) {
        Column(
            modifier = Modifier.padding(10.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    item.room.roomNumber,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.onContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Icon(
                    item.occupancy.icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = palette.accent,
                )
            }
            Text(
                // Two hostels can each have a Room 101, so the number alone is ambiguous once the
                // view spans properties - prefix the hostel only when it actually can be.
                if (showHostel && item.room.hostelName.isNotBlank()) {
                    "${item.room.hostelName} · ${item.room.floorLabel}"
                } else {
                    item.room.floorLabel
                },
                style = MaterialTheme.typography.labelSmall,
                color = palette.onContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // Colour says it at a glance; these two lines say it for anyone the colour does not
            // reach - a screen reader, a printout, or red-green colour blindness (LODGY-62).
            Text(
                stringResource(R.string.all_rooms_free_of_total, item.vacantBeds, item.totalBeds),
                style = MaterialTheme.typography.bodySmall,
                color = palette.onContainer,
            )
            Text(
                item.occupancy.label(),
                style = MaterialTheme.typography.labelSmall,
                color = palette.accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HostelFilterRow(uiState: AllRoomsUiState, onSelect: (String?) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
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

/** "Has space" means empty OR partly filled - a partly filled room still has a bed free, and
 *  hiding it would answer "where can I put someone" wrongly. */
@Composable
private fun SpaceFilterRow(uiState: AllRoomsUiState, onSelect: (RoomSpaceFilter) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        FilterChip(
            selected = uiState.spaceFilter == RoomSpaceFilter.ALL,
            onClick = { onSelect(RoomSpaceFilter.ALL) },
            label = { Text(stringResource(R.string.room_filter_all)) },
        )
        FilterChip(
            selected = uiState.spaceFilter == RoomSpaceFilter.HAS_SPACE,
            onClick = { onSelect(RoomSpaceFilter.HAS_SPACE) },
            label = { Text(stringResource(R.string.room_filter_has_space)) },
        )
    }
}
