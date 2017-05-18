package net.jammos.realmserver.utils.extensions

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufInputStream
import io.netty.buffer.ByteBufOutputStream
import java.io.DataInput
import java.io.DataOutput

fun ByteBuf.asDataOutput(): DataOutput {
    return ByteBufOutputStream(this)
}

fun ByteBuf.asDataInput(): DataInput {
    return ByteBufInputStream(this)
}
