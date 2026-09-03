package com.lodgy.app.contact

import android.content.Context
import com.lodgy.app.R
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ReminderMessageBuilderTest {

    private fun millisFor(year: Int, month: Int, day: Int): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.clear()
        cal.set(year, month, day)
        return cal.timeInMillis
    }

    @Test
    fun `english language uses the english template with tenant, rounded amount, date and hostel`() {
        val context = mockk<Context>()
        val resIdSlot = slot<Int>()
        val nameSlot = slot<Any>()
        val amountSlot = slot<Any>()
        val dateSlot = slot<Any>()
        val hostelSlot = slot<Any>()
        every {
            context.getString(capture(resIdSlot), capture(nameSlot), capture(amountSlot), capture(dateSlot), capture(hostelSlot))
        } returns "built message"

        val result = ReminderMessageBuilder.build(
            context = context,
            language = ReminderLanguage.ENGLISH,
            tenantName = "Ravi",
            amountDue = 4999.9,
            dueDateMillis = millisFor(2026, Calendar.SEPTEMBER, 5),
            hostelName = "Sunrise PG",
        )

        assertEquals("built message", result)
        assertEquals(R.string.reminder_template_en, resIdSlot.captured)
        assertEquals("Ravi", nameSlot.captured)
        assertEquals(4999, amountSlot.captured)
        assertEquals("5 September", dateSlot.captured)
        assertEquals("Sunrise PG", hostelSlot.captured)
    }

    @Test
    fun `hindi language uses the hindi template and resource`() {
        val context = mockk<Context>()
        val resIdSlot = slot<Int>()
        every { context.getString(capture(resIdSlot), *anyVararg()) } returns "बनाया गया संदेश"

        val result = ReminderMessageBuilder.build(
            context = context,
            language = ReminderLanguage.HINDI,
            tenantName = "रवि",
            amountDue = 5000.0,
            dueDateMillis = millisFor(2026, Calendar.SEPTEMBER, 5),
            hostelName = "Sunrise PG",
        )

        assertEquals("बनाया गया संदेश", result)
        assertEquals(R.string.reminder_template_hi, resIdSlot.captured)
    }
}
