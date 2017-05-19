package net.jammos.realmserver.network

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import mu.KLogging
import net.jammos.realmserver.session.SessionId
import net.jammos.realmserver.session.SessionManager
import net.jammos.realmserver.utils.extensions.asSessionId

/**
 * Manages sessions, which track the lifecycle of a channel.  While the game client may keep refreshing
 * its connection, we might decide that the session needs to end at some point.  In this case, an exception
 * will be thrown when the client tries to read from the session.
 */
class SessionHandler(private val sessionManager: SessionManager): ChannelInboundHandlerAdapter() {
    companion object: KLogging()

    override fun channelRegistered(ctx: ChannelHandlerContext) {
        val sessionId = ctx.asSessionId()
        logger.debug { "Registering session: $sessionId" }
        sessionManager.registerSession(sessionId)

        super.channelRegistered(ctx)
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        val sessionId = ctx.asSessionId()
        logger.debug { "Attempting to resume session: $sessionId"}
        if (sessionManager.resumeSession(sessionId) == null) {
            logger.info { "Session has expired: $sessionId" }
            throw SessionExpiredException(sessionId)
        }

        super.channelRead(ctx, msg)
    }

    override fun channelUnregistered(ctx: ChannelHandlerContext) {
        logger.debug { "Unregistering session: ${ctx.asSessionId()}" }
        sessionManager.closeSession(ctx.asSessionId())
        super.channelUnregistered(ctx)
    }
}

class SessionExpiredException(sessionId: SessionId): IllegalStateException("Session expired: $sessionId")