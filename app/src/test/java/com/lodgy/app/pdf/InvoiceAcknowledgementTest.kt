package com.lodgy.app.pdf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InvoiceAcknowledgementTest {

    private val labels = AcknowledgementLabels(
        title = "Payment acknowledgement",
        tenant = "Tenant",
        roomAndBed = "Room",
        period = "Period",
        invoiceAmount = "Invoice amount",
        credit = "Credit applied",
        amountDue = "Amount due",
        totalPaid = "Total paid",
        balance = "Balance",
        paymentsHeading = "Payments",
        columnDate = "Date",
        columnMode = "Mode",
        columnAmount = "Amount",
        noPayments = "No payments recorded against this invoice yet.",
        issuedOn = "Issued on",
    )

    private fun data(
        creditAmount: String? = null,
        payments: List<AcknowledgementPaymentLine> = listOf(
            AcknowledgementPaymentLine("1 Sep 2026", "Cash", "2000"),
        ),
    ) = AcknowledgementData(
        hostelName = "Sunrise Hostel",
        tenantName = "Ravi",
        roomAndBed = "Room 204 - Bed B",
        period = "9/2026",
        invoiceAmount = "5000",
        creditAmount = creditAmount,
        amountDue = "5000",
        totalPaid = "2000",
        balance = "3000",
        payments = payments,
        issuedOn = "3 Sep 2026",
    )

    private fun PdfDocumentContent.keyValues() =
        blocks.filterIsInstance<PdfBlock.KeyValue>().associate { it.label to it.value }

    @Test
    fun `carries the tenant, room, period, amount and issue date`() {
        val content = buildInvoiceAcknowledgement(data(), labels)
        val values = content.keyValues()

        assertEquals("Payment acknowledgement", content.title)
        assertEquals("Sunrise Hostel", content.subtitle)
        assertEquals("Ravi", values["Tenant"])
        assertEquals("Room 204 - Bed B", values["Room"])
        assertEquals("9/2026", values["Period"])
        assertEquals("5000", values["Invoice amount"])
        assertEquals("3 Sep 2026", values["Issued on"])
    }

    @Test
    fun `payments become a table with one row per recorded payment`() {
        val content = buildInvoiceAcknowledgement(
            data(
                payments = listOf(
                    AcknowledgementPaymentLine("1 Sep 2026", "Cash", "2000"),
                    AcknowledgementPaymentLine("5 Sep 2026", "UPI", "1000"),
                ),
            ),
            labels,
        )

        val table = content.blocks.filterIsInstance<PdfBlock.Table>().single()
        assertEquals(listOf("Date", "Mode", "Amount"), table.headers)
        assertEquals(2, table.rows.size)
        assertEquals(listOf("5 Sep 2026", "UPI", "1000"), table.rows[1])
    }

    @Test
    fun `an invoice with no payments says so instead of printing an empty table`() {
        val content = buildInvoiceAcknowledgement(data(payments = emptyList()), labels)

        assertTrue(content.blocks.none { it is PdfBlock.Table })
        assertTrue(
            content.blocks.filterIsInstance<PdfBlock.Paragraph>()
                .any { it.text == labels.noPayments },
        )
    }

    @Test
    fun `a credit only appears when there is one`() {
        assertTrue(buildInvoiceAcknowledgement(data(), labels).keyValues()["Credit applied"] == null)
        assertEquals(
            "500",
            buildInvoiceAcknowledgement(data(creditAmount = "500"), labels).keyValues()["Credit applied"],
        )
    }

    @Test
    fun `the document paginates without losing content`() {
        val content = buildInvoiceAcknowledgement(
            data(payments = List(200) { AcknowledgementPaymentLine("1 Sep 2026", "Cash", "100") }),
            labels,
        )
        val lines = layoutBlocks(content, TextMeasurer { text, _ -> text.length.toFloat() })

        val pages = paginate(lines)

        assertTrue(pages.size > 1)
        assertEquals(200, pages.sumOf { page -> page.count { it.style == PdfLineStyle.TABLE_ROW } })
    }
}
