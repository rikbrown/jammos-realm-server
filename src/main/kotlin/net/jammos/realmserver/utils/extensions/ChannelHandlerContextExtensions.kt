package net.jammos.realmserver.utils.extensions

import io.netty.channel.ChannelHandlerContext
import net.jammos.realmserver.session.SessionId

fun ChannelHandlerContext.asSessionId(): SessionId {
    return SessionId(this.channel().id().asLongText())
}