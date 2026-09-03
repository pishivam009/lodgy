package com.lodgy.app.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryCsvTest {

    @Test
    fun `reads well-formed rows`() {
        val result = parseHistoryCsv("9876543210,8,2026,5000,5000\n9876543211,9,2026,4000,1000")

        assertTrue(result.errors.isEmpty())
        assertEquals(
            listOf(
                HistoryRow("9876543210", 8, 2026, 5000.0, 5000.0),
                HistoryRow("9876543211", 9, 2026, 4000.0, 1000.0),
            ),
            result.rows,
        )
    }

    @Test
    fun `an optional header row is skipped, and only on the first line`() {
        val withHeader = parseHistoryCsv("$HISTORY_CSV_HEADER\n9876543210,8,2026,5000,0")
        assertEquals(1, withHeader.rows.size)
        assertTrue(withHeader.errors.isEmpty())

        val headerLater = parseHistoryCsv("9876543210,8,2026,5000,0\n$HISTORY_CSV_HEADER")
        assertEquals(1, headerLater.rows.size)
        assertEquals(1, headerLater.errors.size)
    }

    @Test
    fun `blank lines and stray spaces are tolerated`() {
        val result = parseHistoryCsv("\n  9876543210 , 8 , 2026 , 5000 , 0  \n\n")

        assertTrue(result.errors.isEmpty())
        assertEquals(HistoryRow("9876543210", 8, 2026, 5000.0, 0.0), result.rows.single())
    }

    @Test
    fun `a bad line is reported with its number and does not stop the rest`() {
        val result = parseHistoryCsv("9876543210,8,2026,5000,0\nnonsense\n9876543211,9,2026,4000,0")

        assertEquals(2, result.rows.size)
        val error = result.errors.single()
        assertEquals(2, error.lineNumber)
        assertEquals(HistoryRowProblem.WRONG_COLUMN_COUNT, error.reason)
    }

    @Test
    fun `non-numeric amounts are rejected rather than read as zero`() {
        val result = parseHistoryCsv("9876543210,8,2026,five thousand,0")

        assertTrue(result.rows.isEmpty())
        assertEquals(HistoryRowProblem.BAD_NUMBER, result.errors.single().reason)
    }

    @Test
    fun `impossible months, years and negative amounts are rejected`() {
        val month = parseHistoryCsv("9876543210,13,2026,5000,0")
        val year = parseHistoryCsv("9876543210,8,26,5000,0")
        val negative = parseHistoryCsv("9876543210,8,2026,-100,0")

        assertEquals(HistoryRowProblem.BAD_PERIOD, month.errors.single().reason)
        assertEquals(HistoryRowProblem.BAD_PERIOD, year.errors.single().reason)
        assertEquals(HistoryRowProblem.BAD_PERIOD, negative.errors.single().reason)
    }

    @Test
    fun `a row with no phone number cannot be matched to anyone and is rejected`() {
        val result = parseHistoryCsv(",8,2026,5000,0")

        assertEquals(HistoryRowProblem.BLANK_PHONE, result.errors.single().reason)
    }

    @Test
    fun `an empty file is not an error, just nothing to import`() {
        val result = parseHistoryCsv("")

        assertTrue(result.rows.isEmpty())
        assertTrue(result.errors.isEmpty())
    }
}
