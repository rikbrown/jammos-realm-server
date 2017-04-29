package net.jammos.realmserver.utils.extensions

import net.jammos.realmserver.utils.types.BigUnsignedInteger
import java.io.DataInput
import java.net.InetAddress
import java.nio.ByteBuffer

fun DataInput.readChars(cnt: Int, reverse: Boolean = true): String {
    val bytes = readBytes(cnt)
    val string = String(bytes).trimEnd { c -> c == Character.MIN_VALUE }
    return if (reverse) string.reversed() else string
}

fun DataInput.readUnsignedInt(): Int {
    val bytes = readBytes(4)
    return ByteBuffer.wrap(bytes).int
}

fun DataInput.readIpAddress(): InetAddress {
    val bytes = readBytes(4)
    return InetAddress.getByAddress(bytes)
}

fun DataInput.readBytes(cnt: Int): ByteArray {
    val buffer = ByteArray(cnt)
    readFully(buffer)
    return buffer
}

fun DataInput.readBigUnsigned(size: Int): BigUnsignedInteger {
    val bytes = readBytes(size)
    return BigUnsignedInteger(bytes)
}