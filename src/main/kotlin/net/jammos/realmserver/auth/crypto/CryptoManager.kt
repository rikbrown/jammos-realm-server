package net.jammos.realmserver.auth.crypto

import net.jammos.realmserver.utils.extensions.digest
import net.jammos.realmserver.utils.types.BigUnsignedInteger
import java.security.MessageDigest

data class CryptoConstants(
        // TODO: implement a thread-local digest
        val hashProvider: () -> MessageDigest = { MessageDigest.getInstance("SHA-1") },
        val k: BigUnsignedInteger = BigUnsignedInteger(3),
        val g: BigUnsignedInteger = BigUnsignedInteger(7),
        val N: BigUnsignedInteger = BigUnsignedInteger("894B645E89E1535BBDAD5B8B290650530801B18EBFBF5E8FAB3C82872A3E9BB7"),
        val Ng: BigUnsignedInteger = (N * hashProvider()) xor (g * hashProvider()))

class CryptoManager(
        val constants: CryptoConstants) {

    private companion object {
        val COLON = ":".toByteArray(Charsets.UTF_8)
    }

    fun sha1(): MessageDigest {
        return constants.hashProvider()
    }

    fun M1(name_utf8: ByteArray,
           salt: ByteArray,
           A: BigUnsignedInteger,
           B: BigUnsignedInteger,
           K: BigUnsignedInteger): BigUnsignedInteger {

        val sha1 = sha1()
        val name_hash = sha1.digest(name_utf8)
        sha1.update(constants.Ng.bytes)
        sha1.update(name_hash)
        sha1.update(salt)
        sha1.update(A.bytes)
        sha1.update(B.bytes)
        sha1.update(K.bytes)
        return BigUnsignedInteger(sha1.digest())
    }

    fun createPrivateKey(usernameUtf8: ByteArray, passwordUtf8: ByteArray, salt: ByteArray): BigUnsignedInteger {
        val sha1 = sha1()
        val tmp = sha1.digest(usernameUtf8, COLON, passwordUtf8)
        return BigUnsignedInteger(sha1.digest(salt, tmp))
    }

    fun createUserVerifier(privateKey: BigUnsignedInteger,
                           g: BigUnsignedInteger = constants.g,
                           N: BigUnsignedInteger = constants.N): BigUnsignedInteger = g.expMod(privateKey, N)

    fun hashSessionKey(session_key: BigUnsignedInteger): BigUnsignedInteger {
        val sha1 = sha1()
        val bytes = session_key.bytes(32)

        val hash = ByteArray(40)
        val half = ByteArray(16)

        for (i in 0..15) half[i] = bytes[i * 2]
        val sha_even = sha1.digest(half)

        for (i in 0..15) half[i] = bytes[i * 2 + 1]
        val sha_odd  = sha1.digest(half)

        for (i in 0..19) {
            hash[i * 2] = sha_even[i]
            hash[i * 2 + 1] = sha_odd[i]
        }

        return BigUnsignedInteger(hash)
    }

}

