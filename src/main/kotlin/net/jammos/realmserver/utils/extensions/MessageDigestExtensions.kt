package net.jammos.realmserver.utils.extensions

import java.security.MessageDigest

/**
 * Update with multiple byte arrays and then return the digest results
 */
fun MessageDigest.digest(vararg byteArrays: ByteArray): ByteArray {
    byteArrays.forEach { update(it) }
    return digest()
}
