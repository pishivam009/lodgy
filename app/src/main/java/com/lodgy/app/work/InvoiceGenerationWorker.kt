package com.lodgy.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lodgy.app.R
import com.lodgy.app.data.prefs.NotificationPreferences
import com.lodgy.app.data.repository.CreditRepository
import com.lodgy.app.data.repository.InvoiceRepository
import com.lodgy.app.data.repository.TenancyAgreementRepository
import com.lodgy.app.notify.CHANNEL_DUES
import com.lodgy.app.notify.LodgyNotifications
import com.lodgy.app.notify.ROUTE_INVOICE_LIST
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar
import kotlinx.coroutines.flow.first

@HiltWorker
class InvoiceGenerationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val tenancyAgreementRepository: TenancyAgreementRepository,
    private val invoiceRepository: InvoiceRepository,
    private val creditRepository: CreditRepository,
    private val notificationPreferences: NotificationPreferences,
    private val notifications: LodgyNotifications,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val today = Calendar.getInstance()
        val dayOfMonth = today.get(Calendar.DAY_OF_MONTH)
        val periodMonth = today.get(Calendar.MONTH) + 1
        val periodYear = today.get(Calendar.YEAR)
        val dueDate = today.timeInMillis

        var created = 0
        var totalDue = 0.0

        tenancyAgreementRepository.getAllActive()
            .filter { it.billingCycleDay == dayOfMonth }
            .forEach { agreement ->
                if (!invoiceRepository.existsForPeriod(agreement.id, periodMonth, periodYear)) {
                    val invoice = invoiceRepository.create(
                        agreement.id, periodMonth, periodYear, agreement.agreedRent, dueDate,
                    )
                    creditRepository.applyPendingTo(agreement.tenantId, invoice.id)
                    created++
                    totalDue += invoice.amountDue
                }
            }

        // Generation is the moment the month's collecting starts, and it used to happen in silence -
        // the warden only found out by opening the app. One summary for the run, never one per
        // invoice: a thirty-tenant hostel would otherwise fire thirty notifications on the same day.
        if (created > 0 && notificationPreferences.duesEnabled.first()) {
            notifications.post(
                channelId = CHANNEL_DUES,
                notificationId = INVOICE_SUMMARY_NOTIFICATION_ID,
                title = applicationContext.getString(R.string.notify_invoices_title),
                text = applicationContext.getString(R.string.notify_invoices_text, created, totalDue),
                route = ROUTE_INVOICE_LIST,
            )
        }

        return Result.success()
    }

}

/** Fixed id so a later run replaces the previous summary rather than stacking another one up.
 *  A literal rather than a hash so it cannot collide with a per-record id derived from a UUID. */
private const val INVOICE_SUMMARY_NOTIFICATION_ID = 1_000_101
