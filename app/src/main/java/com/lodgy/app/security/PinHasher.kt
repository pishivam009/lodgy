package com.lodgy.app.security

import org.mindrot.jbcrypt.BCrypt

object PinHasher {
    fun hash(pin: String): String = BCrypt.hashpw(pin, BCrypt.gensalt())

    fun verify(pin: String, hash: String): Boolean = BCrypt.checkpw(pin, hash)
}
