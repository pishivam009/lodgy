package com.lodgy.app.security

import java.util.concurrent.TimeUnit

/**
 * How long the lock screen makes someone wait after too many wrong PINs (LODGY-77). Proportionality,
 * not maximum severity: an ordinary mistype costs nothing, a scripted hammering slows to a crawl, and
 * there is never a permanent lockout - a warden is always eventually able to try again, with the
 * LODGY-76 forgot-PIN route open the whole time.
 */
object PinBackoff {

    /** The first few failures are free, so a warden fumbling their own PIN is never penalised. */
    const val FREE_ATTEMPTS = 5

    private val FIRST_PENALTY = TimeUnit.SECONDS.toMillis(30)
    private val MAX_PENALTY = TimeUnit.MINUTES.toMillis(5)

    /**
     * The delay required before another attempt is accepted, given the number of consecutive
     * failures so far. Zero for the free attempts, then 30s and doubling each further failure up to
     * a five-minute cap.
     */
    fun requiredDelayMillis(consecutiveFailures: Int): Long {
        if (consecutiveFailures <= FREE_ATTEMPTS) return 0L
        val steps = consecutiveFailures - FREE_ATTEMPTS - 1
        // Double per step, but shift a bounded amount so a large failure count can never overflow.
        val scaled = FIRST_PENALTY shl steps.coerceAtMost(16)
        return scaled.coerceAtMost(MAX_PENALTY)
    }

    /** The wall-clock instant before which another attempt is refused, or a value in the past (no
     *  wait) when the failures are still within the free allowance. */
    fun lockedUntil(consecutiveFailures: Int, lastFailedAt: Long): Long =
        lastFailedAt + requiredDelayMillis(consecutiveFailures)
}
