package net.jammos.realmserver.network

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.util.ReferenceCountUtil
import mu.KLogging
import net.jammos.realmserver.auth.AuthManager
import net.jammos.realmserver.network.step.LogonChallengeStep
import net.jammos.realmserver.network.step.Step
import net.jammos.realmserver.realm.RealmDao
import net.jammos.realmserver.session.SessionManager
import net.jammos.realmserver.utils.extensions.asSessionId

class AuthServerHandler(
        private val sessionManager: SessionManager,
        private val authManager: AuthManager,
        private val realmDao: RealmDao) : ChannelInboundHandlerAdapter() {

    companion object: KLogging()

    // First step is waiting for a login challenge
    private var step: Step<*, *> = LogonChallengeStep(authManager, realmDao)

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        try {
            logger.info { "Read new message: $msg" }

            // Handle message and potentially move to the next step
            val (response, nextStep) = step.handle(msg)
            logger.debug { "Responding with: $response" }

            // Write the response
            ctx.writeAndFlush(response)

            // No more actions possible, close connection
            if (nextStep == null) {
                logger.info("Next step after $step is null, closing connection")
                // If we don't specify this, it seems like we close too quickly
                Thread.sleep(500)
                ctx.close()
                return
            }

            logger.info("step($step) complete, moving to step($nextStep)")
            step = nextStep

        } finally {
            ReferenceCountUtil.release(msg)
        }
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        logger.debug { "Channel read complete" }
        ctx.flush()
    }

    override fun channelUnregistered(ctx: ChannelHandlerContext) {
        sessionManager.closeSession(ctx.asSessionId())
    }

    @Suppress("OverridingDeprecatedMember") // deprecation warning is only on one base class
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        logger.error(cause) { "Caught exception" }
        ctx.close()
    }

}