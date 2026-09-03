package com.lodgy.app.data

/** One invoice's share of a lump sum. */
data class Allocation(val invoiceId: String, val amount: Double)

/** What an open invoice still needs, oldest period first. */
data class OpenBalance(val invoiceId: String, val outstanding: Double)

/**
 * Splits one payment across several invoices, oldest first and settling each in full before
 * moving on - the way a warden describes it ("this clears August and part of September"), and
 * the only split that leaves an unambiguous trail.
 *
 * Anything left after every invoice is settled is dropped rather than parked on the last invoice:
 * the app has no concept of an unallocated balance, and quietly overpaying one month would make
 * that invoice's own arithmetic wrong.
 */
fun allocatePayment(amount: Double, balances: List<OpenBalance>): List<Allocation> {
    var remaining = amount
    val allocations = mutableListOf<Allocation>()
    for (balance in balances) {
        if (remaining <= 0.0) break
        if (balance.outstanding <= 0.0) continue
        val share = minOf(remaining, balance.outstanding)
        allocations += Allocation(balance.invoiceId, share)
        remaining -= share
    }
    return allocations
}
