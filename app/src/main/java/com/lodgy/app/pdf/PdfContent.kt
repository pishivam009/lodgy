package com.lodgy.app.pdf

/** What a document says, with no idea how it will be drawn - so the same content can be laid
 *  out differently (or measured in a unit test) without touching a Canvas. */
sealed interface PdfBlock {
    data class Title(val text: String) : PdfBlock
    data class Heading(val text: String) : PdfBlock
    data class Paragraph(val text: String) : PdfBlock
    data class KeyValue(val label: String, val value: String) : PdfBlock
    data class Table(val headers: List<String>, val rows: List<List<String>>) : PdfBlock
    data class Spacer(val height: Float) : PdfBlock
}

data class PdfDocumentContent(
    /** Shown in the page header and used as the document's own title. */
    val title: String,
    val subtitle: String? = null,
    val blocks: List<PdfBlock> = emptyList(),
)
