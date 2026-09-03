package com.lodgy.app.pdf

/** One recorded payment, already formatted for the page. */
data class AcknowledgementPaymentLine(val date: String, val mode: String, val amount: String)

data class AcknowledgementData(
    val hostelName: String,
    val tenantName: String,
    val roomAndBed: String,
    val period: String,
    val invoiceAmount: String,
    val creditAmount: String?,
    val amountDue: String,
    val totalPaid: String,
    val balance: String,
    val payments: List<AcknowledgementPaymentLine>,
    val issuedOn: String,
)

/** Every piece of chrome the document says, supplied by the caller from string resources so the
 *  receipt comes out in whichever language the warden is running. */
data class AcknowledgementLabels(
    val title: String,
    val tenant: String,
    val roomAndBed: String,
    val period: String,
    val invoiceAmount: String,
    val credit: String,
    val amountDue: String,
    val totalPaid: String,
    val balance: String,
    val paymentsHeading: String,
    val columnDate: String,
    val columnMode: String,
    val columnAmount: String,
    val noPayments: String,
    val issuedOn: String,
)

/**
 * Builds the per-invoice acknowledgement. Numbers arrive already formatted from the underlying
 * invoice/payment/credit rows - nothing is recomputed here, so what the tenant is handed cannot
 * drift from what the app shows.
 */
fun buildInvoiceAcknowledgement(
    data: AcknowledgementData,
    labels: AcknowledgementLabels,
): PdfDocumentContent = PdfDocumentContent(
    title = labels.title,
    subtitle = data.hostelName,
    blocks = buildList {
        add(PdfBlock.KeyValue(labels.tenant, data.tenantName))
        add(PdfBlock.KeyValue(labels.roomAndBed, data.roomAndBed))
        add(PdfBlock.KeyValue(labels.period, data.period))
        add(PdfBlock.Spacer(10f))
        add(PdfBlock.KeyValue(labels.invoiceAmount, data.invoiceAmount))
        data.creditAmount?.let { add(PdfBlock.KeyValue(labels.credit, it)) }
        add(PdfBlock.KeyValue(labels.amountDue, data.amountDue))
        add(PdfBlock.KeyValue(labels.totalPaid, data.totalPaid))
        add(PdfBlock.KeyValue(labels.balance, data.balance))
        add(PdfBlock.Spacer(14f))
        add(PdfBlock.Heading(labels.paymentsHeading))
        if (data.payments.isEmpty()) {
            add(PdfBlock.Paragraph(labels.noPayments))
        } else {
            add(
                PdfBlock.Table(
                    headers = listOf(labels.columnDate, labels.columnMode, labels.columnAmount),
                    rows = data.payments.map { listOf(it.date, it.mode, it.amount) },
                ),
            )
        }
        add(PdfBlock.Spacer(14f))
        add(PdfBlock.KeyValue(labels.issuedOn, data.issuedOn))
    },
)
