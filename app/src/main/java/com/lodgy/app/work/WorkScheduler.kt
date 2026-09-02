package com.lodgy.app.work

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

private const val INVOICE_GENERATION_WORK_NAME = "invoice-generation"

fun WorkManager.scheduleInvoiceGeneration() {
    val request = PeriodicWorkRequestBuilder<InvoiceGenerationWorker>(1, TimeUnit.DAYS).build()
    enqueueUniquePeriodicWork(INVOICE_GENERATION_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
}
