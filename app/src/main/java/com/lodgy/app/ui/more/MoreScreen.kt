package com.lodgy.app.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lodgy.app.R
import com.lodgy.app.ui.icons.CommonIcons
import com.lodgy.app.ui.icons.strokeIcon

private val ExpenseIcon: ImageVector = strokeIcon(
    "MoreExpense",
    "M12,2 V22",
    "M8,6 H14 A3,3 0 0,1 14,12 H10 A3,3 0 0,0 10,18 H16",
)

private val BackupIcon: ImageVector = strokeIcon(
    "MoreBackup",
    "M12,3 V15",
    "M7,10 L12,15 L17,10",
    "M4,19 H20",
)

private data class MoreMenuItem(val labelRes: Int, val icon: ImageVector, val onClick: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(onOpenExpenses: () -> Unit, onOpenBackup: () -> Unit) {
    val menuItems = listOf(
        MoreMenuItem(R.string.more_expenses, ExpenseIcon, onOpenExpenses),
        MoreMenuItem(R.string.more_backup, BackupIcon, onOpenBackup),
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.more_title)) }) },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.fillMaxWidth().padding(padding),
        ) {
            items(menuItems) { menuItem -> MoreRow(menuItem) }
        }
    }
}

@Composable
private fun MoreRow(menuItem: MoreMenuItem) {
    Card(onClick = menuItem.onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(menuItem.icon, contentDescription = null)
                Text(stringResource(menuItem.labelRes), style = MaterialTheme.typography.titleMedium)
            }
            Icon(CommonIcons.ChevronRight, contentDescription = null)
        }
    }
}
