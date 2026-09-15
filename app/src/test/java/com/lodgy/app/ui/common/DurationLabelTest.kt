package com.lodgy.app.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationLabelTest {

    private val day = 24 * 60 * 60 * 1000L

    @Test
    fun `same day reads as today`() {
        assertEquals(DurationValue(DurationUnit.TODAY, 0), durationValue(1_000L, 1_000L))
    }

    @Test
    fun `a span under 30 days is expressed in days`() {
        assertEquals(DurationValue(DurationUnit.DAYS, 5), durationValue(0L, 5 * day))
        assertEquals(DurationValue(DurationUnit.DAYS, 29), durationValue(0L, 29 * day))
    }

    @Test
    fun `a span under a year is expressed in months`() {
        assertEquals(DurationValue(DurationUnit.MONTHS, 1), durationValue(0L, 30 * day))
        assertEquals(DurationValue(DurationUnit.MONTHS, 12), durationValue(0L, 364 * day))
    }

    @Test
    fun `a whole number of years drops the month`() {
        assertEquals(DurationValue(DurationUnit.YEARS, 1), durationValue(0L, 365 * day))
        assertEquals(DurationValue(DurationUnit.YEARS, 2), durationValue(0L, 730 * day))
    }

    @Test
    fun `a year and a remainder shows both`() {
        assertEquals(DurationValue(DurationUnit.YEARS_MONTHS, 1, 2), durationValue(0L, 425 * day))
    }

    @Test
    fun `an end before the start never goes negative`() {
        assertEquals(DurationValue(DurationUnit.TODAY, 0), durationValue(10 * day, 0L))
    }
}
