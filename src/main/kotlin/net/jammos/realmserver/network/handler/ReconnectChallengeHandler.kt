package net.jammos.realmserver.network.handler

import io.netty.channel.ChannelHandlerContext
import mu.KLogging
import net.jammos.realmserver.auth.AuthManager
import net.jammos.realmserver.network.JammosRealmAttributes.AUTH_CHALLENGE_ATTRIBUTE
import net.jammos.realmserver.network.message.client.ClientAuthLogonChallengeMessage
import net.jammos.utils.auth.Username
import net.jammos.utils.network.handler.JammosHandler

class ReconnectChallengeHandler(private val authManager: AuthManager): JammosHandler() {

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg !is ClientAuthLogonChallengeMessage) return pass(ctx, msg)
        if (!msg.isReconnect) return pass(ctx, msg) // pass on non-reconnects

        // reject if already authenticating or authenticated
        attr(ctx, AUTH_CHALLENGE_ATTRIBUTE)?.let { throw IllegalStateException("Already authenticating - rejecting")}
        //attr(ctx, USERNAME_ATTRIBUTE)?.let { throw IllegalStateException("Already authenticated - rejecting")}

        // get identity
        val I = msg.srpIdentity
        val username = Username.username(I.toString())

        logger.debug { "Handling reconnect challenge for username($username)" }




    }

    companion object : KLogging()
}