package com.lodgy.app.notify

import java.util.Calendar
import java.util.concurrent.TimeUnit

/** How many days ahead of a recurring expense's usual day the warden gets a heads-up. */
const val EXPENSE_LEAD_DAYS = 3

/**
 * Whether a recurring expense's usual day of the month falls within the next [EXPENSE_LEAD_DAYS].
 *
 * Read straight off the day component of the last-logged [incurredOn] - the ticket resolved that
 * Expense.isRecurring is enough and no pattern inference across entries is wanted. Months are
 * shorter than 31 days, so a "31st" expense is clamped to the last day of the month being checked
 * rather than skipped in February.
 */
fun isRecurringExpenseDueSoon(incurredOn: Long, now: Long): Boolean {
    val usualDay = Calendar.getInstance().apply { timeInMillis = incurredOn }.get(Calendar.DAY_OF_MONTH)
    val today = Calendar.getInstance().apply { timeInMillis = now }

    for (offset in 0..EXPENSE_LEAD_DAYS) {
        val candidate = Calendar.getInstance().apply { timeInMillis = now + TimeUnit.DAYS.toMillis(offset.toLong()) }
        val lastDayOfThatMonth = candidate.getActualMaximum(Calendar.DAY_OF_MONTH)
        if (candidate.get(Calendar.DAY_OF_MONTH) == minOf(usualDay, lastDayOfThatMonth)) {
            // Only look forward: the day itself and the days before it, never one already passed
            // this month.
            return candidate.timeInMillis >= today.timeInMillis
        }
    }
    return false
}

/** Start of the day [now] falls in - an invoice is overdue only once its due date has fully passed. */
fun startOfDay(now: Long): Long = Calendar.getInstance().apply {
    timeInMillis = now
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis
