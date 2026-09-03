package com.lodgy.app.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object AppLocale {
    const val HINDI = "hi"
    const val ENGLISH = "en"

    /** Warden-facing users default to Hindi; only applies the first time, before any explicit choice exists. */
    fun applyDefaultIfUnset() {
        if (AppCompatDelegate.getApplicationLocales().isEmpty) {
            set(HINDI)
        }
    }

    fun set(languageTag: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
    }

    fun current(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (locales.isEmpty) HINDI else locales[0]?.language ?: HINDI
    }
}
