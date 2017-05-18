package net.jammos.realmserver.utils.types

import io.netty.buffer.ByteBuf
import java.io.DataOutput

interface WriteableByte {
    val value: Int

    fun write(output: DataOutput) {
        output.writeByte(value)
    }

    fun write(output: ByteBuf) {
        output.writeByte(value)
    }

}

