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
/**
 * Overdue means the due date has passed and the invoice is not settled - not merely "unpaid",
 * which would both miss a part-paid invoice and count one that is not due until next month.
 *
 * The single definition, because the Home tile's count and the Payments list it now opens have
 * to agree: a warden who taps "4 overdue" and is shown five rows, or three, stops trusting both
 * numbers (LODGY-89).
 */
fun isOverdue(status: InvoiceStatus, dueDate: Long, startOfToday: Long): Boolean =
    status != InvoiceStatus.PAID && dueDate < startOfToday

fun invoiceStatusFor(effectiveDue: Double, totalPaid: Double): InvoiceStatus = when {
    totalPaid >= effectiveDue -> InvoiceStatus.PAID
    totalPaid > 0 -> InvoiceStatus.PARTIAL
    else -> InvoiceStatus.UNPAID
}
