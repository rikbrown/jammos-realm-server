package net.jammos.realmserver.network.message.client

import io.netty.buffer.ByteBuf
import net.jammos.utils.extensions.readBigUnsigned
import net.jammos.utils.extensions.readByteArray
import net.jammos.utils.field
import net.jammos.utils.types.BigUnsignedInteger

data class ClientAuthLogonProofMessage(
        val A: BigUnsignedInteger,
        val M1: BigUnsignedInteger,
        val crc_hash: ByteArray, // FIXME: change all ByteArrays to sth immutable
        val numberOfKeys: Short,
        val securityFlags: Short
): ClientAuthMessage {
    companion object : ClientAuthMessage.Reader {
        override fun readBody(input: ByteBuf): ClientAuthLogonProofMessage {
            return with(input) {
                // @formatter:off
                ClientAuthLogonProofMessage(
                        A             = field("A")             { readBigUnsigned(32) },
                        M1            = field("M1")            { readBigUnsigned(20) },
                        crc_hash      = field("crc_hash")      { readByteArray(20) },
                        numberOfKeys  = field("numberOfKeys")  { readUnsignedByte() },
                        securityFlags = field("securityFlags") { readUnsignedByte() }
                )
                // @formatter:on
            }
        }
    }

}




