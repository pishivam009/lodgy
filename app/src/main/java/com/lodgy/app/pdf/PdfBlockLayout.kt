package com.lodgy.app.pdf

/** Measures one string at a given point size. Backed by Paint on a device, by a stub in tests. */
fun interface TextMeasurer {
    fun width(text: String, textSize: Float): Float
}

val PdfLineStyle.textSize: Float
    get() = when (this) {
        PdfLineStyle.TITLE -> 18f
        PdfLineStyle.SUBTITLE -> 11f
        PdfLineStyle.HEADING -> 14f
        PdfLineStyle.TABLE_HEADER -> 10f
        else -> 11f
    }

/** Column widths are proportional so a table always fills the text column exactly. */
private fun tableWeights(columnCount: Int): List<Float> = List(columnCount) { 1f / columnCount }

/**
 * Turns content blocks into wrapped, measured lines. Every wrap decision happens here, once, so
 * pagination downstream only has to add up heights.
 */
fun layoutBlocks(
    content: PdfDocumentContent,
    measurer: TextMeasurer,
    contentWidth: Float = PdfPage.contentWidth,
): List<PdfLine> {
    val lines = mutableListOf<PdfLine>()

    fun wrapped(style: PdfLineStyle, text: String, width: Float = contentWidth) =
        wrapText(text, width) { measurer.width(it, style.textSize) }
            .map { PdfLine(style, listOf(it), style.lineHeight) }

    lines += PdfLine(PdfLineStyle.TITLE, listOf(content.title), PdfLineStyle.TITLE.lineHeight)
    content.subtitle?.let { lines += PdfLine(PdfLineStyle.SUBTITLE, listOf(it), PdfLineStyle.SUBTITLE.lineHeight) }

    content.blocks.forEach { block ->
        when (block) {
            is PdfBlock.Title -> lines += wrapped(PdfLineStyle.TITLE, block.text)
            is PdfBlock.Heading -> lines += wrapped(PdfLineStyle.HEADING, block.text)
            is PdfBlock.Paragraph -> lines += wrapped(PdfLineStyle.BODY, block.text)
            is PdfBlock.KeyValue -> lines += PdfLine(
                PdfLineStyle.KEY_VALUE,
                listOf(block.label, block.value),
                PdfLineStyle.KEY_VALUE.lineHeight,
            )
            is PdfBlock.Spacer -> lines += PdfLine(PdfLineStyle.SPACER, emptyList(), block.height)
            is PdfBlock.Table -> {
                val weights = tableWeights(block.headers.size)
                lines += PdfLine(
                    PdfLineStyle.TABLE_HEADER,
                    block.headers,
                    PdfLineStyle.TABLE_HEADER.lineHeight,
                    repeatOnPageBreak = true,
                    columnWeights = weights,
                )
                block.rows.forEach { row ->
                    lines += PdfLine(
                        PdfLineStyle.TABLE_ROW,
                        row,
                        PdfLineStyle.TABLE_ROW.lineHeight,
                        columnWeights = weights,
                    )
                }
            }
        }
    }
    return lines
}
