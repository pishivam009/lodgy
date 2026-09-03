package com.lodgy.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentAllocationTest {

    private val balances = listOf(
        OpenBalance("august", 5000.0),
        OpenBalance("september", 5000.0),
    )

    @Test
    fun `a lump sum covering both months settles both`() {
        assertEquals(
            listOf(Allocation("august", 5000.0), Allocation("september", 5000.0)),
            allocatePayment(10000.0, balances),
        )
    }

    @Test
    fun `the oldest month is settled in full before the next gets anything`() {
        assertEquals(
            listOf(Allocation("august", 5000.0), Allocation("september", 2000.0)),
            allocatePayment(7000.0, balances),
        )
    }

    @Test
    fun `less than the oldest month's balance goes entirely to it`() {
        assertEquals(listOf(Allocation("august", 3000.0)), allocatePayment(3000.0, balances))
    }

    @Test
    fun `money beyond every outstanding balance is dropped, not parked on the last invoice`() {
        val allocations = allocatePayment(20000.0, balances)

        assertEquals(10000.0, allocations.sumOf { it.amount }, 0.0001)
        assertEquals(2, allocations.size)
    }

    @Test
    fun `an already settled invoice in the list is skipped`() {
        val allocations = allocatePayment(
            4000.0,
            listOf(OpenBalance("settled", 0.0), OpenBalance("august", 5000.0)),
        )

        assertEquals(listOf(Allocation("august", 4000.0)), allocations)
    }

    @Test
    fun `nothing to pay and nothing to pay it to both allocate nothing`() {
        assertTrue(allocatePayment(0.0, balances).isEmpty())
        assertTrue(allocatePayment(5000.0, emptyList()).isEmpty())
    }
}
