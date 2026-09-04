package com.lodgy.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lodgy.app.R
import com.lodgy.app.data.effectiveAmountDue
import com.lodgy.app.data.entity.InvoiceStatus
import com.lodgy.app.data.prefs.NotificationPreferences
import com.lodgy.app.data.repository.CreditRepository
import com.lodgy.app.data.repository.ExpenseRepository
import com.lodgy.app.data.repository.InvoiceRepository
import com.lodgy.app.data.repository.PaymentRepository
import com.lodgy.app.data.repository.TenancyAgreementRepository
import com.lodgy.app.data.repository.TenantRepository
import com.lodgy.app.notify.CHANNEL_DUES
import com.lodgy.app.notify.LodgyNotifications
import com.lodgy.app.notify.isRecurringExpenseDueSoon
import com.lodgy.app.notify.routeToExpense
import com.lodgy.app.notify.ROUTE_INVOICE_LIST
import com.lodgy.app.notify.routeToRecordPayment
import com.lodgy.app.notify.startOfDay
import com.lodgy.app.ui.common.labelRes
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/** Daily check for invoices that have gone overdue and recurring expenses coming round again. */
@HiltWorker
class DuesReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val invoiceRepository: InvoiceRepository,
    private val paymentRepository: PaymentRepository,
    private val creditRepository: CreditRepository,
    private val tenancyAgreementRepository: TenancyAgreementRepository,
    private val tenantRepository: TenantRepository,
    private val expenseRepository: ExpenseRepository,
    private val notificationPreferences: NotificationPreferences,
    private val notifications: LodgyNotifications,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!notificationPreferences.duesEnabled.first()) return Result.success()

        val now = System.currentTimeMillis()
        notifyOverdueInvoices(now)
        notifyRecurringExpenses(now)
        return Result.success()
    }

    private suspend fun notifyOverdueInvoices(now: Long) {
        val context = applicationContext
        val today = startOfDay(now)

        val overdue = invoiceRepository.getAll().first()
            .filter { it.status != InvoiceStatus.PAID && it.dueDate < today }
            .mapNotNull { invoice ->
                val credits = creditRepository.getByInvoiceId(invoice.id).sumOf { it.amount }
                val outstanding = effectiveAmountDue(invoice.amountDue, credits) -
                    paymentRepository.getTotalPaid(invoice.id)
                // A credit or a payment can settle an invoice without its status row having been
                // rewritten yet; nagging about a zero balance would be the wrong nudge.
                if (outstanding <= 0.0) return@mapNotNull null
                invoice to outstanding
            }

        if (overdue.isEmpty()) return

        // One notification for the run, not one per invoice. Thirty late tenants in a lean month
        // would otherwise mean thirty notifications in one morning, which is how a warden ends up
        // turning the whole category off (LODGY-83, same shape as LODGY-74).
        if (overdue.size == 1) {
            val (invoice, outstanding) = overdue.first()
            val agreement = tenancyAgreementRepository.getById(invoice.tenancyAgreementId)
            val tenantName = agreement?.let { tenantRepository.getById(it.tenantId)?.name }.orEmpty()
            notifications.post(
                channelId = CHANNEL_DUES,
                notificationId = OVERDUE_SUMMARY_NOTIFICATION_ID,
                title = context.getString(R.string.notify_overdue_title),
                text = context.getString(
                    R.string.notify_overdue_text,
                    tenantName,
                    invoice.periodMonth,
                    invoice.periodYear,
                    outstanding,
                ),
                // A single overdue invoice can still open straight to recording its payment.
                route = routeToRecordPayment(invoice.id),
            )
        } else {
            notifications.post(
                channelId = CHANNEL_DUES,
                notificationId = OVERDUE_SUMMARY_NOTIFICATION_ID,
                title = context.getString(R.string.notify_overdue_title),
                text = context.getString(
                    R.string.notify_overdue_text_many,
                    overdue.size,
                    overdue.sumOf { it.second },
                ),
                route = ROUTE_INVOICE_LIST,
            )
        }
    }

    private suspend fun notifyRecurringExpenses(now: Long) {
        val context = applicationContext

        expenseRepository.getAll()
            .filter { it.isRecurring && isRecurringExpenseDueSoon(it.incurredOn, now) }
            .forEach { expense ->
                notifications.post(
                    channelId = CHANNEL_DUES,
                    notificationId = expense.id.hashCode(),
                    title = context.getString(R.string.notify_expense_title),
                    text = context.getString(
                        R.string.notify_expense_text,
                        context.getString(expense.category.labelRes),
                        expense.amount,
                    ),
                    route = routeToExpense(expense.id),
                )
            }
    }
}

/** Fixed id so a later run replaces the previous summary rather than stacking another one.
 *  A literal rather than a hash so it cannot collide with a per-record id derived from a UUID. */
private const val OVERDUE_SUMMARY_NOTIFICATION_ID = 1_000_103
