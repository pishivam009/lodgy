package com.lodgy.app.work

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.lodgy.app.data.entity.AgreementStatus
import com.lodgy.app.data.entity.TenancyAgreement
import com.lodgy.app.data.repository.CreditRepository
import com.lodgy.app.data.repository.InvoiceRepository
import com.lodgy.app.data.repository.TenancyAgreementRepository
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

    private val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    private val thisMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
    private val thisYear = Calendar.getInstance().get(Calendar.YEAR)

    private fun agreement(id: String, billingDay: Int, tenantId: String = "t1") = TenancyAgreement(
        id = id,
        tenantId = tenantId,
        bedId = "b1",
        agreedRent = 5000.0,
        advanceDeposit = 0.0,
        billingCycleDay = billingDay,
        moveInDate = 0L,
        moveOutDate = null,
        depositRefundAmount = null,
        status = AgreementStatus.ACTIVE,
        createdAt = 0L,
        updatedAt = 0L,
    )

    private fun worker() =
        InvoiceGenerationWorker(context, params, agreementRepository, invoiceRepository, creditRepository)

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
}
