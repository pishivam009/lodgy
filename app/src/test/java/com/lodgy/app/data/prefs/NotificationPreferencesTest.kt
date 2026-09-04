package com.lodgy.app.data.prefs

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * One method only, for the same reason as [AuthPreferencesTest]: the DataStore delegate is a
 * process-wide singleton pinned to the first [Context] that resolves it, so a second method would
 * read a deleted temp directory.
 */
class NotificationPreferencesTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `defaults are on with a seven day threshold, and every value round-trips`() = runTest {
        val context: Context = mockk()
        every { context.applicationContext } returns context
        every { context.filesDir } returns tempFolder.newFolder("files")
        val prefs = NotificationPreferences(context)

        assertTrue(prefs.vacancyEnabled.first())
        assertTrue(prefs.duesEnabled.first())
        assertEquals(NotificationPreferences.DEFAULT_VACANCY_THRESHOLD_DAYS, prefs.vacancyThresholdDays.first())
        assertTrue(prefs.notifiedBedIds.first().isEmpty())

        // The two categories are independent - LODGY-60 AC 4.
        prefs.setDuesEnabled(false)
        assertFalse(prefs.duesEnabled.first())
        assertTrue(prefs.vacancyEnabled.first())

        prefs.setVacancyEnabled(false)
        assertFalse(prefs.vacancyEnabled.first())

        prefs.setVacancyThresholdDays(14)
        assertEquals(14, prefs.vacancyThresholdDays.first())

        // Clamped, so a stray value can neither disable nor spam the check.
        prefs.setVacancyThresholdDays(0)
        assertEquals(NotificationPreferences.MIN_THRESHOLD_DAYS, prefs.vacancyThresholdDays.first())
        prefs.setVacancyThresholdDays(9999)
        assertEquals(NotificationPreferences.MAX_THRESHOLD_DAYS, prefs.vacancyThresholdDays.first())

        prefs.setNotifiedBedIds(setOf("b1", "b2"))
        assertEquals(setOf("b1", "b2"), prefs.notifiedBedIds.first())
        prefs.setNotifiedBedIds(emptySet())
        assertTrue(prefs.notifiedBedIds.first().isEmpty())
    }
}
