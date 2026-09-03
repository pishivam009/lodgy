package com.lodgy.app.ui.property

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.lodgy.app.data.entity.Room
import com.lodgy.app.ui.common.FilterChipRow
import com.lodgy.app.ui.common.label
import com.lodgy.app.ui.icons.CommonIcons
import com.lodgy.app.ui.icons.strokeIcon

private val BulkAddIcon = strokeIcon(
    "RoomListBulkAdd",
    "M4,4 H14 V14 H4 Z",
    "M10,10 H20 V20 H10 Z",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomListScreen(
    onBack: () -> Unit,
    onAddRoom: () -> Unit,
    onBulkAddRooms: () -> Unit,
    onEditRoom: (Room) -> Unit,
    onOpenBeds: (Room) -> Unit,
    viewModel: RoomListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.room_list_title))
                        Text(uiState.floorLabel, style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(CommonIcons.Back, contentDescription = null) }
                },
                actions = {
                    IconButton(onClick = onBulkAddRooms) {
                        Icon(BulkAddIcon, contentDescription = stringResource(R.string.room_bulk_add))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddRoom) {
                Icon(CommonIcons.Plus, contentDescription = stringResource(R.string.room_add))
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            FilterChipRow(
                options = RoomFilter.entries,
                selected = uiState.filter,
                onSelect = viewModel::onFilterChange,
                label = {
                    stringResource(
                        when (it) {
                            RoomFilter.ALL -> R.string.room_filter_all
                            RoomFilter.HAS_SPACE -> R.string.room_filter_has_space
                            RoomFilter.FULL -> R.string.room_filter_full
                        },
                    )
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )

            val visible = uiState.filteredItems
            if (visible.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp)) {
                    Text(
                        stringResource(
                            if (uiState.items.isEmpty()) R.string.room_list_empty else R.string.filter_no_match,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(visible, key = { it.room.id }) { item ->
                        RoomRow(
                            item = item,
                            onOpen = { onOpenBeds(item.room) },
                            onEdit = { onEditRoom(item.room) },
                            onDelete = { viewModel.requestDelete(item.room) },
                        )
                    }
                }
            }
        }
    }

    uiState.pendingDeleteRoom?.let { room ->
        AlertDialog(
            onDismissRequest = viewModel::dismissPendingDelete,
            title = { Text(stringResource(R.string.room_delete_confirm_title)) },
            text = { Text(stringResource(R.string.room_delete_confirm_body, room.roomNumber)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) { Text(stringResource(R.string.room_delete_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissPendingDelete) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    uiState.blockedDeleteRoom?.let { room ->
        AlertDialog(
            onDismissRequest = viewModel::dismissBlockedDelete,
            title = { Text(stringResource(R.string.room_delete_blocked_title)) },
            text = { Text(stringResource(R.string.room_delete_blocked_body, room.roomNumber)) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissBlockedDelete) { Text(stringResource(R.string.ok)) }
            },
        )
    }
}

@Composable
private fun RoomRow(item: RoomListItem, onOpen: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val room = item.room
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.room_number_prefix, room.roomNumber),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(R.string.room_price_per_bed, room.pricePerBed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(R.string.room_bed_summary, item.vacantBeds, item.totalBeds),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AssistChip(onClick = onOpen, label = { Text(room.type.label()) })
            IconButton(onClick = onEdit) { Icon(CommonIcons.Edit, contentDescription = stringResource(R.string.room_edit)) }
            IconButton(onClick = onDelete) { Icon(CommonIcons.Trash, contentDescription = stringResource(R.string.room_delete)) }
        }
    }
}
