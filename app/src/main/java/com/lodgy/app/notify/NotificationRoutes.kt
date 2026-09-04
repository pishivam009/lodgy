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

/**
 * MainActivity is exported (it has to be, to be launchable), so the route extra on its intent is
 * attacker-controllable: any app on the device can start Lodgy with an arbitrary value. Navigating
 * to an unknown route throws, so the value is checked against the routes notifications actually
 * use before it is followed. Nothing here bypasses the PIN gate - the route is only replayed after
 * unlock - so the risk this closes is a crash, not a data leak.
 */
fun isSupportedNotificationRoute(route: String): Boolean =
    route == ROUTE_VACANT_VIEW ||
        (route.startsWith("$ROUTE_RECORD_PAYMENT/") && route.count { it == '/' } == 1) ||
        route.startsWith("$ROUTE_EXPENSE_FORM?expenseId=")

