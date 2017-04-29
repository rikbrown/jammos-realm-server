package net.jammos.realmserver.utils.types

import java.nio.charset.Charset

data class InternetAddress(
        val address: String,
        val port: Int) {

    fun toByteArray(charset: Charset): ByteArray {
        return String.format("%s:%s", address, port).toByteArray(charset)
    }

}

