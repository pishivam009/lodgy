package com.lodgy.app.ui.payment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lodgy.app.R
import com.lodgy.app.data.entity.Tenant
import com.lodgy.app.ui.icons.CommonIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualInvoiceTenantPickerScreen(
    onBack: () -> Unit,
    onTenantSelected: (Tenant) -> Unit,
    viewModel: ManualInvoiceTenantPickerViewModel = hiltViewModel(),
) {
    val tenants by viewModel.activeTenants.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manual_invoice_pick_tenant_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(CommonIcons.Back, contentDescription = null) }
                },
            )
        },
    ) { padding ->
        if (tenants.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxWidth().padding(32.dp)) {
                Text(
                    stringResource(R.string.manual_invoice_no_active_tenants),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(padding),
            ) {
                items(tenants, key = Tenant::id) { tenant ->
                    Card(onClick = { onTenantSelected(tenant) }, modifier = Modifier.fillMaxWidth()) {
                        Text(tenant.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(14.dp))
                    }
                }
            }
        }
    }
}
