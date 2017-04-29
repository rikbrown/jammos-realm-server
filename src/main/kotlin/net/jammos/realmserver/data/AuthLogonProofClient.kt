package net.jammos.realmserver.data

import net.jammos.realmserver.utils.extensions.readBigUnsigned
import net.jammos.realmserver.utils.extensions.readBytes
import net.jammos.realmserver.utils.types.BigUnsignedInteger
import java.io.DataInput

data class AuthLogonProofClient(
        val A: BigUnsignedInteger,
        val M1: BigUnsignedInteger,
        val crc_hash: ByteArray,
        val numberOfKeys: Int,
        val securityFlags: Int
) {
    companion object {
        fun read(input: DataInput): AuthLogonProofClient {
            return AuthLogonProofClient(
                    A = input.readBigUnsigned(32),
                    M1 = input.readBigUnsigned(20),
                    crc_hash = input.readBytes(20),
                    numberOfKeys = input.readUnsignedByte(),
                    securityFlags = input.readUnsignedByte()
            )
        }
    }

}

