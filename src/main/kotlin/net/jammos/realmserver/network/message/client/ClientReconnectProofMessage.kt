package net.jammos.realmserver.network.message.client

import io.netty.buffer.ByteBuf
import net.jammos.utils.extensions.readBigUnsigned
import net.jammos.utils.field
import net.jammos.utils.types.BigUnsignedInteger

data class ClientReconnectProofMessage(
        val R1: BigUnsignedInteger,
        val R2: BigUnsignedInteger,
        val R3: BigUnsignedInteger,
        val numberOfKeys: Short
): ClientAuthMessage {
    companion object : ClientAuthMessage.Reader {
        override fun readBody(input: ByteBuf): ClientReconnectProofMessage {
            return with(input) {
                // @formatter:off
                ClientReconnectProofMessage(
                        R1            = field("R1")            { readBigUnsigned(16) },
                        R2            = field("R2")            { readBigUnsigned(20) },
                        R3            = field("R3")            { readBigUnsigned(20) },
                        numberOfKeys  = field("numberOfKeys")  { readUnsignedByte() }
                )
                // @formatter:on
            }
        }
    }

}


