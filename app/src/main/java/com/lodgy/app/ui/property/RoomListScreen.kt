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
import com.lodgy.app.ui.icons.CommonIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomListScreen(
    onBack: () -> Unit,
    onAddRoom: () -> Unit,
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
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddRoom) {
                Icon(CommonIcons.Plus, contentDescription = stringResource(R.string.room_add))
            }
        },
    ) { padding ->
        if (uiState.rooms.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxWidth().padding(32.dp)) {
                Text(
                    stringResource(R.string.room_list_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(padding),
            ) {
                items(uiState.rooms, key = Room::id) { room ->
                    RoomRow(
                        room = room,
                        onOpen = { onOpenBeds(room) },
                        onEdit = { onEditRoom(room) },
                        onDelete = { viewModel.requestDelete(room) },
                    )
                }
            }
        }
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
private fun RoomRow(room: Room, onOpen: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
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
            }
            AssistChip(onClick = onOpen, label = { Text(room.type.name) })
            IconButton(onClick = onEdit) { Icon(CommonIcons.Edit, contentDescription = stringResource(R.string.room_edit)) }
            IconButton(onClick = onDelete) { Icon(CommonIcons.Trash, contentDescription = stringResource(R.string.room_delete)) }
        }
    }
}
