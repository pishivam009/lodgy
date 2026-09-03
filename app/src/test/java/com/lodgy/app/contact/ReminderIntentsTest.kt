package com.lodgy.app.contact

import com.lodgy.app.testutil.FakeAndroidIntents
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ReminderIntentsTest {

    @get:Rule
    val fakeIntents = FakeAndroidIntents()

    @Test
    fun `whatsApp strips non-digits and url-encodes the message into the link`() {
        ReminderIntents.whatsApp("+91 98765-43210", "Rent due: ₹5000 & thanks!")
        val expectedEncoded = java.net.URLEncoder.encode("Rent due: ₹5000 & thanks!", "UTF-8")
        assertEquals(listOf("https://wa.me/919876543210?text=$expectedEncoded"), fakeIntents.capturedUriStrings)
    }

    @Test
    fun `sms builds an smsto uri and puts the message as the sms_body extra`() {
        ReminderIntents.sms("9876543210", "Rent due tomorrow")
        assertEquals(listOf("smsto:9876543210"), fakeIntents.capturedUriStrings)
        assertEquals("Rent due tomorrow", fakeIntents.capturedExtras["sms_body"])
    }
}
