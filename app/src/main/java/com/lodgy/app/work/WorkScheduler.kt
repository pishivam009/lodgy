package com.lodgy.app.work

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

private const val INVOICE_GENERATION_WORK_NAME = "invoice-generation"
private const val VACANCY_CHECK_WORK_NAME = "vacancy-check"
private const val DUES_REMINDER_WORK_NAME = "dues-reminder"

fun WorkManager.scheduleInvoiceGeneration() {
    val request = PeriodicWorkRequestBuilder<InvoiceGenerationWorker>(1, TimeUnit.DAYS).build()
    enqueueUniquePeriodicWork(INVOICE_GENERATION_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
}

/** Both checks are daily and each reads its own on/off switch, so scheduling is unconditional and
 *  turning a category off simply makes its next run a no-op. */
fun WorkManager.scheduleVacancyCheck() {
    val request = PeriodicWorkRequestBuilder<VacancyCheckWorker>(1, TimeUnit.DAYS).build()
    enqueueUniquePeriodicWork(VACANCY_CHECK_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
}

fun WorkManager.scheduleDuesReminder() {
    val request = PeriodicWorkRequestBuilder<DuesReminderWorker>(1, TimeUnit.DAYS).build()
    enqueueUniquePeriodicWork(DUES_REMINDER_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
}
