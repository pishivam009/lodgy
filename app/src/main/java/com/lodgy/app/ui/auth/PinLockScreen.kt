package com.lodgy.app.ui.auth

import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lodgy.app.R

@Composable
fun PinLockScreen(onUnlocked: () -> Unit, viewModel: PinLockViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? FragmentActivity

    LaunchedEffect(uiState.unlocked) {
        if (uiState.unlocked) onUnlocked()
    }

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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 72.dp),
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

            uiState.error?.let { errorRes ->
                Text(
                    text = stringResource(errorRes),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 10.dp),
                )
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

        Keypad(
            onDigit = viewModel::onDigit,
            onBackspace = viewModel::onBackspace,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 24.dp),
        )
    }
}
