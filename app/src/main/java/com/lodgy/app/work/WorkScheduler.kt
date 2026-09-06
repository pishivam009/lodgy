package com.lodgy.app.work

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

private const val INVOICE_GENERATION_WORK_NAME = "invoice-generation"
private const val VACANCY_CHECK_WORK_NAME = "vacancy-check"
private const val DUES_REMINDER_WORK_NAME = "dues-reminder"
private const val AUTO_BACKUP_WORK_NAME = "auto-backup"

fun WorkManager.scheduleInvoiceGeneration() {
    val request = PeriodicWorkRequestBuilder<InvoiceGenerationWorker>(1, TimeUnit.DAYS).build()
    enqueueUniquePeriodicWork(INVOICE_GENERATION_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
}

/**
 * Both checks run hourly and each reads its own on/off switch, so scheduling is unconditional
 * and turning a category off simply makes its next run a no-op. An empty room and unpaid rent
 * both cost money for as long as nobody acts on them, so the warden hears about them the hour
 * they happen rather than the next morning (LODGY-88).
 *
 * UPDATE, not KEEP: these two were enqueued daily on every phone already running Lodgy, and KEEP
 * would leave them there - the new interval would apply to new installs only, which is the kind
 * of change that looks shipped and is not.
 */
fun WorkManager.scheduleVacancyCheck() {
    val request = PeriodicWorkRequestBuilder<VacancyCheckWorker>(1, TimeUnit.HOURS).build()
    enqueueUniquePeriodicWork(VACANCY_CHECK_WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
}

fun WorkManager.scheduleDuesReminder() {
    val request = PeriodicWorkRequestBuilder<DuesReminderWorker>(1, TimeUnit.HOURS).build()
    enqueueUniquePeriodicWork(DUES_REMINDER_WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
}

/** Daily automatic backup (LODGY-68). Scheduled unconditionally like the others; a run with no
 *  folder chosen is a no-op, so nothing has to be rescheduled when the warden picks one later. */
fun WorkManager.scheduleAutoBackup() {
    val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(1, TimeUnit.DAYS).build()
    enqueueUniquePeriodicWork(AUTO_BACKUP_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
}
