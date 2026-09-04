package com.lodgy.app.data.prefs

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Single method, same DataStore-singleton constraint as [AuthPreferencesTest]. */
class ThemePreferencesTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `defaults to following the system and round-trips an explicit choice`() = runTest {
        val context: Context = mockk()
        every { context.applicationContext } returns context
        every { context.filesDir } returns tempFolder.newFolder("files")
        val prefs = ThemePreferences(context)

        assertEquals(ThemeMode.SYSTEM, prefs.themeMode.first())

        prefs.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, prefs.themeMode.first())

        prefs.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, prefs.themeMode.first())
    }
}
