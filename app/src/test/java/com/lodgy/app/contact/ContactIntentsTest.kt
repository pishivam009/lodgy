package com.lodgy.app.contact

import com.lodgy.app.testutil.FakeAndroidIntents
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ContactIntentsTest {

    @get:Rule
    val fakeIntents = FakeAndroidIntents()

    @Test
    fun `dial builds a tel uri from the raw phone number`() {
        ContactIntents.dial("+91 98765 43210")
        assertEquals(listOf("tel:+91 98765 43210"), fakeIntents.capturedUriStrings)
    }

    @Test
    fun `whatsApp strips non-digit characters before building the wa me link`() {
        ContactIntents.whatsApp("+91 98765-43210")
        assertEquals(listOf("https://wa.me/919876543210"), fakeIntents.capturedUriStrings)
    }

    @Test
    fun `sms builds an smsto uri from the raw phone number`() {
        ContactIntents.sms("9876543210")
        assertEquals(listOf("smsto:9876543210"), fakeIntents.capturedUriStrings)
    }
}
