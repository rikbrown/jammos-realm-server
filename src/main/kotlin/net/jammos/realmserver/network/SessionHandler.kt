package net.jammos.realmserver.network

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import mu.KLogging
import net.jammos.realmserver.session.SessionManager
import net.jammos.realmserver.utils.extensions.asSessionId

class SessionHandler(private val sessionManager: SessionManager): ChannelInboundHandlerAdapter() {
    companion object: KLogging()

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        logger.debug { "Registering sessionId(${ctx.asSessionId()}) "}
        sessionManager.registerSession(ctx.asSessionId())
        super.channelRead(ctx, msg)
    }

    override fun channelUnregistered(ctx: ChannelHandlerContext) {
        logger.debug { "Unregistering sessionId(${ctx.asSessionId()})" }
        sessionManager.closeSession(ctx.asSessionId())
    }

}

