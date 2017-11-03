package net.jammos.realmserver.network.handler

import io.netty.channel.ChannelHandlerContext
import mu.KLogging
import net.jammos.realmserver.auth.AuthManager
import net.jammos.realmserver.network.JammosRealmAttributes.AUTH_CHALLENGE_ATTRIBUTE
import net.jammos.realmserver.network.JammosRealmAttributes.RECONNECT_CHALLENGE_ATTRIBUTE
import net.jammos.realmserver.network.JammosRealmAttributes.USERID_ATTRIBUTE
import net.jammos.realmserver.network.message.client.ClientAuthLogonChallengeMessage
import net.jammos.realmserver.network.message.server.ServerReconnectChallengeMessage
import net.jammos.utils.auth.Username.Username.username
import net.jammos.utils.network.handler.JammosHandler

class ReconnectChallengeHandler(private val authManager: AuthManager): JammosHandler() {

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg !is ClientAuthLogonChallengeMessage) return pass(ctx, msg)
        if (!msg.isReconnect) return pass(ctx, msg) // pass on non-reconnects

        // reject if already in a challenge
        attr(ctx, AUTH_CHALLENGE_ATTRIBUTE)?.let { throw IllegalStateException("Already authenticating - rejecting")}
        attr(ctx, USERID_ATTRIBUTE)?.let { throw IllegalStateException("Already authenticated - rejecting")}
        attr(ctx, RECONNECT_CHALLENGE_ATTRIBUTE)?.let { throw IllegalStateException("Already in reconnect challenge - rejecting")}

        // get identity
        val I = msg.srpIdentity
        val username = username(I.toString())

        logger.debug { "Handling reconnect challenge for username($username)" }

        // generate and save reconnect challenge
        val challenge = authManager.challengeReconnect(username)
        attr(ctx, RECONNECT_CHALLENGE_ATTRIBUTE, challenge)

        // let's demand reconnect proof!
        respond(ctx, ServerReconnectChallengeMessage(challenge.reconnectProof))
    }

    companion object : KLogging()
}