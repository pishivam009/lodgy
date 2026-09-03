package com.lodgy.app.backup

/** One past month for one tenant, keyed by the phone number already in the app. */
data class HistoryRow(
    val phone: String,
    val periodMonth: Int,
    val periodYear: Int,
    val amountDue: Double,
    val amountPaid: Double,
)

/** A line that could not be read, kept with its line number so the warden can go fix it. */
data class HistoryRowError(val lineNumber: Int, val line: String, val reason: HistoryRowProblem)

enum class HistoryRowProblem { WRONG_COLUMN_COUNT, BAD_NUMBER, BAD_PERIOD, BLANK_PHONE }

data class HistoryCsvResult(val rows: List<HistoryRow>, val errors: List<HistoryRowError>)

private const val EXPECTED_COLUMNS = 5

/** The header the export side writes and the import side skips if present. */
const val HISTORY_CSV_HEADER = "phone,month,year,amount_due,amount_paid"

/**
 * Parses the backfill CSV. Deliberately forgiving about surroundings (blank lines, an optional
 * header, stray spaces) and strict about values: a bad line is reported with its number rather
 * than silently dropped or allowed to poison the import, because a warden typing years of history
 * into a spreadsheet will get some of it wrong and needs to be told which lines.
 */
fun parseHistoryCsv(text: String): HistoryCsvResult {
    val rows = mutableListOf<HistoryRow>()
    val errors = mutableListOf<HistoryRowError>()

    text.lineSequence().forEachIndexed { index, rawLine ->
        val line = rawLine.trim()
        if (line.isEmpty()) return@forEachIndexed
        if (index == 0 && line.replace(" ", "").equals(HISTORY_CSV_HEADER, ignoreCase = true)) return@forEachIndexed

        val cells = line.split(',').map { it.trim() }
        val lineNumber = index + 1

        if (cells.size != EXPECTED_COLUMNS) {
            errors += HistoryRowError(lineNumber, line, HistoryRowProblem.WRONG_COLUMN_COUNT)
            return@forEachIndexed
        }

        val phone = cells[0]
        if (phone.isEmpty()) {
            errors += HistoryRowError(lineNumber, line, HistoryRowProblem.BLANK_PHONE)
            return@forEachIndexed
        }

        val month = cells[1].toIntOrNull()
        val year = cells[2].toIntOrNull()
        val due = cells[3].toDoubleOrNull()
        val paid = cells[4].toDoubleOrNull()

        if (month == null || year == null || due == null || paid == null) {
            errors += HistoryRowError(lineNumber, line, HistoryRowProblem.BAD_NUMBER)
            return@forEachIndexed
        }
        if (month !in 1..12 || year < 1900 || due < 0 || paid < 0) {
            errors += HistoryRowError(lineNumber, line, HistoryRowProblem.BAD_PERIOD)
            return@forEachIndexed
        }

        rows += HistoryRow(phone, month, year, due, paid)
    }

    return HistoryCsvResult(rows, errors)
}
