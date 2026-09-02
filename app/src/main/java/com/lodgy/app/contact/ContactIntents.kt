package com.lodgy.app.contact

import android.content.Intent
import android.net.Uri

/** Tap-to-send/tap-to-dial intents only - nothing here is ever launched automatically. */
object ContactIntents {
    fun dial(phone: String): Intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))

    fun whatsApp(phone: String): Intent {
        val digitsOnly = phone.filter { it.isDigit() }
        return Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digitsOnly"))
    }

    fun sms(phone: String): Intent = Intent(Intent.ACTION_VIEW, Uri.parse("smsto:$phone"))
}
