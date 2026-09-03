package com.lodgy.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lodgy.app.data.prefs.ThemeMode
import com.lodgy.app.locale.AppLocale
import com.lodgy.app.notify.EXTRA_NOTIFICATION_ROUTE
import com.lodgy.app.ui.AppRoot
import com.lodgy.app.ui.theme.LodgyTheme
import com.lodgy.app.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * AppCompatActivity (not a plain FragmentActivity) is required for per-app language
 * switching: on API 33+, AppCompatDelegate.setApplicationLocales() only reaches the
 * platform LocaleManager when at least one AppCompatDelegate is registered via an
 * Activity's lifecycle (AppCompatDelegate.getLocaleManagerForApplication() walks
 * AppCompatDelegate's own active-delegate set) - without it, the call silently no-ops.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Must run after super.onCreate() - that's what registers this Activity's
        // AppCompatDelegate, which setApplicationLocales() needs on API 33+ (see above).
        // Calling this from LodgyApplication.onCreate() would run too early, before any
        // delegate exists, and silently no-op.
        AppLocale.applyDefaultIfUnset()
        enableEdgeToEdge()
        // Delivered by a notification tap; replayed once the app is past the PIN gate rather
        // than navigated to now, because the NavHost does not exist until then.
        val notificationRoute = intent?.getStringExtra(EXTRA_NOTIFICATION_ROUTE)

        setContent {
            val themeMode by hiltViewModel<ThemeViewModel>().themeMode.collectAsStateWithLifecycle()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { /* Denied is fine - the app works without notifications (LODGY-59 AC 5). */ }
                LaunchedEffect(Unit) { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
            }

            LodgyTheme(
                darkTheme = when (themeMode) {
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                },
            ) {
                AppRoot(pendingRoute = notificationRoute)
            }
        }
    }
}
