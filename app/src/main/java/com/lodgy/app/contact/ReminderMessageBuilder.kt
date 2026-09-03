package com.lodgy.app.contact

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReminderMessageBuilder {
    fun build(
        language: ReminderLanguage,
        tenantName: String,
        amountDue: Double,
        dueDateMillis: Long,
        hostelName: String,
    ): String {
        val locale = if (language == ReminderLanguage.HINDI) Locale.forLanguageTag("hi") else Locale.ENGLISH
        val date = SimpleDateFormat("d MMMM", locale).format(Date(dueDateMillis))
        val amount = amountDue.toInt()
        return if (language == ReminderLanguage.HINDI) {
            "नमस्ते $tenantName जी, आपका ₹$amount किराया बकाया है, due date $date। – $hostelName"
        } else {
            "Hi $tenantName, your rent of ₹$amount is due on $date. - $hostelName"
        }
    }
}
