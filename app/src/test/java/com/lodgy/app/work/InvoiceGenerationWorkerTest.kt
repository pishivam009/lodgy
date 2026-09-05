package com.lodgy.app.work

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.lodgy.app.data.entity.AgreementStatus
import com.lodgy.app.data.entity.InvoiceStatus
import com.lodgy.app.data.entity.Invoice
import com.lodgy.app.data.entity.TenancyAgreement
import com.lodgy.app.data.repository.CreditRepository
import com.lodgy.app.data.repository.InvoiceRepository
import com.lodgy.app.data.repository.TenancyAgreementRepository
import com.lodgy.app.data.prefs.NotificationPreferences
import com.lodgy.app.notify.LodgyNotifications
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.util.Calendar
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Drives doWork() directly with mocked WorkerParameters. That is enough because the worker's
 * decision - which agreements bill today, and not billing the same period twice - is plain Kotlin;
 * only the scheduling around it needs WorkManager, and WorkSchedulerTest covers that.
 */
class InvoiceGenerationWorkerTest {

    private val context: Context = mockk(relaxed = true)
    private val params: WorkerParameters = mockk(relaxed = true)
    private val agreementRepository: TenancyAgreementRepository = mockk()
    private val invoiceRepository: InvoiceRepository = mockk()
    private val creditRepository: CreditRepository = mockk()
    private val notificationPreferences: NotificationPreferences = mockk()
    private val notifications: LodgyNotifications = mockk(relaxed = true)

    private val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    private val thisMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
    private val thisYear = Calendar.getInstance().get(Calendar.YEAR)

    private fun agreement(id: String, billingDay: Int, tenantId: String = "t1", nonRevenue: Boolean = false) = TenancyAgreement(
        id = id,
        tenantId = tenantId,
        bedId = "b1",
        agreedRent = 5000.0,
        advanceDeposit = 0.0,
        billingCycleDay = billingDay,
        nonRevenue = nonRevenue,
        moveInDate = 0L,
        moveOutDate = null,
        depositRefundAmount = null,
        status = AgreementStatus.ACTIVE,
        createdAt = 0L,
        updatedAt = 0L,
    )


    private fun invoice(agreementId: String) = Invoice(
        id = "inv-$agreementId",
        tenancyAgreementId = agreementId,
        periodMonth = thisMonth,
        periodYear = thisYear,
        amountDue = 5000.0,
        dueDate = 0L,
        status = InvoiceStatus.UNPAID,
        createdAt = 0L,
        updatedAt = 0L,
    )

    private fun worker(duesEnabled: Boolean = true): InvoiceGenerationWorker {
        every { notificationPreferences.duesEnabled } returns flowOf(duesEnabled)
        return InvoiceGenerationWorker(
            context, params, agreementRepository, invoiceRepository, creditRepository,
            notificationPreferences, notifications,
        )
    }

    @Test
    fun `generates this month's invoice for an agreement billing today`() = runTest {
        coEvery { agreementRepository.getAllActive() } returns listOf(agreement("a1", today))
        coEvery { invoiceRepository.existsForPeriod("a1", thisMonth, thisYear) } returns false
        coEvery { invoiceRepository.create(any(), any(), any(), any(), any()) } returns mockk(relaxed = true)
        coEvery { creditRepository.applyPendingTo(any(), any()) } returns Unit

        assertEquals(ListenableWorker.Result.success(), worker().doWork())

        coVerify { invoiceRepository.create("a1", thisMonth, thisYear, 5000.0, any()) }
    }

