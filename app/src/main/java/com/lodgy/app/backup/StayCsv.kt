package com.lodgy.app.backup

import java.text.SimpleDateFormat
import java.util.Locale

/** One past stay, keyed by the phone number and the property/room/bed text already in the app -
 *  a warden's spreadsheet has no internal IDs to match on (LODGY-94). */
data class StayRow(
    val phone: String,
    val hostel: String,
    val room: String,
    val bed: String,
    val startDate: Long,
    val endDate: Long,
)

/** A line that could not be read, kept with its line number so the warden can go fix it. */
data class StayRowError(val lineNumber: Int, val line: String, val reason: StayRowProblem)

enum class StayRowProblem { WRONG_COLUMN_COUNT, BAD_DATE, BLANK_PHONE, BAD_RANGE }

data class StayCsvResult(val rows: List<StayRow>, val errors: List<StayRowError>)

private const val EXPECTED_COLUMNS = 6
private const val DATE_PATTERN = "yyyy-MM-dd"

/** The header the import screen shows and skips if present. */
const val STAY_CSV_HEADER = "phone,hostel,room,bed,start_date,end_date"

/**
 * Parses the stay-backfill CSV. Same shape as [parseHistoryCsv]: forgiving about surroundings
 * (blank lines, an optional header, stray spaces), strict about values, and a bad line is reported
 * with its number rather than dropped silently - a warden typing years of history into a
 * spreadsheet will get some of it wrong and needs to be told which lines.
 */
fun parseStayCsv(text: String): StayCsvResult {
    val rows = mutableListOf<StayRow>()
    val errors = mutableListOf<StayRowError>()
    val dateFormat = SimpleDateFormat(DATE_PATTERN, Locale.US).apply { isLenient = false }

    text.lineSequence().forEachIndexed { index, rawLine ->
        val line = rawLine.trim()
        if (line.isEmpty()) return@forEachIndexed
        if (index == 0 && line.replace(" ", "").equals(STAY_CSV_HEADER, ignoreCase = true)) return@forEachIndexed

        val cells = line.split(',').map { it.trim() }
        val lineNumber = index + 1

        if (cells.size != EXPECTED_COLUMNS) {
            errors += StayRowError(lineNumber, line, StayRowProblem.WRONG_COLUMN_COUNT)
            return@forEachIndexed
        }

        val phone = cells[0]
        if (phone.isEmpty()) {
            errors += StayRowError(lineNumber, line, StayRowProblem.BLANK_PHONE)
            return@forEachIndexed
        }

        val start = runCatching { dateFormat.parse(cells[4])?.time }.getOrNull()
        val end = runCatching { dateFormat.parse(cells[5])?.time }.getOrNull()
        if (start == null || end == null) {
            errors += StayRowError(lineNumber, line, StayRowProblem.BAD_DATE)
            return@forEachIndexed
        }
        if (start > end) {
            errors += StayRowError(lineNumber, line, StayRowProblem.BAD_RANGE)
            return@forEachIndexed
        }

        rows += StayRow(phone, cells[1], cells[2], cells[3], start, end)
    }

    return StayCsvResult(rows, errors)
}
