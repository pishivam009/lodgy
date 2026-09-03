package com.lodgy.app.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {

    @Test
    fun `hash never stores the pin in plaintext`() {
        val hash = PinHasher.hash("1234")
        assertNotEquals("1234", hash)
    }

    @Test
    fun `verify accepts the correct pin`() {
        val hash = PinHasher.hash("4321")
        assertTrue(PinHasher.verify("4321", hash))
    }

    @Test
    fun `verify rejects an incorrect pin`() {
        val hash = PinHasher.hash("4321")
        assertFalse(PinHasher.verify("0000", hash))
    }

    @Test
    fun `hashing the same pin twice produces different hashes`() {
        val first = PinHasher.hash("1234")
        val second = PinHasher.hash("1234")
        assertNotEquals("bcrypt salts each hash, so two hashes of the same pin must differ", first, second)
        assertTrue(PinHasher.verify("1234", first))
        assertTrue(PinHasher.verify("1234", second))
    }
}
