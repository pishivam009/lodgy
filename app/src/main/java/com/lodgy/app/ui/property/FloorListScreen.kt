package com.lodgy.app.ui.property

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lodgy.app.R
import com.lodgy.app.data.entity.Floor
import com.lodgy.app.ui.icons.CommonIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloorListScreen(
    onBack: () -> Unit,
    onAddFloor: () -> Unit,
    onEditFloor: (Floor) -> Unit,
    onOpenRooms: (Floor) -> Unit,
    onOpenAllRooms: () -> Unit,
    viewModel: FloorListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<Floor?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Column {
                    Text(stringResource(R.string.floor_list_title))
                    Text(uiState.hostelName, style = MaterialTheme.typography.bodySmall)
                } },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(CommonIcons.Back, contentDescription = null) }
                },
                actions = {
                    TextButton(onClick = onOpenAllRooms) { Text(stringResource(R.string.all_rooms_action)) }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddFloor) {
                Icon(CommonIcons.Plus, contentDescription = stringResource(R.string.floor_add))
            }
        },
    ) { padding ->
        if (uiState.floors.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxWidth().padding(32.dp)) {
                Text(
                    stringResource(R.string.floor_list_empty),
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
                items(uiState.items, key = { it.floor.id }) { item ->
                    FloorRow(
                        item = item,
                        onOpen = { onOpenRooms(item.floor) },
                        onEdit = { onEditFloor(item.floor) },
                        onMoveUp = { viewModel.moveUp(item.floor) },
                        onMoveDown = { viewModel.moveDown(item.floor) },
                        onDelete = { pendingDelete = item.floor },
                    )
                }
            }
        }
    }

    pendingDelete?.let { floor ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.floor_delete_title)) },
            text = { Text(stringResource(R.string.floor_delete_body, floor.label)) },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(floor); pendingDelete = null }) {
                    Text(stringResource(R.string.floor_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun FloorRow(
    item: FloorListItem,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.floor.label, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.floor_bed_summary, item.vacantBeds, item.occupiedBeds),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onMoveUp) { Icon(CommonIcons.ArrowUp, contentDescription = stringResource(R.string.floor_move_up)) }
            IconButton(onClick = onMoveDown) { Icon(CommonIcons.ArrowDown, contentDescription = stringResource(R.string.floor_move_down)) }
            IconButton(onClick = onEdit) { Icon(CommonIcons.Edit, contentDescription = stringResource(R.string.floor_edit)) }
            IconButton(onClick = onDelete) { Icon(CommonIcons.Trash, contentDescription = stringResource(R.string.floor_delete)) }
        }
    }
}