    @Test
    fun `agreements billing on another day are left alone`() = runTest {
        val otherDay = if (today == 1) 2 else 1
        coEvery { agreementRepository.getAllActive() } returns listOf(agreement("a1", otherDay))

        worker().doWork()

        coVerify(exactly = 0) { invoiceRepository.create(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `a period already invoiced is not billed twice`() = runTest {
        coEvery { agreementRepository.getAllActive() } returns listOf(agreement("a1", today))
        coEvery { invoiceRepository.existsForPeriod("a1", thisMonth, thisYear) } returns true

        worker().doWork()

        coVerify(exactly = 0) { invoiceRepository.create(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `credits waiting on the tenant's next invoice are attached to the one just generated`() = runTest {
        val generated: com.lodgy.app.data.entity.Invoice = mockk()
        every { generated.id } returns "inv-new"
        every { generated.amountDue } returns 5000.0
        coEvery { agreementRepository.getAllActive() } returns listOf(agreement("a1", today, tenantId = "t7"))
        coEvery { invoiceRepository.existsForPeriod("a1", thisMonth, thisYear) } returns false
        coEvery { invoiceRepository.create(any(), any(), any(), any(), any()) } returns generated
        coEvery { creditRepository.applyPendingTo(any(), any()) } returns Unit

        worker().doWork()

        coVerify { creditRepository.applyPendingTo("t7", "inv-new") }
    }

    @Test
    fun `no active agreements is a quiet success`() = runTest {
        coEvery { agreementRepository.getAllActive() } returns emptyList()

        assertEquals(ListenableWorker.Result.success(), worker().doWork())
        coVerify(exactly = 0) { invoiceRepository.create(any(), any(), any(), any(), any()) }
    }

    /** LODGY-73: generation used to happen in silence, so the warden only learned the month's
     *  collecting had started by opening the app. */
    @Test
    fun `one summary notification is posted for the run, not one per invoice`() = runTest {
        coEvery { agreementRepository.getAllActive() } returns listOf(
            agreement("a1", today, tenantId = "t1"),
            agreement("a2", today, tenantId = "t2"),
            agreement("a3", today, tenantId = "t3"),
        )
        coEvery { invoiceRepository.existsForPeriod(any(), any(), any()) } returns false
        coEvery { invoiceRepository.create(any(), any(), any(), any(), any()) } answers {
            invoice(firstArg())
        }
        coEvery { creditRepository.applyPendingTo(any(), any()) } returns Unit

        worker().doWork()

        verify(exactly = 1) { notifications.post(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `a run that creates no invoices notifies nobody`() = runTest {
        coEvery { agreementRepository.getAllActive() } returns listOf(agreement("a1", today))
        coEvery { invoiceRepository.existsForPeriod("a1", thisMonth, thisYear) } returns true

        worker().doWork()

        verify(exactly = 0) { notifications.post(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `the payments switch being off suppresses the summary but still generates invoices`() = runTest {
        coEvery { agreementRepository.getAllActive() } returns listOf(agreement("a1", today))
        coEvery { invoiceRepository.existsForPeriod("a1", thisMonth, thisYear) } returns false
        coEvery { invoiceRepository.create(any(), any(), any(), any(), any()) } answers { invoice("a1") }
        coEvery { creditRepository.applyPendingTo(any(), any()) } returns Unit

        worker(duesEnabled = false).doWork()

        coVerify(exactly = 1) { invoiceRepository.create(any(), any(), any(), any(), any()) }
        verify(exactly = 0) { notifications.post(any(), any(), any(), any(), any()) }
    }

    /** LODGY-82: a warden's or caretaker's own room bills nobody. Generating an invoice for it
     *  would show as overdue forever and pollute the money figures. */
    @Test
    fun `a non-revenue tenancy never generates an invoice`() = runTest {
        coEvery { agreementRepository.getAllActive() } returns listOf(
            agreement("a1", today, nonRevenue = true),
        )

        worker().doWork()

        coVerify(exactly = 0) { invoiceRepository.create(any(), any(), any(), any(), any()) }
        verify(exactly = 0) { notifications.post(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `an ordinary tenancy on the same day still bills`() = runTest {
        coEvery { agreementRepository.getAllActive() } returns listOf(
            agreement("a1", today, nonRevenue = true),
            agreement("a2", today, tenantId = "t2"),
        )
        coEvery { invoiceRepository.existsForPeriod("a2", thisMonth, thisYear) } returns false
        coEvery { invoiceRepository.create(any(), any(), any(), any(), any()) } answers { invoice("a2") }
        coEvery { creditRepository.applyPendingTo(any(), any()) } returns Unit

        worker().doWork()

        coVerify(exactly = 1) { invoiceRepository.create(any(), any(), any(), any(), any()) }
    }
}
