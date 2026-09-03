package com.lodgy.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.lodgy.app.locale.AppLocale
import com.lodgy.app.ui.AppRoot
import com.lodgy.app.ui.theme.LodgyTheme
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
        setContent {
            LodgyTheme {
                AppRoot()
            }
        }
    }
}
