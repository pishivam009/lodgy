package com.lodgy.app.notify

/**
 * The handful of destinations a notification can land on. Shared with LodgyNavHost rather than
 * re-typed there, so a renamed route cannot silently turn a notification into a no-op.
 */
const val ROUTE_VACANT_VIEW = "vacant_view"
const val ROUTE_RECORD_PAYMENT = "record_payment"
const val ROUTE_EXPENSE_FORM = "expense_form"

fun routeToRecordPayment(invoiceId: String) = "$ROUTE_RECORD_PAYMENT/$invoiceId"

fun routeToExpense(expenseId: String) = "$ROUTE_EXPENSE_FORM?expenseId=$expenseId"
