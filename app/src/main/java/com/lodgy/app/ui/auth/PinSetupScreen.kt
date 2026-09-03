package com.lodgy.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lodgy.app.R
import com.lodgy.app.ui.common.FilterChipRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinSetupScreen(onComplete: () -> Unit, viewModel: PinSetupViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.step) {
        if (uiState.step == PinSetupStep.DONE) onComplete()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.pin_setup_title)) }) },
    ) { padding ->
        when (uiState.step) {
            PinSetupStep.ENTER, PinSetupStep.CONFIRM -> PinEntryContent(
                uiState = uiState,
                onDigit = viewModel::onDigit,
                onBackspace = viewModel::onBackspace,
                onLengthChange = viewModel::onLengthChange,
                onSubmit = viewModel::onSubmit,
                modifier = Modifier.padding(padding),
            )
            PinSetupStep.BIOMETRIC -> BiometricOptInContent(
                enabled = uiState.biometricEnabled,
                onToggle = viewModel::onBiometricToggle,
                onContinue = viewModel::onFinishBiometricStep,
                modifier = Modifier.padding(padding),
            )
            PinSetupStep.DONE -> Unit
        }
    }
}

@Composable
private fun PinEntryContent(
    uiState: PinSetupUiState,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onLengthChange: (Int) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(AuthIcons.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }

            Text(
                text = stringResource(
                    if (uiState.step == PinSetupStep.CONFIRM) R.string.pin_setup_headline_confirm else R.string.pin_setup_headline_enter,
                ),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = stringResource(
                    if (uiState.step == PinSetupStep.CONFIRM) R.string.pin_setup_sub_confirm else R.string.pin_setup_sub_enter,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, start = 24.dp, end = 24.dp),
            )

            if (uiState.step == PinSetupStep.ENTER) {
                FilterChipRow(
                    options = uiState.lengthOptions,
                    selected = uiState.pinLength,
                    onSelect = onLengthChange,
                    label = { stringResource(R.string.pin_setup_length_option, it) },
                    modifier = Modifier.padding(top = 16.dp),
                    leadingLabel = stringResource(R.string.pin_setup_length),
                )
            }

            PinDots(length = uiState.pinLength, filledCount = uiState.enteredDigits.length, modifier = Modifier.padding(top = 24.dp))

            uiState.error?.let { errorRes ->
                Text(
                    text = stringResource(errorRes),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 24.dp),
        ) {
            Keypad(onDigit = onDigit, onBackspace = onBackspace)
            Button(
                onClick = onSubmit,
                enabled = uiState.canSubmit,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) {
                Text(
                    stringResource(
                        if (uiState.step == PinSetupStep.CONFIRM) {
                            R.string.pin_setup_confirm_action
                        } else {
                            R.string.pin_setup_continue
                        },
                    ),
                )
            }
        }
    }
}

@Composable
private fun BiometricOptInContent(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(AuthIcons.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Text(
                text = stringResource(R.string.pin_setup_biometric_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = stringResource(R.string.pin_setup_biometric_sub),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.pin_setup_biometric_toggle), style = MaterialTheme.typography.bodyMedium)
                Switch(checked = enabled, onCheckedChange = onToggle)
            }
        }

        Button(
            onClick = onContinue,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp),
        ) {
            Text(stringResource(R.string.pin_setup_continue))
        }
    }
}
