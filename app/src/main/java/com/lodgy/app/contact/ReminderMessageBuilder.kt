package com.lodgy.app.contact

import android.content.Context
import com.lodgy.app.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReminderMessageBuilder {
    fun build(
        context: Context,
        language: ReminderLanguage,
        tenantName: String,
        amountDue: Double,
        dueDateMillis: Long,
        hostelName: String,
    ): String {
        val locale = if (language == ReminderLanguage.HINDI) Locale.forLanguageTag("hi") else Locale.ENGLISH
        val date = SimpleDateFormat("d MMMM", locale).format(Date(dueDateMillis))
        val amount = amountDue.toInt()
        val templateRes = if (language == ReminderLanguage.HINDI) {
            R.string.reminder_template_hi
        } else {
            R.string.reminder_template_en
        }
        return context.getString(templateRes, tenantName, amount, date, hostelName)
    }
}
