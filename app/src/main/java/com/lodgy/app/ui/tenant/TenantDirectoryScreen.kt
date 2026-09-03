package com.lodgy.app.ui.tenant

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.lodgy.app.R
import com.lodgy.app.data.entity.Tenant
import com.lodgy.app.ui.common.FilterChipRow
import com.lodgy.app.ui.common.StatusBadge
import com.lodgy.app.ui.common.icon
import com.lodgy.app.ui.common.label
import com.lodgy.app.ui.common.level
import com.lodgy.app.ui.icons.CommonIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantDirectoryScreen(
    onAddTenant: () -> Unit,
    onOpenTenant: (Tenant) -> Unit,
    viewModel: TenantDirectoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tenant_directory_title)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTenant) {
                Icon(CommonIcons.Plus, contentDescription = stringResource(R.string.tenant_add))
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                label = { Text(stringResource(R.string.tenant_search_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )

            FilterChipRow(
                options = TenantFilter.entries,
                selected = uiState.filter,
                onSelect = viewModel::onFilterChange,
                label = {
                    stringResource(
                        if (it == TenantFilter.ACTIVE) R.string.tenant_filter_active else R.string.tenant_filter_all,
                    )
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )

            FilterChipRow(
                options = TenantSort.entries,
                selected = uiState.sort,
                onSelect = viewModel::onSortChange,
                label = {
                    stringResource(
                        if (it == TenantSort.NAME) R.string.tenant_sort_name else R.string.tenant_sort_room,
                    )
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                leadingLabel = stringResource(R.string.filter_sort_by),
            )

            if (uiState.hiddenByFilter > 0) {
                Text(
                    stringResource(R.string.tenant_hidden_by_filter, uiState.hiddenByFilter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            if (uiState.items.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp)) {
                    Text(
                        stringResource(R.string.tenant_directory_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(uiState.items, key = { it.tenant.id }) { item ->
                        TenantRow(item = item, onClick = { onOpenTenant(item.tenant) })
                    }
                }
            }
        }
    }
}

@Composable
private fun TenantRow(item: TenantDirectoryItem, onClick: () -> Unit) {
    val tenant = item.tenant
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (tenant.photoPath != null) {
                AsyncImage(
                    model = tenant.photoPath,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp).clip(CircleShape),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(tenant.name.take(2).uppercase(), style = MaterialTheme.typography.labelMedium)
                }
            }
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                if (item.location != null) {
                    Text(
                        item.location.label(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(tenant.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    tenant.phone,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusBadge(tenant.status.level, tenant.status.icon, tenant.status.label())
        }
    }
}
