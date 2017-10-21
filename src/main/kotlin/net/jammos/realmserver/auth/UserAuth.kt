package net.jammos.realmserver.auth

import net.jammos.utils.types.BigUnsignedInteger
import java.nio.charset.Charset
import java.time.Instant

data class UserAuth(
        val username: Username,
        val salt: SaltByteArray,
        val verifier: BigUnsignedInteger)

data class Username private constructor(val username: String) {
    companion object Username {
        fun username(username: String) = Username(username.toUpperCase())
    }

    fun toByteArray(charset: Charset): ByteArray {
        return username.toByteArray(charset)
    }

    override fun toString() = username
}

data class UserSuspension(
        val start: Instant,
        val end: Instant? = null)