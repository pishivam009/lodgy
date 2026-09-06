package com.lodgy.app.work

import java.util.Calendar
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * LODGY-90. Every case here is about which side of a date something falls, which is exactly what
 * could not be tested while the decision lived inside the worker and read the real clock.
 */
class BillingScheduleTest {

    private fun date(year: Int, month: Int, day: Int): Long = Calendar.getInstance().apply {
        clear()
        set(year, month - 1, day, 0, 0, 0)
    }.timeInMillis

    private val billingDay = 5
    private val sept5 = date(2026, 9, 5)

    @Test
    fun `on the billing day itself, rent is due`() {
        assertTrue(shouldBillPeriod(billingDay, dayOfMonth = 5, moveInDate = 0L, periodBillingDate = sept5))
    }

    /** The whole point of the ticket: a phone that missed the 5th still bills when it wakes up. */
    @Test
    fun `after the billing day has passed, rent is still due`() {
        assertTrue(shouldBillPeriod(billingDay, dayOfMonth = 6, moveInDate = 0L, periodBillingDate = sept5))
        assertTrue(shouldBillPeriod(billingDay, dayOfMonth = 20, moveInDate = 0L, periodBillingDate = sept5))
        assertTrue(shouldBillPeriod(billingDay, dayOfMonth = 31, moveInDate = 0L, periodBillingDate = sept5))
    }

    @Test
    fun `before the billing day, nothing is due yet`() {
        assertFalse(shouldBillPeriod(billingDay, dayOfMonth = 1, moveInDate = 0L, periodBillingDate = sept5))
        assertFalse(shouldBillPeriod(billingDay, dayOfMonth = 4, moveInDate = 0L, periodBillingDate = sept5))
    }

    /**
     * The regression this change would otherwise have introduced: under the old exact-day rule a
     * tenant onboarded on the 20th was simply never matched, so nobody had to think about it.
     */
    @Test
    fun `a tenancy that began after this period's billing day is not billed for it`() {
        val movedInOn20th = date(2026, 9, 20)
        assertFalse(
            shouldBillPeriod(billingDay, dayOfMonth = 20, moveInDate = movedInOn20th, periodBillingDate = sept5),
        )
        assertFalse(
            shouldBillPeriod(billingDay, dayOfMonth = 30, moveInDate = movedInOn20th, periodBillingDate = sept5),
        )
    }

    @Test
    fun `that same tenancy is billed the following period`() {
        val movedInOn20thSept = date(2026, 9, 20)
        val oct5 = date(2026, 10, 5)
        assertTrue(
            shouldBillPeriod(billingDay, dayOfMonth = 5, moveInDate = movedInOn20thSept, periodBillingDate = oct5),
        )
    }

    /**
     * A real move-in carries a time of day, and the caller normalises it to midnight before
     * comparing - otherwise a tenant who moved in at noon on the billing day would be judged to
     * have arrived after it and go unbilled for a month they lived through.
     */
    @Test
    fun `moving in on the billing day is billed for that period, whatever the hour`() {
        assertTrue(shouldBillPeriod(billingDay, dayOfMonth = 5, moveInDate = sept5, periodBillingDate = sept5))

        val noonOnTheFifth = sept5 + 12 * 60 * 60 * 1000
        assertFalse(
            "raw timestamps compare wrongly, which is why the caller normalises",
            shouldBillPeriod(billingDay, dayOfMonth = 5, moveInDate = noonOnTheFifth, periodBillingDate = sept5),
        )
    }

    @Test
    fun `a tenancy that began before the period is billed once the day arrives`() {
        val lastYear = date(2025, 3, 11)
        assertFalse(shouldBillPeriod(billingDay, dayOfMonth = 4, moveInDate = lastYear, periodBillingDate = sept5))
        assertTrue(shouldBillPeriod(billingDay, dayOfMonth = 5, moveInDate = lastYear, periodBillingDate = sept5))
    }

    /** Billing days are constrained to 1-28 so they exist in February; both ends behave. */
    @Test
    fun `the first and last permitted billing days behave`() {
        assertTrue(shouldBillPeriod(1, dayOfMonth = 1, moveInDate = 0L, periodBillingDate = date(2026, 2, 1)))
        assertFalse(shouldBillPeriod(28, dayOfMonth = 27, moveInDate = 0L, periodBillingDate = date(2026, 2, 28)))
        assertTrue(shouldBillPeriod(28, dayOfMonth = 28, moveInDate = 0L, periodBillingDate = date(2026, 2, 28)))
    }
}
