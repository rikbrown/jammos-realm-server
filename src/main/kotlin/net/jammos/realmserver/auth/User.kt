package net.jammos.realmserver.auth

import net.jammos.realmserver.utils.types.BigUnsignedInteger
import java.nio.charset.Charset
import java.time.Instant

data class User(
        val username: Username,
        val salt: SaltByteArray,
        val verifier: BigUnsignedInteger,
        val suspension: UserSuspension? = null) {

    val isSuspended get() = suspension != null
}

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