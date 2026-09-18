package com.lodgy.app.ui.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lodgy.app.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PinLockScreen(
    onUnlocked: () -> Unit,
    onPinReset: () -> Unit = {},
    viewModel: PinLockViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? FragmentActivity

    LaunchedEffect(uiState.unlocked) {
        if (uiState.unlocked) onUnlocked()
    }
    LaunchedEffect(uiState.pinReset) {
        if (uiState.pinReset) onPinReset()
    }

    // Counts the backoff delay down once a second (LODGY-77), refreshing the "try again in ~Ns"
    // message and re-enabling entry the moment it elapses.
    var lockRemainingSeconds by remember { mutableStateOf(0) }
    LaunchedEffect(uiState.lockedUntilMillis) {
        val until = uiState.lockedUntilMillis
        if (until == null) {
            lockRemainingSeconds = 0
            return@LaunchedEffect
        }
        while (true) {
            val remainingMs = until - System.currentTimeMillis()
            if (remainingMs <= 0) {
                lockRemainingSeconds = 0
                viewModel.onLockoutElapsed()
                break
            }
            lockRemainingSeconds = ((remainingMs + 999) / 1000).toInt()
            kotlinx.coroutines.delay(500)
        }
    }
    val lockedOut = uiState.lockedUntilMillis != null && lockRemainingSeconds > 0

    val onBiometricSuccess by rememberUpdatedState(viewModel::onBiometricSuccess)

    LaunchedEffect(uiState.biometricEnabled, activity) {
        val host = activity ?: return@LaunchedEffect
        if (!uiState.biometricEnabled) return@LaunchedEffect

        val prompt = BiometricPrompt(
            host,
            ContextCompat.getMainExecutor(host),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onBiometricSuccess()
                }
                // Any error (lockout, cancellation, no hardware, too many attempts, negative
                // button) is a graceful fallback: the PIN keypad underneath stays usable.
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = Unit
            },
        )
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(host.getString(R.string.pin_lock_biometric_title))
            .setNegativeButtonText(host.getString(R.string.pin_lock_use_pin))
            .build()
        prompt.authenticate(promptInfo)
    }

    if (uiState.showForgot) {
        ForgotPinFlow(
            uiState = uiState,
            onExportPicked = viewModel::onBackupExportPicked,
            onResetConfirmed = viewModel::onResetConfirmed,
            onDismiss = viewModel::onForgotDismissed,
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "L",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                text = stringResource(R.string.pin_lock_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 28.dp),
            )

            PinDots(length = uiState.pinLength, filledCount = uiState.enteredDigits.length, modifier = Modifier.padding(top = 20.dp))

            if (lockedOut) {
                Text(
                    text = stringResource(R.string.pin_lock_too_many_attempts, lockRemainingSeconds),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 10.dp, start = 32.dp, end = 32.dp),
                )
            } else {
                uiState.error?.let { errorRes ->
                    Text(
                        text = stringResource(errorRes),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }

            if (uiState.biometricEnabled) {
                Box(
                    modifier = Modifier
                        .padding(top = 20.dp)
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(AuthIcons.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Kept out of the scrollable region above, alongside the keypad, so it never has to
        // compete with the icon/title/dots for space - a locked-out warden must always be able to
        // reach it without knowing to scroll first (LODGY-76, AC1; found not reaching a sighted
        // tap at all before this fix, LODGY-102).
        TextButton(
            onClick = viewModel::onForgotPinClicked,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp),
        ) {
            Text(stringResource(R.string.forgot_pin_link))
        }

        Keypad(
            onDigit = viewModel::onDigit,
            onBackspace = viewModel::onBackspace,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(horizontal = 24.dp, vertical = 24.dp),
        )
    }
}

/**
 * The forgotten-PIN recovery flow (LODGY-76): save a backup first, then - and only then - offer to
 * clear the PIN. A warden with fingerprint unlock is pointed back to it before any of this, since
 * it costs them nothing (AC6). Nothing is destroyed until the export has succeeded and the reset is
 * explicitly confirmed (AC3-AC5).
 */
@Composable
private fun ForgotPinFlow(
    uiState: PinLockUiState,
    onExportPicked: (android.net.Uri) -> Unit,
    onResetConfirmed: () -> Unit,
    onDismiss: () -> Unit,
) {
    var showResetConfirm by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri -> uri?.let(onExportPicked) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.forgot_pin_title), style = MaterialTheme.typography.headlineSmall)

        if (uiState.biometricEnabled) {
            Text(
                stringResource(R.string.forgot_pin_biometric_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.forgot_pin_use_biometric))
            }
        }

        Text(
            stringResource(R.string.forgot_pin_explanation),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(
            onClick = {
                val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                exportLauncher.launch("lodgy-backup-$stamp.zip")
            },
            enabled = !uiState.exporting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (uiState.exporting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp).padding(end = 8.dp))
            }
            Text(stringResource(R.string.forgot_pin_save_backup))
        }

        uiState.forgotMessage?.let { messageRes ->
            Text(
                stringResource(messageRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        // Only reachable once a backup is in hand (AC3).
        if (uiState.backupExported) {
            Button(
                onClick = { showResetConfirm = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.forgot_pin_reset))
            }
        }

        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.forgot_pin_back))
        }

        Text(
            stringResource(R.string.forgot_pin_data_safe_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(stringResource(R.string.forgot_pin_reset_confirm_title)) },
            text = { Text(stringResource(R.string.forgot_pin_reset_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showResetConfirm = false
                    onResetConfirmed()
                }) {
                    Text(stringResource(R.string.forgot_pin_reset_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}
