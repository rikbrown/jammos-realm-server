package net.jammos.realmserver.network.message.server

import com.google.common.base.Preconditions.checkArgument
import io.netty.buffer.ByteBuf
import net.jammos.realmserver.network.AuthCommand
import net.jammos.utils.extensions.writeByte

data class ServerReconnectChallengeMessage(val reconnectProof: ByteArray): ServerAuthMessage {
    init {
        checkArgument(reconnectProof.size == 16, "16 byte reconnect proof expected")
    }

    override fun write(output: ByteBuf) {
        output.writeByte(AuthCommand.RECONNECT_CHALLENGE)
        output.writeByte(0x00) // error byte
        output.writeBytes(reconnectProof)
        output.writeBytes(SIXTEEN_ZERO_BYTES)
    }

    companion object {
        val SIXTEEN_ZERO_BYTES = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
    }
}