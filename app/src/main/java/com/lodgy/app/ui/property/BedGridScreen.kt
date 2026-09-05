package com.lodgy.app.ui.property

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
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
import com.lodgy.app.data.entity.Bed
import com.lodgy.app.ui.common.BedFilter
import com.lodgy.app.ui.common.FilterChipRow
import com.lodgy.app.ui.common.icon
import com.lodgy.app.ui.common.label
import com.lodgy.app.ui.common.level
import com.lodgy.app.ui.icons.CommonIcons
import com.lodgy.app.ui.theme.LodgyStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BedGridScreen(
    onBack: () -> Unit,
    onAssignTenant: (String) -> Unit = {},
    onViewTenant: (String) -> Unit = {},
    viewModel: BedGridViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.bed_grid_title, uiState.roomNumber))
                        Text(uiState.roomType, style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(CommonIcons.Back, contentDescription = null) }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            RoomDetails(uiState)
            BedFilterChips(
                selected = uiState.filter,
                onSelect = viewModel::onFilterChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiState.filteredBeds, key = Bed::id) { bed ->
                    BedTile(bed, onClick = { viewModel.onBedSelected(bed) })
                }
            }
        }

        uiState.selectedBed?.let { selected ->
            BedActionSheet(
                selected = selected,
                uiState = uiState,
                onDismiss = viewModel::onBedSheetDismissed,
                onAssignTenant = onAssignTenant,
                onViewTenant = onViewTenant,
            )
        }
    }
}


/** Every bed tap opens this, and only the second row changes with the bed's state - so the warden
 *  learns one gesture and nothing navigates on a stray touch of a dense grid (LODGY-69). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BedActionSheet(
    selected: SelectedBed,
    uiState: BedGridUiState,
    onDismiss: () -> Unit,
    onAssignTenant: (String) -> Unit,
    onViewTenant: (String) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 32.dp)) {
            Text(
                stringResource(R.string.bed_sheet_title, uiState.roomNumber, selected.bed.label),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                selected.bed.status.label(),
                style = MaterialTheme.typography.bodySmall,
                color = LodgyStatus.colors[selected.bed.status.level].accent,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            // The room's own details, since a bed has only a label and a status of its own.
            Text(
                stringResource(R.string.room_price_per_bed, uiState.pricePerBed),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (uiState.amenities.isNotBlank()) {
                Text(
                    stringResource(R.string.room_amenities_label, uiState.amenities),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (selected.tenantId != null) {
                Text(
                    stringResource(R.string.bed_sheet_occupied_by, selected.tenantName),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // An occupied bed offers the person in it; a vacant one offers filling it. A bed whose
            // tenancy has gone missing falls back to assign rather than opening a profile that is
            // not there.
            if (selected.tenantId != null) {
                TextButton(onClick = { onDismiss(); onViewTenant(selected.tenantId) }) {
                    Text(stringResource(R.string.bed_sheet_view_tenant))
                }
            } else {
                TextButton(onClick = { onDismiss(); onAssignTenant(selected.bed.id) }) {
                    Text(stringResource(R.string.bed_sheet_assign_tenant))
                }
            }
        }
    }
}

@Composable
internal fun BedFilterChips(selected: BedFilter, onSelect: (BedFilter) -> Unit, modifier: Modifier = Modifier) {
    FilterChipRow(
        options = BedFilter.entries,
        selected = selected,
        onSelect = onSelect,
        label = {
            stringResource(
                when (it) {
                    BedFilter.ALL -> R.string.bed_filter_all
                    BedFilter.VACANT -> R.string.bed_filter_vacant
                    BedFilter.OCCUPIED -> R.string.bed_filter_occupied
                },
            )
        },
        modifier = modifier,
    )
}

@Composable
private fun BedTile(bed: Bed, onClick: () -> Unit) {
    val palette = LodgyStatus.colors[bed.status.level]
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.container, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                stringResource(R.string.bed_label, bed.label),
                style = MaterialTheme.typography.titleMedium,
                color = palette.onContainer,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                val label = bed.status.label()
                Icon(bed.status.icon, contentDescription = label, tint = palette.onContainer, modifier = Modifier.size(16.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = palette.onContainer)
            }
        }
    }
}

/** The room's own details, read-only. Before this, the only way to see what a room had was to open
 *  its edit form - a screen whose whole purpose is changing things - so answering "does 204 have an
 *  attached bathroom?" risked editing the record you came to read (LODGY-71). */
@Composable
private fun RoomDetails(uiState: BedGridUiState) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            stringResource(R.string.room_price_per_bed, uiState.pricePerBed),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (uiState.amenities.isNotBlank()) {
            Text(
                stringResource(R.string.room_amenities_label, uiState.amenities),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
