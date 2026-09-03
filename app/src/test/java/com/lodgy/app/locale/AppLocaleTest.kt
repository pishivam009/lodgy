package com.lodgy.app.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class AppLocaleTest {

    @After
    fun tearDown() {
        unmockkStatic(AppCompatDelegate::class)
        unmockkStatic(LocaleListCompat::class)
    }

    @Test
    fun `applyDefaultIfUnset sets hindi when no explicit locale has been chosen`() {
        mockkStatic(AppCompatDelegate::class)
        mockkStatic(LocaleListCompat::class)
        val emptyList: LocaleListCompat = mockk()
        every { emptyList.isEmpty } returns true
        every { AppCompatDelegate.getApplicationLocales() } returns emptyList
        val hindiList: LocaleListCompat = mockk()
        every { LocaleListCompat.forLanguageTags("hi") } returns hindiList
        every { AppCompatDelegate.setApplicationLocales(hindiList) } returns Unit

        AppLocale.applyDefaultIfUnset()

        verify { AppCompatDelegate.setApplicationLocales(hindiList) }
    }

    @Test
    fun `applyDefaultIfUnset does nothing once a locale is already set`() {
        mockkStatic(AppCompatDelegate::class)
        val existing: LocaleListCompat = mockk()
        every { existing.isEmpty } returns false
        every { AppCompatDelegate.getApplicationLocales() } returns existing

        AppLocale.applyDefaultIfUnset()

        verify(exactly = 0) { AppCompatDelegate.setApplicationLocales(any()) }
    }

    @Test
    fun `set applies the given language tag`() {
        mockkStatic(AppCompatDelegate::class)
        mockkStatic(LocaleListCompat::class)
        val englishList: LocaleListCompat = mockk()
        every { LocaleListCompat.forLanguageTags("en") } returns englishList
        every { AppCompatDelegate.setApplicationLocales(englishList) } returns Unit

        AppLocale.set("en")

        verify { AppCompatDelegate.setApplicationLocales(englishList) }
    }

    @Test
    fun `current returns hindi when no locale is set`() {
        mockkStatic(AppCompatDelegate::class)
        val empty: LocaleListCompat = mockk()
        every { empty.isEmpty } returns true
        every { AppCompatDelegate.getApplicationLocales() } returns empty

        assertEquals("hi", AppLocale.current())
    }

    @Test
    fun `current returns the language of the first configured locale`() {
        mockkStatic(AppCompatDelegate::class)
        val nonEmpty: LocaleListCompat = mockk()
        every { nonEmpty.isEmpty } returns false
        every { nonEmpty[0] } returns Locale.ENGLISH
        every { AppCompatDelegate.getApplicationLocales() } returns nonEmpty

        assertEquals("en", AppLocale.current())
    }
}
