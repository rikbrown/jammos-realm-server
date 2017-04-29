package net.jammos.realmserver.utils

object ByteArrays {
    fun randomBytes(count: Int): ByteArray {
        val bytes = ByteArray(count)
        RANDOM.nextBytes(bytes)
        return bytes
    }
}