package com.lodgy.app.ui.property

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
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
import com.lodgy.app.data.entity.Hostel
import com.lodgy.app.ui.icons.CommonIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostelListScreen(
    onAddHostel: () -> Unit,
    onEditHostel: (String) -> Unit,
    onOpenFloors: (String) -> Unit,
    viewModel: HostelListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.hostel_list_title)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddHostel) {
                Icon(CommonIcons.Plus, contentDescription = stringResource(R.string.hostel_add))
            }
        },
    ) { padding ->
        if (uiState.hostels.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxWidth().padding(32.dp)) {
                Text(
                    text = stringResource(R.string.hostel_list_empty),
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
                items(uiState.hostels, key = Hostel::id) { hostel ->
                    HostelCard(
                        hostel = hostel,
                        selected = hostel.id == uiState.selectedHostelId,
                        onOpen = {
                            viewModel.selectHostel(hostel.id)
                            onOpenFloors(hostel.id)
                        },
                        onEdit = { onEditHostel(hostel.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HostelCard(hostel: Hostel, selected: Boolean, onOpen: () -> Unit, onEdit: () -> Unit) {
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        colors = if (selected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(hostel.name.take(2).uppercase(), style = MaterialTheme.typography.titleSmall)
            }
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(hostel.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    hostel.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SuggestionChip(onClick = onEdit, label = { Text(stringResource(R.string.hostel_edit)) })
        }
    }
}
