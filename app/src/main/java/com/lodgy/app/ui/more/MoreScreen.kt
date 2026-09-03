package com.lodgy.app.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.activity.ComponentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lodgy.app.BuildConfig
import com.lodgy.app.R
import com.lodgy.app.data.prefs.ThemeMode
import com.lodgy.app.locale.AppLocale
import com.lodgy.app.ui.theme.ThemeViewModel
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

private val ThemeIcon: ImageVector = strokeIcon(
    "MoreTheme",
    "M12,3 A9,9 0 1,0 12,21 A9,9 0 1,0 12,3 Z",
    "M12,3 A9,9 0 0,1 12,21 Z",
)

private val PrintIcon: ImageVector = strokeIcon(
    "MorePrint",
    "M7,9 V4 H17 V9",
    "M5,9 H19 V16 H17",
    "M7,16 H5",
    "M7,13 H17 V20 H7 Z",
)

private val HistoryIcon: ImageVector = strokeIcon(
    "MoreHistory",
    "M12,4 A8,8 0 1,1 4,12",
    "M4,12 L4,7 M4,12 L9,12",
    "M12,8 V12 L15,14",
)

private val LanguageIcon: ImageVector = strokeIcon(
    "MoreLanguage",
    "M12,3 A9,9 0 1,0 12,21 A9,9 0 1,0 12,3 Z",
    "M3,12 H21",
    "M12,3 A14,9 0 0,1 12,21 A14,9 0 0,1 12,3 Z",
)

private data class MoreMenuItem(val labelRes: Int, val icon: ImageVector, val onClick: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onOpenExpenses: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenPrintableRecords: () -> Unit,
    onOpenHistoryImport: () -> Unit,
    themeViewModel: ThemeViewModel = hiltViewModel(),
) {
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()

    val menuItems = listOf(
        MoreMenuItem(R.string.more_expenses, ExpenseIcon, onOpenExpenses),
        MoreMenuItem(R.string.more_backup, BackupIcon, onOpenBackup),
        MoreMenuItem(R.string.packet_action, PrintIcon, onOpenPrintableRecords),
        MoreMenuItem(R.string.more_history_import, HistoryIcon, onOpenHistoryImport),
        MoreMenuItem(R.string.more_theme, ThemeIcon, { showThemeDialog = true }),
        MoreMenuItem(R.string.more_language, LanguageIcon, { showLanguageDialog = true }),
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.more_title)) }) },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.fillMaxWidth().padding(padding),
        ) {
            items(menuItems) { menuItem -> MoreRow(menuItem) }
            item {
                Text(
                    stringResource(R.string.more_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    if (showLanguageDialog) {
        LanguagePickerDialog(onDismiss = { showLanguageDialog = false })
    }

    if (showThemeDialog) {
        ThemePickerDialog(
            selected = themeMode,
            onSelect = themeViewModel::setThemeMode,
            onDismiss = { showThemeDialog = false },
        )
    }
}

/** Applies on tap rather than on a confirm button - the whole screen repaints underneath the
 *  dialog, which is the only preview of the choice worth having. */
@Composable
private fun ThemePickerDialog(selected: ThemeMode, onSelect: (ThemeMode) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.theme_picker_title)) },
        text = {
            Column {
                ThemeMode.entries.forEach { mode ->
                    OptionRow(
                        label = stringResource(
                            when (mode) {
                                ThemeMode.LIGHT -> R.string.theme_light
                                ThemeMode.DARK -> R.string.theme_dark
                                ThemeMode.SYSTEM -> R.string.theme_system
                            },
                        ),
                        selected = selected == mode,
                        onClick = { onSelect(mode) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) }
        },
    )
}

@Composable
private fun LanguagePickerDialog(onDismiss: () -> Unit) {
    var selected by remember { mutableStateOf(AppLocale.current()) }
    val activity = LocalContext.current as? ComponentActivity

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language_picker_title)) },
        text = {
            Column {
                OptionRow(
                    label = stringResource(R.string.language_hindi),
                    selected = selected == AppLocale.HINDI,
                    onClick = { selected = AppLocale.HINDI },
                )
                OptionRow(
                    label = stringResource(R.string.language_english),
                    selected = selected == AppLocale.ENGLISH,
                    onClick = { selected = AppLocale.ENGLISH },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                AppLocale.set(selected)
                onDismiss()
                activity?.recreate()
            }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun OptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 4.dp))
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
