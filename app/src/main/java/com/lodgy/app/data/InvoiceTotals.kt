package com.lodgy.app.data

import com.lodgy.app.data.entity.InvoiceStatus

/**
 * The one place a credit is subtracted from an invoice. Every screen that shows what a tenant
 * owes, and every status recalculation, goes through here - otherwise a credit would reduce the
 * displayed total on one screen while another still called the invoice unpaid.
 *
 * Credits never push an invoice negative: relief beyond the invoice total is capped here rather
 * than turning into a refund the app has no way to hand back.
 */
fun effectiveAmountDue(amountDue: Double, creditTotal: Double): Double =
    (amountDue - creditTotal).coerceAtLeast(0.0)

/**
 * An invoice's status from what it needs against what has been paid. The single definition, so
 * recording a payment, deleting a payment and deleting a credit (all of which move one of these
 * numbers) can never disagree about what PAID/PARTIAL/UNPAID means (LODGY-64).
 */
fun invoiceStatusFor(effectiveDue: Double, totalPaid: Double): InvoiceStatus = when {
    totalPaid >= effectiveDue -> InvoiceStatus.PAID
    totalPaid > 0 -> InvoiceStatus.PARTIAL
    else -> InvoiceStatus.UNPAID
}
