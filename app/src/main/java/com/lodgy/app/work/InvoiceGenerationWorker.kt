package com.lodgy.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lodgy.app.R
import com.lodgy.app.data.entity.ExpenseCategory
import com.lodgy.app.data.entity.TenancyAgreement
import com.lodgy.app.data.prefs.NotificationPreferences
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.CreditRepository
import com.lodgy.app.data.repository.ExpenseRepository
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
    private val expenseRepository: ExpenseRepository,
    private val bedRepository: BedRepository,
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

        val dueToday = tenancyAgreementRepository.getAllActive()
            .filter { it.billingCycleDay == dayOfMonth }

        dueToday
            // A warden's or caretaker's room bills nobody, so it must never generate an invoice -
            // one would show as overdue forever and pollute the money figures (LODGY-82).
            .filter { !it.nonRevenue }
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

        // The other half of the same monthly beat: a warden who pays a caretaker partly in
        // accommodation can ask for the rent they give up to show as a cost (LODGY-84). It rides on
        // the tenancy's own billing day so the expense lands with the month's other money, and it is
        // an EXPENSE - a cost, never a receivable - so it cannot reappear as a due.
        dueToday
            .filter { it.nonRevenue && it.forgoneRentExpense }
            .forEach { recordForgoneRent(it, today) }

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

    /**
     * One entry per tenancy per calendar month. The guard is the month the expense is dated into
     * rather than a flag on the agreement, so a re-run on the same day - or a warden switching the
     * option off and back on - cannot double-count. Switching it off simply stops the next run:
     * entries already recorded are history the warden may have reported on, and are left alone.
     */
    private suspend fun recordForgoneRent(agreement: TenancyAgreement, today: Calendar) {
        val amount = agreement.forgoneRentAmount
            ?: bedRepository.getRoomPrice(agreement.bedId)
            ?: return
        if (amount <= 0.0) return
        val hostelId = bedRepository.getHostelId(agreement.bedId) ?: return
        val monthStart = (today.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nextMonthStart = (monthStart.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
        if (expenseRepository.existsForTenancyInPeriod(agreement.id, monthStart.timeInMillis, nextMonthStart.timeInMillis)) {
            return
        }
        val room = bedRepository.getLocation(agreement.bedId)
        val where = room?.let {
            if (it.propertyType.isSingleUnit) {
                it.propertyName.ifBlank { it.roomNumber }
            } else {
                applicationContext.getString(R.string.bed_location, it.roomNumber, it.bedLabel)
            }
        }
        expenseRepository.create(
            hostelId = hostelId,
            category = ExpenseCategory.ACCOMMODATION,
            amount = amount,
            // NOT marked recurring: that flag exists to nudge the warden to log an expense they pay
            // by hand, and this one logs itself. Marking it would nag them monthly about an entry
            // the app has already made.
            isRecurring = false,
            incurredOn = today.timeInMillis,
            note = applicationContext.getString(R.string.expense_forgone_rent_note, where.orEmpty()),
            tenancyAgreementId = agreement.id,
        )
    }
}

/** Fixed id so a later run replaces the previous summary rather than stacking another one up.
 *  A literal rather than a hash so it cannot collide with a per-record id derived from a UUID. */
private const val INVOICE_SUMMARY_NOTIFICATION_ID = 1_000_101
