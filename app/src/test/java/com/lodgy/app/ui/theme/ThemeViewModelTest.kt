package com.lodgy.app.ui.theme

import com.lodgy.app.data.prefs.ThemeMode
import com.lodgy.app.data.prefs.ThemePreferences
import com.lodgy.app.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ThemeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val preferences: ThemePreferences = mockk()

    @Test
    fun `exposes the stored theme mode`() {
        every { preferences.themeMode } returns flowOf(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, ThemeViewModel(preferences).themeMode.value)
    }

    @Test
    fun `falls back to system default before any stored value arrives`() {
        every { preferences.themeMode } returns emptyFlow()

        assertEquals(ThemeMode.SYSTEM, ThemeViewModel(preferences).themeMode.value)
    }

    @Test
    fun `setThemeMode persists the choice`() {
        every { preferences.themeMode } returns flowOf(ThemeMode.SYSTEM)
        coEvery { preferences.setThemeMode(any()) } returns Unit

        ThemeViewModel(preferences).setThemeMode(ThemeMode.LIGHT)

        coVerify { preferences.setThemeMode(ThemeMode.LIGHT) }
    }
}
