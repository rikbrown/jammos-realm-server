package net.jammos.realmserver.network.message.server

import io.netty.buffer.ByteBuf
import net.jammos.realmserver.network.AuthCommand
import net.jammos.utils.extensions.writeByte

object ServerReconnectProofMessage: ServerAuthMessage {
    override fun write(output: ByteBuf) {
        output.writeByte(AuthCommand.RECONNECT_PROOF)
        output.writeByte(0x00) // error byte
    }
}
