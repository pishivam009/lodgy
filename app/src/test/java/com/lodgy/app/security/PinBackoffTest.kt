package com.lodgy.app.security

import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinBackoffTest {

    @Test
    fun `the first attempts up to the free allowance cost nothing`() {
        for (failures in 0..PinBackoff.FREE_ATTEMPTS) {
            assertEquals("failure #$failures should be free", 0L, PinBackoff.requiredDelayMillis(failures))
        }
    }

    @Test
    fun `the penalty starts at 30 seconds and doubles, capped at five minutes`() {
        assertEquals(TimeUnit.SECONDS.toMillis(30), PinBackoff.requiredDelayMillis(6))
        assertEquals(TimeUnit.SECONDS.toMillis(60), PinBackoff.requiredDelayMillis(7))
        assertEquals(TimeUnit.SECONDS.toMillis(120), PinBackoff.requiredDelayMillis(8))
        assertEquals(TimeUnit.SECONDS.toMillis(240), PinBackoff.requiredDelayMillis(9))
        // Capped from here on, and never above the cap however many failures pile up.
        assertEquals(TimeUnit.MINUTES.toMillis(5), PinBackoff.requiredDelayMillis(10))
        assertEquals(TimeUnit.MINUTES.toMillis(5), PinBackoff.requiredDelayMillis(50))
        assertEquals(TimeUnit.MINUTES.toMillis(5), PinBackoff.requiredDelayMillis(1000))
    }

    @Test
    fun `lockedUntil is the last failure plus the delay`() {
        val lastFailedAt = 10_000L
        // Within the free allowance there is no wait, so the lock instant is not in the future.
        assertEquals(lastFailedAt, PinBackoff.lockedUntil(3, lastFailedAt))
        // Past it, the wait is added on.
        assertEquals(lastFailedAt + TimeUnit.SECONDS.toMillis(30), PinBackoff.lockedUntil(6, lastFailedAt))
    }

    @Test
    fun `the delay never overflows for an absurd failure count`() {
        // The shift is bounded, so a scripted attacker running up the count can't wrap it negative.
        assertTrue(PinBackoff.requiredDelayMillis(Int.MAX_VALUE) > 0)
        assertEquals(TimeUnit.MINUTES.toMillis(5), PinBackoff.requiredDelayMillis(Int.MAX_VALUE))
    }
}
