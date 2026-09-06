package com.lodgy.app.work

/**
 * Whether a tenancy's rent for the period in progress is due to be invoiced yet.
 *
 * Both halves matter and neither is obvious from the other:
 *
 * - **The billing day has been reached**, rather than being exactly today. Matching only the exact
 *   day meant a phone that was off or dozing on the 5th never billed that month, and failed
 *   silently — no invoice, so no due, so nothing in the overdue count, the reminders or the
 *   dashboard for the warden to notice (LODGY-90).
 * - **The tenancy already existed by that day.** Once the rule becomes "the day has passed", a
 *   tenant onboarded on the 20th with a billing day of 5 would otherwise be invoiced the instant
 *   they were added, for a month that had already started without them. The old exact-day match
 *   hid that case by never matching it.
 *
 * Pure, and separate from the worker, because the interesting cases are all about which side of a
 * date something falls — and a test that has to wait for the 5th of the month to run is no test.
 */
fun shouldBillPeriod(
    billingCycleDay: Int,
    dayOfMonth: Int,
    moveInDate: Long,
    periodBillingDate: Long,
): Boolean = billingCycleDay <= dayOfMonth && moveInDate <= periodBillingDate
