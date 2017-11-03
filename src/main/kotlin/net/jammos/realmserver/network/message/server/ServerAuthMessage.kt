package net.jammos.realmserver.network.message.server

import io.netty.buffer.ByteBuf
import java.io.DataOutput

interface ServerAuthMessage {
    fun write(output: ByteBuf)
}