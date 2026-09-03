package com.lodgy.app.ui.payment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lodgy.app.R
import com.lodgy.app.contact.ReminderChannel
import com.lodgy.app.contact.ReminderIntents
import com.lodgy.app.contact.ReminderLanguage
import com.lodgy.app.ui.icons.CommonIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderScreen(onBack: () -> Unit, viewModel: ReminderViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.reminder_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(CommonIcons.Back, contentDescription = null) }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.reminder_channel), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = uiState.channel == ReminderChannel.WHATSAPP,
                    onClick = { viewModel.onChannelChange(ReminderChannel.WHATSAPP) },
                    label = { Text(stringResource(R.string.tenant_contact_whatsapp)) },
                )
                FilterChip(
                    selected = uiState.channel == ReminderChannel.SMS,
                    onClick = { viewModel.onChannelChange(ReminderChannel.SMS) },
                    label = { Text(stringResource(R.string.tenant_contact_sms)) },
                )
            }

            Text(stringResource(R.string.reminder_language), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = uiState.language == ReminderLanguage.HINDI,
                    onClick = { viewModel.onLanguageChange(ReminderLanguage.HINDI) },
                    label = { Text(stringResource(R.string.reminder_language_hindi)) },
                )
                FilterChip(
                    selected = uiState.language == ReminderLanguage.ENGLISH,
                    onClick = { viewModel.onLanguageChange(ReminderLanguage.ENGLISH) },
                    label = { Text(stringResource(R.string.reminder_language_english)) },
                )
            }

            Text(stringResource(R.string.reminder_preview), style = MaterialTheme.typography.labelLarge)
            Text(
                uiState.message,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(10.dp))
                    .padding(14.dp),
                style = MaterialTheme.typography.bodyMedium,
            )

            Text(
                stringResource(R.string.reminder_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val isWhatsApp = uiState.channel == ReminderChannel.WHATSAPP
            Button(
                onClick = {
                    val intent = if (isWhatsApp) {
                        ReminderIntents.whatsApp(uiState.tenantPhone, uiState.message)
                    } else {
                        ReminderIntents.sms(uiState.tenantPhone, uiState.message)
                    }
                    runCatching { context.startActivity(intent) }
                },
                enabled = !uiState.loading,
                colors = if (isWhatsApp) {
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                } else {
                    ButtonDefaults.buttonColors()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(if (isWhatsApp) R.string.reminder_open_whatsapp else R.string.reminder_open_sms),
                )
            }
        }
    }
}
