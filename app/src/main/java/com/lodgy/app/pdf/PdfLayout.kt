package com.lodgy.app.pdf

/** A4 at 72 points per inch, which is the unit android.graphics.pdf.PdfDocument works in. */
object PdfPage {
    const val WIDTH = 595
    const val HEIGHT = 842
    const val MARGIN = 40f
    const val HEADER_HEIGHT = 34f
    const val FOOTER_HEIGHT = 24f

    val contentTop: Float get() = MARGIN + HEADER_HEIGHT
    val contentBottom: Float get() = HEIGHT - MARGIN - FOOTER_HEIGHT
    val contentWidth: Float get() = WIDTH - 2 * MARGIN
    val contentHeight: Float get() = contentBottom - contentTop
}

enum class PdfLineStyle { TITLE, SUBTITLE, HEADING, BODY, KEY_VALUE, TABLE_HEADER, TABLE_ROW, SPACER }

/**
 * One already-wrapped, already-measured thing to draw. Turning blocks into these first is what
 * makes pagination a list-splitting problem rather than something tangled up with Canvas state.
 */
data class PdfLine(
    val style: PdfLineStyle,
    val cells: List<String>,
    val height: Float,
    /** Table headers are re-emitted at the top of each page a table continues onto. */
    val repeatOnPageBreak: Boolean = false,
    val columnWeights: List<Float> = emptyList(),
)

val PdfLineStyle.lineHeight: Float
    get() = when (this) {
        PdfLineStyle.TITLE -> 26f
        PdfLineStyle.SUBTITLE -> 18f
        PdfLineStyle.HEADING -> 22f
        PdfLineStyle.BODY -> 16f
        PdfLineStyle.KEY_VALUE -> 16f
        PdfLineStyle.TABLE_HEADER -> 18f
        PdfLineStyle.TABLE_ROW -> 16f
        PdfLineStyle.SPACER -> 0f
    }

/**
 * Greedy word wrap against a caller-supplied measurer, so the same code wraps real Paint-measured
 * Devanagari on a device and fixed-width fake text in a test.
 *
 * A single word wider than the line is emitted on its own rather than dropped or looped on.
 */
fun wrapText(text: String, maxWidth: Float, measure: (String) -> Float): List<String> {
    if (text.isEmpty()) return listOf("")
    val words = text.split(' ').filter { it.isNotEmpty() }
    if (words.isEmpty()) return listOf("")

    val lines = mutableListOf<String>()
    var current = StringBuilder()
    for (word in words) {
        val candidate = if (current.isEmpty()) word else "$current $word"
        if (measure(candidate) <= maxWidth || current.isEmpty()) {
            current = StringBuilder(candidate)
        } else {
            lines += current.toString()
            current = StringBuilder(word)
        }
    }
    if (current.isNotEmpty()) lines += current.toString()
    return lines
}

/**
 * Splits lines into pages, carrying any [PdfLine.repeatOnPageBreak] line (a table's header row)
 * onto the next page so a table that overflows is still readable.
 *
 * A line taller than a whole page still gets its own page rather than looping forever.
 */
fun paginate(lines: List<PdfLine>, availableHeight: Float = PdfPage.contentHeight): List<List<PdfLine>> {
    if (lines.isEmpty()) return listOf(emptyList())

    val pages = mutableListOf<List<PdfLine>>()
    var page = mutableListOf<PdfLine>()
    var used = 0f
    var repeated: PdfLine? = null

    for (line in lines) {
        if (line.repeatOnPageBreak) repeated = line
        if (used + line.height > availableHeight && page.isNotEmpty()) {
            pages += page
            page = mutableListOf()
            used = 0f
            val header = repeated
            if (header != null && line.style == PdfLineStyle.TABLE_ROW) {
                page += header
                used += header.height
            }
        }
        page += line
        used += line.height
    }
    if (page.isNotEmpty()) pages += page
    return pages
}
