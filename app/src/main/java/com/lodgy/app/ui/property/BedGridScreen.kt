package com.lodgy.app.ui.property

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
fun BedGridScreen(onBack: () -> Unit, viewModel: BedGridViewModel = hiltViewModel()) {
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
                items(uiState.filteredBeds, key = Bed::id) { bed -> BedTile(bed) }
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
private fun BedTile(bed: Bed) {
    val palette = LodgyStatus.colors[bed.status.level]
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.container, RoundedCornerShape(14.dp))
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
