package com.lodgy.app.data

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
