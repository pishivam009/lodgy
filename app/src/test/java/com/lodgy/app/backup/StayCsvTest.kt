package com.lodgy.app.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StayCsvTest {

    private fun millis(iso: String): Long =
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(iso)!!.time

    @Test
    fun `reads well-formed rows`() {
        val result = parseStayCsv("9876543210,Sunrise Hostel,101,A,2023-01-15,2024-03-01")

        assertTrue(result.errors.isEmpty())
        assertEquals(
            StayRow("9876543210", "Sunrise Hostel", "101", "A", millis("2023-01-15"), millis("2024-03-01")),
            result.rows.single(),
        )
    }

    @Test
    fun `an optional header row is skipped, and only on the first line`() {
        val withHeader = parseStayCsv("$STAY_CSV_HEADER\n9876543210,Sunrise Hostel,101,A,2023-01-15,2024-03-01")
        assertEquals(1, withHeader.rows.size)
        assertTrue(withHeader.errors.isEmpty())

        val headerLater = parseStayCsv("9876543210,Sunrise Hostel,101,A,2023-01-15,2024-03-01\n$STAY_CSV_HEADER")
        assertEquals(1, headerLater.rows.size)
        assertEquals(1, headerLater.errors.size)
    }

    @Test
    fun `blank lines and stray spaces are tolerated`() {
        val result = parseStayCsv("\n  9876543210 , Sunrise Hostel , 101 , A , 2023-01-15 , 2024-03-01  \n\n")

        assertTrue(result.errors.isEmpty())
        assertEquals("Sunrise Hostel", result.rows.single().hostel)
    }

    @Test
    fun `a bad line is reported with its number and does not stop the rest`() {
        val result = parseStayCsv(
            "9876543210,Sunrise Hostel,101,A,2023-01-15,2024-03-01\nnonsense\n" +
                "9876543211,Sunrise Hostel,102,B,2023-02-01,2023-06-01",
        )

        assertEquals(2, result.rows.size)
        val error = result.errors.single()
        assertEquals(2, error.lineNumber)
        assertEquals(StayRowProblem.WRONG_COLUMN_COUNT, error.reason)
    }

    @Test
    fun `unparsable dates are rejected`() {
        val result = parseStayCsv("9876543210,Sunrise Hostel,101,A,15 Jan 2023,2024-03-01")

        assertTrue(result.rows.isEmpty())
        assertEquals(StayRowProblem.BAD_DATE, result.errors.single().reason)
    }

    @Test
    fun `an end date before the start date is rejected`() {
        val result = parseStayCsv("9876543210,Sunrise Hostel,101,A,2024-03-01,2023-01-15")

        assertEquals(StayRowProblem.BAD_RANGE, result.errors.single().reason)
    }

    @Test
    fun `a row with no phone number cannot be matched to anyone and is rejected`() {
        val result = parseStayCsv(",Sunrise Hostel,101,A,2023-01-15,2024-03-01")

        assertEquals(StayRowProblem.BLANK_PHONE, result.errors.single().reason)
    }

    @Test
    fun `an empty file is not an error, just nothing to import`() {
        val result = parseStayCsv("")

        assertTrue(result.rows.isEmpty())
        assertTrue(result.errors.isEmpty())
    }
}
