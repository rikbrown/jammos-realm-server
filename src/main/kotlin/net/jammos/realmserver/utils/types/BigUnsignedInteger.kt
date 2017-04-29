package net.jammos.realmserver.utils.types

import net.jammos.realmserver.utils.*
import net.jammos.realmserver.utils.extensions.bigEndian
import net.jammos.realmserver.utils.extensions.hexString
import net.jammos.realmserver.utils.extensions.xor
import java.math.BigInteger
import java.security.MessageDigest

/**
 * Represents a big unsigned integer out of which a little-endian byte array can be obtained.
 *
 * The byte array is ordered little-endian, the bytes themselves are unchanged.
 *
 * The representation does not include a sign bit.
 */
class BigUnsignedInteger(val big_integer: BigInteger) {
    companion object {
        /**
         * Construct a big unsigned with [bytes] random bytes.
         */
        fun random(bytes: Int): BigUnsignedInteger {
            return BigUnsignedInteger(BigInteger(bytes * 8, RANDOM))
        }
    }

    init {
        assert(big_integer.signum() >= 0)
    }

    /**
     * Construct the big unsigned with the given long value.
     */
    constructor (value: Long): this(BigInteger.valueOf(value))

    /**
     * Construct the big unsigned from a string representation a big endian hex number.
     */
    constructor (byte_string: String): this(BigInteger(byte_string, 16))

    /**
     * Construct the big unsigned from little-endian byte array.
     * The byte array may have trailing zero bytes.
     */
    constructor (bytes: ByteArray): this(BigInteger(bytes.bigEndian())) {
        if (bigIntegerBytes() == bytes.size) _bytes = bytes
    }

    // ---------------------------------------------------------------------------------------------

    // ---------------------------------------------------------------------------------------------

    private var _bytes: ByteArray? = null

    // ---------------------------------------------------------------------------------------------

    /**
     * A little-endian unsigned representation of this number, as a byte array.
     *
     * The returned array is as small as possible and at least [min] bytes long,
     * padded with zeroes at the end if necessary.
     */
    val bytes: ByteArray
        get() {
            some(_bytes) { return it }

            val computed = bytes()
            _bytes = computed
            return computed
        }

    val isZero get() = big_integer == BigInteger.ZERO

    /**
     * Minimum number of bytes required to represent this number.
     */
    private fun bigIntegerBytes(): Int = (big_integer.bitLength() + 7) / 8 // round up


    fun bytes(min: Int = (bigIntegerBytes())): ByteArray {
        val b = _bytes
        if (b != null && b.size >= min) return b

        // This two's complement representation has its bytes ordered big-endian.
        // (Each byte has regular endianness -- no need to mess with them.)
        // The spec guarantees at least a prefix zero bit for positive numbers.
        val bigint_bytes = big_integer.toByteArray()

        // If the magnitude size in bits is a multiple of 8, an extra prefix byte is added for the
        // zero sign bit. We must discard that byte.
        val deleted_byte = if (big_integer.bitLength() % 8 == 0) 1 else 0

        // Number of bytes to copy.
        val nbytes = bigint_bytes.size - deleted_byte

        // Size of the output array, including padding if any.
        val size = if (min > nbytes) min else nbytes

        val out = ByteArray(size)
        val last_index = bigint_bytes.size - 1

        // Copy all bytes from the big-endian representation (potentially excluding the extra
        // prefix byte), reversing their order to create a little-endian representation.
        for (i in 0..(nbytes - 1)) {
            out[i] = bigint_bytes[last_index - i]
        }

        return out
    }

    /**
     * Computes (this ^ exp) % mod.
     */
    fun expMod(exp: BigUnsignedInteger, mod: BigUnsignedInteger): BigUnsignedInteger
            = BigUnsignedInteger(big_integer.modPow(exp.big_integer, mod.big_integer))

    operator fun times(other: BigUnsignedInteger): BigUnsignedInteger = BigUnsignedInteger(big_integer * other.big_integer)

    operator fun plus (other: BigUnsignedInteger): BigUnsignedInteger = BigUnsignedInteger(big_integer + other.big_integer)

    operator fun rem (other: BigUnsignedInteger): BigUnsignedInteger = BigUnsignedInteger(big_integer % other.big_integer)

    /**
     * Returns the big unsigned resulting from applying the given digest over
     * the bytes of this number.
     */
    operator fun times(digest: MessageDigest): BigUnsignedInteger = BigUnsignedInteger(digest.digest(bytes))

    infix fun xor (other: BigUnsignedInteger): BigUnsignedInteger
            = BigUnsignedInteger(bytes xor other.bytes)

    override fun toString(): String = bytes.hexString

    override fun equals (other: Any?): Boolean = other is BigUnsignedInteger && big_integer == other.big_integer

    override fun hashCode(): Int = big_integer.hashCode()
}

