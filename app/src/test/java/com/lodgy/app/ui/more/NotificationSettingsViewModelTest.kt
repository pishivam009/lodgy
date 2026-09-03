package com.lodgy.app.ui.more

import com.lodgy.app.data.prefs.NotificationPreferences
import com.lodgy.app.notify.LodgyNotifications
import com.lodgy.app.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NotificationSettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val preferences: NotificationPreferences = mockk(relaxed = true)
    private val notifications: LodgyNotifications = mockk()

    private fun viewModel(
        vacancy: Boolean = true,
        dues: Boolean = true,
        threshold: Int = 7,
        canPost: Boolean = true,
    ): NotificationSettingsViewModel {
        every { preferences.vacancyEnabled } returns flowOf(vacancy)
        every { preferences.duesEnabled } returns flowOf(dues)
        every { preferences.vacancyThresholdDays } returns flowOf(threshold)
        every { notifications.canPost() } returns canPost
        return NotificationSettingsViewModel(preferences, notifications)
    }

    @Test
    fun `exposes both switches and the threshold`() {
        val state = viewModel(vacancy = false, dues = true, threshold = 14).uiState.value

        assertFalse(state.vacancyEnabled)
        assertTrue(state.duesEnabled)
        assertEquals(14, state.vacancyThresholdDays)
    }

    @Test
    fun `a blocked system permission is surfaced regardless of the switches`() {
        assertFalse(viewModel(canPost = false).uiState.value.systemPermissionGranted)
        assertTrue(viewModel(canPost = true).uiState.value.systemPermissionGranted)
    }

    @Test
    fun `turning one category off leaves the other untouched`() {
        coEvery { preferences.setVacancyEnabled(any()) } returns Unit
        coEvery { preferences.setDuesEnabled(any()) } returns Unit

        val viewModel = viewModel()
        viewModel.onVacancyEnabledChange(false)

        coVerify { preferences.setVacancyEnabled(false) }
        coVerify(exactly = 0) { preferences.setDuesEnabled(any()) }
    }

    @Test
    fun `the threshold is a warden setting, not a constant`() {
        coEvery { preferences.setVacancyThresholdDays(any()) } returns Unit

        val viewModel = viewModel()
        assertEquals(listOf(3, 7, 14, 30), viewModel.uiState.value.thresholdOptions)

        viewModel.onThresholdChange(30)
        coVerify { preferences.setVacancyThresholdDays(30) }
    }
}
