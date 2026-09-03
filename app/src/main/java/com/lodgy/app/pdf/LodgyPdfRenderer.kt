package com.lodgy.app.pdf

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.OutputStream
import javax.inject.Inject

/**
 * The one PDF writer in the app. Consumers hand it [PdfDocumentContent] and never touch a Canvas.
 *
 * Uses the platform PdfDocument rather than a PDF library on purpose: the system typeface already
 * shapes Devanagari correctly and PdfDocument embeds what it draws, so Hindi content (LODGY-30)
 * comes out right without shipping and subsetting a font ourselves.
 */
class LodgyPdfRenderer @Inject constructor() {

    private fun paintFor(style: PdfLineStyle) = Paint().apply {
        isAntiAlias = true
        textSize = style.textSize
        typeface = when (style) {
            PdfLineStyle.TITLE, PdfLineStyle.HEADING, PdfLineStyle.TABLE_HEADER ->
                Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            else -> Typeface.DEFAULT
        }
        color = when (style) {
            PdfLineStyle.SUBTITLE -> Color.DKGRAY
            else -> Color.BLACK
        }
    }

    fun render(content: PdfDocumentContent, out: OutputStream) {
        val measurer = TextMeasurer { text, size -> Paint().apply { textSize = size }.measureText(text) }
        val pages = paginate(layoutBlocks(content, measurer))

        val document = PdfDocument()
        try {
            pages.forEachIndexed { index, lines ->
                val pageInfo = PdfDocument.PageInfo.Builder(PdfPage.WIDTH, PdfPage.HEIGHT, index + 1).create()
                val page = document.startPage(pageInfo)
                drawPage(page.canvas, lines, content.title, index + 1, pages.size)
                document.finishPage(page)
            }
            document.writeTo(out)
        } finally {
            document.close()
        }
    }

    private fun drawPage(canvas: Canvas, lines: List<PdfLine>, title: String, pageNumber: Int, pageCount: Int) {
        val chrome = paintFor(PdfLineStyle.SUBTITLE)
        canvas.drawText(title, PdfPage.MARGIN, PdfPage.MARGIN + 12f, chrome)
        canvas.drawLine(
            PdfPage.MARGIN,
            PdfPage.MARGIN + 20f,
            PdfPage.WIDTH - PdfPage.MARGIN,
            PdfPage.MARGIN + 20f,
            Paint().apply { color = Color.LTGRAY },
        )
        canvas.drawText(
            "$pageNumber / $pageCount",
            PdfPage.WIDTH - PdfPage.MARGIN - 40f,
            PdfPage.HEIGHT - PdfPage.MARGIN,
            chrome,
        )

        var y = PdfPage.contentTop
        lines.forEach { line ->
            y += line.height
            val paint = paintFor(line.style)
            when {
                line.style == PdfLineStyle.SPACER -> Unit
                line.style == PdfLineStyle.KEY_VALUE -> {
                    canvas.drawText(line.cells[0], PdfPage.MARGIN, y, paintFor(PdfLineStyle.SUBTITLE))
                    canvas.drawText(line.cells[1], PdfPage.MARGIN + PdfPage.contentWidth / 2f, y, paint)
                }
                line.columnWeights.isNotEmpty() -> {
                    var x = PdfPage.MARGIN
                    line.cells.forEachIndexed { column, cell ->
                        canvas.drawText(cell, x, y, paint)
                        x += PdfPage.contentWidth * line.columnWeights[column]
                    }
                }
                else -> canvas.drawText(line.cells.firstOrNull().orEmpty(), PdfPage.MARGIN, y, paint)
            }
        }
    }
}
