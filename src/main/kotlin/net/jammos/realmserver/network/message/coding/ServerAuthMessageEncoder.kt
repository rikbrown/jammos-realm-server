package net.jammos.realmserver.network.message.coding

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder
import mu.KLogging
import net.jammos.realmserver.network.message.server.ServerAuthMessage

class ServerAuthMessageEncoder : MessageToByteEncoder<ServerAuthMessage>() {
    companion object : KLogging()

    override fun encode(ctx: ChannelHandlerContext, msg: ServerAuthMessage, out: ByteBuf) {
        try {
            msg.write(out)

        } catch (e: Exception) {
            logger.error(e) { "Error encoding output: $msg" }

        }
    }
}