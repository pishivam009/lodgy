package com.lodgy.app.testutil

import android.content.Intent
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import org.junit.rules.ExternalResource

/**
 * `Intent`/`Uri` come from the android.jar stub used for JVM unit tests, where every method
 * throws "not mocked". This rule swaps their construction/static calls for MockK doubles so
 * Intent-building code (dial/whatsapp/sms link builders) can run for real and be asserted on,
 * without needing Robolectric or a device.
 */
class FakeAndroidIntents : ExternalResource() {
    val capturedUriStrings = mutableListOf<String>()
    val capturedExtras = mutableMapOf<String, String>()

    override fun before() {
        mockkStatic(Uri::class)
        every { Uri.parse(capture(capturedUriStrings)) } returns mockk(relaxed = true)
        mockkConstructor(Intent::class)
        every {
            anyConstructed<Intent>().putExtra(any<String>(), any<String>())
        } answers {
            capturedExtras[firstArg()] = secondArg()
            self as Intent
        }
    }

    override fun after() {
        unmockkConstructor(Intent::class)
        unmockkStatic(Uri::class)
    }
}
