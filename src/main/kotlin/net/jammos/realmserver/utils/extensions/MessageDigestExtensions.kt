package net.jammos.realmserver.utils.extensions

import net.jammos.realmserver.utils.types.BigUnsignedInteger
import java.security.MessageDigest

/**
 * Update with multiple byte arrays and then return the digest results
 */
fun MessageDigest.digest(vararg byteArrays: ByteArray): ByteArray {
    byteArrays.forEach { update(it) }
    return digest()
}

/**
 * Update the message digest using the bytes of a big unsigned integer
 */
fun MessageDigest.update(i: BigUnsignedInteger) {
    update(i.bytes)
}