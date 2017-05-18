package net.jammos.realmserver.network.message.client

import net.jammos.realmserver.utils.extensions.readBigUnsigned
import net.jammos.realmserver.utils.extensions.readBytes
import net.jammos.realmserver.utils.types.BigUnsignedInteger
import java.io.DataInput

data class ClientAuthLogonProofMessage(
        val A: BigUnsignedInteger,
        val M1: BigUnsignedInteger,
        val crc_hash: ByteArray,
        val numberOfKeys: Int,
        val securityFlags: Int
): ClientAuthMessage {
    companion object : ClientAuthMessage.Reader {
        override fun readBody(input: DataInput): ClientAuthLogonProofMessage {
            return ClientAuthLogonProofMessage(
                    A = input.readBigUnsigned(32),
                    M1 = input.readBigUnsigned(20),
                    crc_hash = input.readBytes(20),
                    numberOfKeys = input.readUnsignedByte(),
                    securityFlags = input.readUnsignedByte()
            )
        }
    }

}




