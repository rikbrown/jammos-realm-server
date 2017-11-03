package net.jammos.realmserver.network.handler

import io.netty.channel.ChannelHandlerContext
import net.jammos.realmserver.auth.AuthManager
import net.jammos.realmserver.network.JammosRealmAttributes.AUTH_CHALLENGE_ATTRIBUTE
import net.jammos.realmserver.network.JammosRealmAttributes.RECONNECT_CHALLENGE_ATTRIBUTE
import net.jammos.realmserver.network.JammosRealmAttributes.USERID_ATTRIBUTE
import net.jammos.realmserver.network.message.client.ClientReconnectProofMessage
import net.jammos.realmserver.network.message.server.ServerReconnectProofMessage
import net.jammos.utils.network.handler.JammosHandler

class ReconnectProofHandler(private val authManager: AuthManager): JammosHandler() {

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg !is ClientReconnectProofMessage) return pass(ctx, msg)

        // reject if already in a challenge
        attr(ctx, AUTH_CHALLENGE_ATTRIBUTE)?.let { throw IllegalStateException("Already authenticating - rejecting")}
        attr(ctx, USERID_ATTRIBUTE)?.let { throw IllegalStateException("Already authenticated - rejecting")}

        val challenge = attr(ctx, RECONNECT_CHALLENGE_ATTRIBUTE) ?: throw IllegalStateException("No reconnect challenge")

        val userId = authManager.proofReconnect(challenge, msg.R1, msg.R2)
        if (userId != null) {
            // Proofed successfully, save username
            attr(ctx, USERID_ATTRIBUTE, userId)
            return respond(ctx, ServerReconnectProofMessage)
        }

        throw IllegalArgumentException("Reconnect proof failed")
    }
}