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
import androidx.compose.material3.AssistChip
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
import com.lodgy.app.data.entity.TenantStatus
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

            if (uiState.tenants.isEmpty()) {
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
                    items(uiState.tenants, key = Tenant::id) { tenant ->
                        TenantRow(tenant = tenant, onClick = { onOpenTenant(tenant) })
                    }
                }
            }
        }
    }
}

@Composable
private fun TenantRow(tenant: Tenant, onClick: () -> Unit) {
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
                Text(tenant.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    tenant.phone,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AssistChip(
                onClick = onClick,
                label = { Text(if (tenant.status == TenantStatus.ACTIVE) stringResource(R.string.tenant_status_active) else stringResource(R.string.tenant_status_vacated)) },
            )
        }
    }
}
