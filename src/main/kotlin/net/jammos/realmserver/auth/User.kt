package net.jammos.realmserver.auth

import net.jammos.realmserver.utils.types.BigUnsignedInteger
import java.nio.charset.Charset

data class User(
        val username: Username,
        val salt: ByteArray,
        val verifier: BigUnsignedInteger,
        val isBanned: Boolean = false)

data class Username private constructor(val username: String) {
    companion object Username {
        fun username(username: String) = Username(username.toUpperCase())
    }

    fun toByteArray(charset: Charset): ByteArray {
        return username.toByteArray(charset)
    }
}

