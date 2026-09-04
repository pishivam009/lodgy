package com.lodgy.app.work

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.lodgy.app.data.entity.AgreementStatus
import com.lodgy.app.data.entity.Credit
import com.lodgy.app.data.entity.Expense
import com.lodgy.app.data.entity.ExpenseCategory
import com.lodgy.app.data.entity.Invoice
import com.lodgy.app.data.entity.InvoiceStatus
import com.lodgy.app.data.entity.TenancyAgreement
import com.lodgy.app.data.entity.Tenant
import com.lodgy.app.data.entity.TenantStatus
import com.lodgy.app.data.prefs.NotificationPreferences
import com.lodgy.app.data.repository.CreditRepository
import com.lodgy.app.data.repository.ExpenseRepository
import com.lodgy.app.data.repository.InvoiceRepository
import com.lodgy.app.data.repository.PaymentRepository
import com.lodgy.app.data.repository.TenancyAgreementRepository
import com.lodgy.app.data.repository.TenantRepository
import com.lodgy.app.notify.CHANNEL_DUES
import com.lodgy.app.notify.LodgyNotifications
import com.lodgy.app.notify.routeToExpense
import com.lodgy.app.notify.routeToRecordPayment
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DuesReminderWorkerTest {

    private val context: Context = mockk(relaxed = true)
    private val params: WorkerParameters = mockk(relaxed = true)
    private val invoiceRepository: InvoiceRepository = mockk()
    private val paymentRepository: PaymentRepository = mockk()
    private val creditRepository: CreditRepository = mockk()
    private val agreementRepository: TenancyAgreementRepository = mockk()
    private val tenantRepository: TenantRepository = mockk()
    private val expenseRepository: ExpenseRepository = mockk()
    private val preferences: NotificationPreferences = mockk(relaxed = true)
    private val notifications: LodgyNotifications = mockk(relaxed = true)

    private val yesterday = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
    private val tomorrow = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)

    private fun invoice(id: String, dueDate: Long, status: InvoiceStatus = InvoiceStatus.UNPAID) =
        Invoice(id = id, tenancyAgreementId = "a1", periodMonth = 9, periodYear = 2026, amountDue = 5000.0, dueDate = dueDate, status = status, createdAt = 0L, updatedAt = 0L)

    @Before
    fun setUp() {
        every { preferences.duesEnabled } returns flowOf(true)
        every { invoiceRepository.getAll() } returns flowOf(emptyList())
        coEvery { expenseRepository.getAll() } returns emptyList()
        coEvery { paymentRepository.getTotalPaid(any()) } returns 0.0
        coEvery { creditRepository.getByInvoiceId(any()) } returns emptyList()
        coEvery { agreementRepository.getById("a1") } returns TenancyAgreement(id = "a1", tenantId = "t1", bedId = "b1", agreedRent = 5000.0, advanceDeposit = 0.0, billingCycleDay = 5, moveInDate = 0L, moveOutDate = null, depositRefundAmount = null, status = AgreementStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)
        coEvery { tenantRepository.getById("t1") } returns Tenant(id = "t1", name = "Ravi", phone = "1", photoPath = null, idProofPhotoPath = null, emergencyContactName = "", emergencyContactPhone = "", status = TenantStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)
    }

    private fun worker() = DuesReminderWorker(
        context, params, invoiceRepository, paymentRepository, creditRepository,
        agreementRepository, tenantRepository, expenseRepository, preferences, notifications,
    )

    @Test
    fun `an overdue invoice is notified and the tap opens that invoice`() = runTest {
        every { invoiceRepository.getAll() } returns flowOf(listOf(invoice("inv-1", yesterday)))

        assertEquals(ListenableWorker.Result.success(), worker().doWork())

        coVerify { notifications.post(CHANNEL_DUES, any(), any(), any(), routeToRecordPayment("inv-1")) }
    }

    @Test
    fun `an invoice not yet due, and a paid one, are left alone`() = runTest {
        every { invoiceRepository.getAll() } returns flowOf(
            listOf(invoice("future", tomorrow), invoice("settled", yesterday, InvoiceStatus.PAID)),
        )

        worker().doWork()

        coVerify(exactly = 0) { notifications.post(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `an overdue invoice already covered by payments and credits is not nagged about`() = runTest {
        every { invoiceRepository.getAll() } returns flowOf(listOf(invoice("inv-1", yesterday)))
        coEvery { creditRepository.getByInvoiceId("inv-1") } returns listOf(
            Credit(id = "c1", tenantId = "t1", invoiceId = "inv-1", amount = 2000.0, reason = "Plumbing", createdAt = 0L, updatedAt = 0L),
        )
        coEvery { paymentRepository.getTotalPaid("inv-1") } returns 3000.0

        worker().doWork()

        coVerify(exactly = 0) { notifications.post(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `a recurring expense coming round again is notified and the tap opens it`() = runTest {
        val dayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        val lastLogged = Calendar.getInstance().apply {
            add(Calendar.MONTH, -1)
            set(Calendar.DAY_OF_MONTH, dayOfMonth)
        }.timeInMillis
        coEvery { expenseRepository.getAll() } returns listOf(
            Expense(id = "e1", hostelId = "h1", category = ExpenseCategory.WIFI, amount = 800.0, isRecurring = true, incurredOn = lastLogged, note = null, createdAt = 0L, updatedAt = 0L),
        )

        worker().doWork()

        coVerify { notifications.post(CHANNEL_DUES, any(), any(), any(), routeToExpense("e1")) }
    }

    @Test
    fun `a one-off expense is never notified, however recently it was logged`() = runTest {
        coEvery { expenseRepository.getAll() } returns listOf(
            Expense(id = "e1", hostelId = "h1", category = ExpenseCategory.REPAIR, amount = 800.0, isRecurring = false, incurredOn = System.currentTimeMillis(), note = null, createdAt = 0L, updatedAt = 0L),
        )

        worker().doWork()

        coVerify(exactly = 0) { notifications.post(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `the switch being off skips both checks`() = runTest {
        every { preferences.duesEnabled } returns flowOf(false)
        every { invoiceRepository.getAll() } returns flowOf(listOf(invoice("inv-1", yesterday)))

        assertEquals(ListenableWorker.Result.success(), worker().doWork())

        coVerify(exactly = 0) { notifications.post(any(), any(), any(), any(), any()) }
    }
}
