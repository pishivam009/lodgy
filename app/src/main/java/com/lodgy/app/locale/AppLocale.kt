package com.lodgy.app.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object AppLocale {
    const val HINDI = "hi"
    const val ENGLISH = "en"

    /** Only applies the first time, before any explicit choice exists - an install that already
     *  stored a choice keeps it, including Hindi picked under the old default. */
    fun applyDefaultIfUnset() {
        if (AppCompatDelegate.getApplicationLocales().isEmpty) {
            set(ENGLISH)
        }
    }

    fun set(languageTag: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
    }

    fun current(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (locales.isEmpty) ENGLISH else locales[0]?.language ?: ENGLISH
    }
}
