package com.lodgy.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lodgy.app.ui.auth.PinLockScreen
import com.lodgy.app.ui.auth.PinSetupScreen
import com.lodgy.app.ui.nav.LodgyNavHost

@Composable
fun AppRoot(viewModel: AppRootViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (state) {
        AppStartState.Loading -> Unit
        AppStartState.NeedsPinSetup -> PinSetupScreen(onComplete = viewModel::onPinSetupComplete)
        AppStartState.Locked -> PinLockScreen(onUnlocked = viewModel::onUnlocked)
        AppStartState.Unlocked -> LodgyNavHost()
    }
}
