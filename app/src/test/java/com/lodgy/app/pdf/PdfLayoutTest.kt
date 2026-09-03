package com.lodgy.app.pdf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfLayoutTest {

    /** Every character is one unit wide, so wrap points are exactly predictable. */
    private val monospace = TextMeasurer { text, _ -> text.length.toFloat() }

    @Test
    fun `text wraps on word boundaries at the given width`() {
        val lines = wrapText("one two three four", 9f) { it.length.toFloat() }

        assertEquals(listOf("one two", "three", "four"), lines)
    }

    @Test
    fun `a word longer than the line gets its own line instead of being dropped`() {
        val lines = wrapText("hi supercalifragilistic ok", 5f) { it.length.toFloat() }

        assertEquals(listOf("hi", "supercalifragilistic", "ok"), lines)
    }

    @Test
    fun `empty text still produces one line rather than nothing`() {
        assertEquals(listOf(""), wrapText("", 100f) { it.length.toFloat() })
        assertEquals(listOf(""), wrapText("   ", 100f) { it.length.toFloat() })
    }

    @Test
    fun `content that fits stays on a single page`() {
        val lines = listOf(
            PdfLine(PdfLineStyle.BODY, listOf("a"), 20f),
            PdfLine(PdfLineStyle.BODY, listOf("b"), 20f),
        )

        assertEquals(1, paginate(lines, availableHeight = 100f).size)
    }

    @Test
    fun `overflowing content breaks onto further pages`() {
        val lines = List(10) { PdfLine(PdfLineStyle.BODY, listOf("row $it"), 20f) }

        val pages = paginate(lines, availableHeight = 50f)

        assertEquals(5, pages.size)
        assertEquals(10, pages.sumOf { it.size })
    }

    @Test
    fun `a table's header row repeats at the top of every page it continues onto`() {
        val header = PdfLine(PdfLineStyle.TABLE_HEADER, listOf("Name"), 20f, repeatOnPageBreak = true)
        val rows = List(6) { PdfLine(PdfLineStyle.TABLE_ROW, listOf("row $it"), 20f) }

        val pages = paginate(listOf(header) + rows, availableHeight = 60f)

        assertTrue(pages.size > 1)
        assertTrue(pages.all { page -> page.first().style == PdfLineStyle.TABLE_HEADER })
        assertEquals(6, pages.sumOf { page -> page.count { it.style == PdfLineStyle.TABLE_ROW } })
    }

    @Test
    fun `a line taller than a whole page is placed rather than looping forever`() {
        val pages = paginate(
            listOf(
                PdfLine(PdfLineStyle.BODY, listOf("normal"), 20f),
                PdfLine(PdfLineStyle.BODY, listOf("huge"), 500f),
            ),
            availableHeight = 50f,
        )

        assertEquals(2, pages.size)
        assertEquals("huge", pages[1].single().cells.single())
    }

    @Test
    fun `empty content still yields one page`() {
        assertEquals(1, paginate(emptyList()).size)
    }

    @Test
    fun `blocks become lines with the title and subtitle leading`() {
        val lines = layoutBlocks(
            PdfDocumentContent(
                title = "Receipt",
                subtitle = "Sunrise Hostel",
                blocks = listOf(
                    PdfBlock.KeyValue("Tenant", "Ravi"),
                    PdfBlock.Table(listOf("Period", "Amount"), listOf(listOf("Sep 2026", "5000"))),
                ),
            ),
            monospace,
        )

        assertEquals(PdfLineStyle.TITLE, lines[0].style)
        assertEquals(PdfLineStyle.SUBTITLE, lines[1].style)
        assertEquals(PdfLineStyle.KEY_VALUE, lines[2].style)
        assertEquals(PdfLineStyle.TABLE_HEADER, lines[3].style)
        assertTrue(lines[3].repeatOnPageBreak)
        assertEquals(PdfLineStyle.TABLE_ROW, lines[4].style)
    }

    @Test
    fun `a long paragraph is wrapped into several lines before pagination sees it`() {
        val lines = layoutBlocks(
            PdfDocumentContent(title = "T", blocks = listOf(PdfBlock.Paragraph("a ".repeat(400).trim()))),
            monospace,
        )

        assertTrue(lines.count { it.style == PdfLineStyle.BODY } > 1)
        assertTrue(lines.filter { it.style == PdfLineStyle.BODY }.all { it.cells.single().length <= PdfPage.contentWidth.toInt() })
    }

    @Test
    fun `table columns share the text width evenly`() {
        val lines = layoutBlocks(
            PdfDocumentContent(title = "T", blocks = listOf(PdfBlock.Table(listOf("a", "b", "c", "d"), emptyList()))),
            monospace,
        )

        val weights = lines.first { it.style == PdfLineStyle.TABLE_HEADER }.columnWeights
        assertEquals(4, weights.size)
        assertEquals(1.0f, weights.sum(), 0.0001f)
    }
}
