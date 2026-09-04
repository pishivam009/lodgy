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
                        Text(uiState.hostelName, style = MaterialTheme.typography.bodySmall)
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
            OccupancySummary(uiState)
            // Adaptive rather than a fixed column count so a small phone still gets two per row
            // and a large one gets more, instead of tiles stretching to fill the width.
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 108.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(uiState.items, key = { it.room.roomId }) { item ->
                    RoomTile(item = item, onClick = { onOpenRoom(item.room.roomId) })
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
private fun RoomTile(item: AllRoomsItem, onClick: () -> Unit) {
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
                item.room.floorLabel,
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
