package net.jammos.realmserver.network.handler

import io.netty.channel.ChannelHandlerContext
import mu.KLogging
import net.jammos.realmserver.auth.AuthManager
import net.jammos.realmserver.network.AuthStatus
import net.jammos.realmserver.network.JammosRealmAttributes.AUTH_CHALLENGE_ATTRIBUTE
import net.jammos.realmserver.network.JammosRealmAttributes.USERID_ATTRIBUTE
import net.jammos.realmserver.network.message.client.ClientAuthLogonProofMessage
import net.jammos.realmserver.network.message.server.ServerAuthLogonProofResponse
import net.jammos.utils.network.handler.JammosHandler

class LogonProofHandler(private val authManager: AuthManager): JammosHandler() {
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg !is ClientAuthLogonProofMessage) return pass(ctx, msg)

        // reject if already authenticated
        attr(ctx, USERID_ATTRIBUTE)?.let { throw IllegalStateException("Already authenticated - rejecting")}

        // Extract ongoing status
        val authChallenge = attr(ctx, AUTH_CHALLENGE_ATTRIBUTE)
            ?: throw IllegalStateException("Cannot handle proof without challenge")
        val userAuth = authChallenge.userAuth
        logger.debug { "Handling logon proof for user ${userAuth.username}" }

        // Proof logon
        val M2 = authManager.proofLogon(
                userAuth = userAuth,
                B = authChallenge.B,
                bSecret = authChallenge.bSecret,
                A = msg.A,
                M1 = msg.M1)

        // Proof fail (password mismatch)
        if (M2 == null) {
            logger.info { "Password mismatch for ${userAuth.username}" }
            return finalResponse(ctx, ServerAuthLogonProofResponse(AuthStatus.INCORRECT_PASSWORD))
        }

        // Successful!
        logger.debug { "Happy with proof for user ${userAuth.username}"}

        // - remove challenge and save username (FIXME: user ID)
        attr(ctx, AUTH_CHALLENGE_ATTRIBUTE, null)
        attr(ctx, USERID_ATTRIBUTE, userAuth.userId)

        // - respond
        respond(ctx, ServerAuthLogonProofResponse(AuthStatus.SUCCESS,
                ServerAuthLogonProofResponse.SuccessData(M2)))
    }

    companion object : KLogging()
}