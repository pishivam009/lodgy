package com.lodgy.app.ui.tenant

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.lodgy.app.R
import com.lodgy.app.contact.ContactIntents
import com.lodgy.app.data.entity.TenantStatus
import com.lodgy.app.ui.common.StatusBadge
import com.lodgy.app.ui.common.icon
import com.lodgy.app.ui.common.label
import com.lodgy.app.ui.common.level
import com.lodgy.app.ui.icons.CommonIcons
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantProfileScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onCheckout: (String) -> Unit,
    onTransfer: (String) -> Unit,
    onRecordCredit: (String) -> Unit,
    onPaySeveralMonths: (String) -> Unit,
    onOpenNotes: (String) -> Unit,
    viewModel: TenantProfileViewModel = hiltViewModel(),
) {
    val tenant by viewModel.tenant.collectAsStateWithLifecycle()
    val location by viewModel.location.collectAsStateWithLifecycle()
    val plannedMoveOut by viewModel.plannedMoveOut.collectAsStateWithLifecycle()
    var showNoticePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tenant_profile_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(CommonIcons.Back, contentDescription = null) }
                },
                actions = {
                    IconButton(onClick = { onEdit(viewModel.tenantId) }) {
                        Icon(CommonIcons.Edit, contentDescription = stringResource(R.string.tenant_edit))
                    }
                },
            )
        },
    ) { padding ->
        val current = tenant ?: return@Scaffold
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (current.photoPath != null) {
                AsyncImage(
                    model = current.photoPath,
                    contentDescription = null,
                    modifier = Modifier.size(84.dp).clip(CircleShape),
                )
            } else {
                Column(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(current.name.take(2).uppercase(), style = MaterialTheme.typography.headlineSmall)
                }
            }
            Text(current.name, style = MaterialTheme.typography.titleLarge)
            location?.let {
                Text(
                    it.label(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            StatusBadge(current.status.level, current.status.icon, current.status.label())

            ContactButtonsRow(phone = current.phone)

            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DetailRow(stringResource(R.string.tenant_field_phone), current.phone)
                    DetailRow(stringResource(R.string.tenant_detail_emergency), "${current.emergencyContactName} (${current.emergencyContactPhone})")
                }
            }

            Card(
                onClick = { onOpenNotes(current.id) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.tenant_notes_link), style = MaterialTheme.typography.titleMedium)
                    Icon(CommonIcons.ChevronRight, contentDescription = null)
                }
            }

            if (current.status == TenantStatus.ACTIVE) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.tenant_notice_title), style = MaterialTheme.typography.titleMedium)
                        Text(
                            plannedMoveOut?.let { noticeDateFormat.format(Date(it)) }
                                ?: stringResource(R.string.tenant_notice_none),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            stringResource(R.string.tenant_notice_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { showNoticePicker = true }) {
                                Text(stringResource(R.string.tenant_notice_set))
                            }
                            if (plannedMoveOut != null) {
                                TextButton(onClick = { viewModel.setPlannedMoveOut(null) }) {
                                    Text(stringResource(R.string.tenant_notice_clear))
                                }
                            }
                        }
                    }
                }

                Card(
                    onClick = { onPaySeveralMonths(current.id) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.multi_period_action), style = MaterialTheme.typography.titleMedium)
                        Icon(CommonIcons.ChevronRight, contentDescription = null)
                    }
                }

                Card(
                    onClick = { onRecordCredit(current.id) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.credit_action), style = MaterialTheme.typography.titleMedium)
                        Icon(CommonIcons.ChevronRight, contentDescription = null)
                    }
                }

                Card(
                    onClick = { onTransfer(current.id) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.transfer_action), style = MaterialTheme.typography.titleMedium)
                        Icon(CommonIcons.ChevronRight, contentDescription = null)
                    }
                }

                Card(
                    onClick = { onCheckout(current.id) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        stringResource(R.string.tenant_checkout_action),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }

    if (showNoticePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = plannedMoveOut ?: System.currentTimeMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showNoticePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let(viewModel::setPlannedMoveOut)
                    showNoticePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showNoticePicker = false }) { Text(stringResource(R.string.cancel)) }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private val noticeDateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ContactButtonsRow(phone: String) {
    val context = LocalContext.current
    val whatsAppUnavailable = stringResource(R.string.tenant_contact_whatsapp_unavailable)

    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
    ) {
        ContactButton(ContactIcons.Call, stringResource(R.string.tenant_contact_call), Modifier.weight(1f)) {
            runCatching { context.startActivity(ContactIntents.dial(phone)) }
        }
        ContactButton(ContactIcons.WhatsApp, stringResource(R.string.tenant_contact_whatsapp), Modifier.weight(1f)) {
            runCatching { context.startActivity(ContactIntents.whatsApp(phone)) }
                .onFailure { if (it is ActivityNotFoundException) Toast.makeText(context, whatsAppUnavailable, Toast.LENGTH_SHORT).show() }
        }
        ContactButton(ContactIcons.Sms, stringResource(R.string.tenant_contact_sms), Modifier.weight(1f)) {
            runCatching { context.startActivity(ContactIntents.sms(phone)) }
        }
    }
}

@Composable
private fun ContactButton(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
