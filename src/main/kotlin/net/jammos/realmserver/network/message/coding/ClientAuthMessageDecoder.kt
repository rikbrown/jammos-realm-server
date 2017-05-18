package net.jammos.realmserver.network.message.coding

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import mu.KLogging
import net.jammos.realmserver.network.message.client.ClientAuthMessage
import net.jammos.realmserver.utils.extensions.asDataInput

class ClientAuthMessageDecoder: ByteToMessageDecoder() {
    companion object : KLogging()

    override fun decode(ctx: ChannelHandlerContext, buf: ByteBuf, out: MutableList<Any>) {

        try {
            val message = ClientAuthMessage.read(buf.asDataInput())
            logger.debug { "Read message: $message" }
            out.add(message)

        } catch (e: Exception) {
            logger.warn(e) { "Error decoding client message"}
        }

        buf.clear()
    }

}

