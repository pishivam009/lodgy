package com.lodgy.app.notify

import java.util.Calendar
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DuesNudgeTest {

    private fun date(year: Int, month: Int, day: Int): Long = Calendar.getInstance().apply {
        set(year, month - 1, day, 12, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    @Test
    fun `a recurring expense is flagged on its usual day and the days just before`() {
        val lastLogged = date(2026, 8, 10)

        assertTrue(isRecurringExpenseDueSoon(lastLogged, date(2026, 9, 10)))
        assertTrue(isRecurringExpenseDueSoon(lastLogged, date(2026, 9, 8)))
        assertTrue(isRecurringExpenseDueSoon(lastLogged, date(2026, 9, 7)))
    }

    @Test
    fun `it is not flagged well ahead of time or once the day has passed`() {
        val lastLogged = date(2026, 8, 10)

        assertFalse(isRecurringExpenseDueSoon(lastLogged, date(2026, 9, 1)))
        assertFalse(isRecurringExpenseDueSoon(lastLogged, date(2026, 9, 11)))
        assertFalse(isRecurringExpenseDueSoon(lastLogged, date(2026, 9, 20)))
    }

    @Test
    fun `a 31st expense still fires in a shorter month rather than being skipped`() {
        val lastLogged = date(2026, 1, 31)

        assertTrue(isRecurringExpenseDueSoon(lastLogged, date(2026, 2, 28)))
    }

    @Test
    fun `startOfDay strips the time so a due date is overdue only after its day ends`() {
        val midday = date(2026, 9, 4)
        val start = startOfDay(midday)

        val calendar = Calendar.getInstance().apply { timeInMillis = start }
        assertTrue(start <= midday)
        assertTrue(midday - start < TimeUnit.DAYS.toMillis(1))
        assertTrue(calendar.get(Calendar.HOUR_OF_DAY) == 0 && calendar.get(Calendar.MINUTE) == 0)
    }
}
