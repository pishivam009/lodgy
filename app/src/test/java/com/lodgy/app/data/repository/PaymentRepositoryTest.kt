package com.lodgy.app.data.repository

import com.lodgy.app.data.dao.PaymentDao
import com.lodgy.app.data.entity.Payment
import com.lodgy.app.data.entity.PaymentMode
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentRepositoryTest {

    private val paymentDao: PaymentDao = mockk()
    private val repository = PaymentRepository(paymentDao)

    private fun payment(amount: Double) =
        Payment(invoiceId = "inv-1", amount = amount, paymentMode = PaymentMode.CASH, paidOn = 0L, note = null, createdAt = 0L, updatedAt = 0L)

    @Test
    fun `getTotalPaid sums every payment recorded against the invoice`() = runTest {
        every { paymentDao.getByInvoiceId("inv-1") } returns flowOf(listOf(payment(1000.0), payment(500.5)))
        assertEquals(1500.5, repository.getTotalPaid("inv-1"), 0.0001)
    }

    @Test
    fun `getTotalPaid is zero when nothing has been paid yet`() = runTest {
        every { paymentDao.getByInvoiceId("inv-1") } returns flowOf(emptyList())
        assertEquals(0.0, repository.getTotalPaid("inv-1"), 0.0001)
    }

    @Test
    fun `create persists a payment with the given fields`() = runTest {
        val inserted = slot<Payment>()
        coEvery { paymentDao.insert(capture(inserted)) } returns Unit

        val created = repository.create("inv-1", 2000.0, PaymentMode.UPI, 999L, "part payment")

        assertEquals(2000.0, created.amount, 0.0001)
        assertEquals(PaymentMode.UPI, created.paymentMode)
        assertEquals("part payment", created.note)
        assertEquals(created, inserted.captured)
    }
}
