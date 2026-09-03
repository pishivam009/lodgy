package com.lodgy.app.contact

import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

enum class ReminderChannel { WHATSAPP, SMS }
enum class ReminderLanguage { HINDI, ENGLISH }

/** Tap-to-send only - the warden always reviews and sends in the target app themselves. */
object ReminderIntents {
    fun whatsApp(phone: String, message: String): Intent {
        val digitsOnly = phone.filter { it.isDigit() }
        val encoded = URLEncoder.encode(message, "UTF-8")
        return Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digitsOnly?text=$encoded"))
    }

    fun sms(phone: String, message: String): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse("smsto:$phone")).putExtra("sms_body", message)
}
