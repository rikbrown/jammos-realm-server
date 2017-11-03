package net.jammos.realmserver.network.handler

import io.netty.channel.ChannelHandlerContext
import mu.KLogging
import net.jammos.realmserver.auth.AuthManager
import net.jammos.realmserver.auth.SuspendedException
import net.jammos.realmserver.auth.UnknownUserException
import net.jammos.realmserver.network.AuthStatus
import net.jammos.realmserver.network.AuthStatus.UNKNOWN_ACCOUNT
import net.jammos.realmserver.network.JammosRealmAttributes.AUTH_CHALLENGE_ATTRIBUTE
import net.jammos.realmserver.network.JammosRealmAttributes.USERID_ATTRIBUTE
import net.jammos.realmserver.network.message.client.ClientAuthLogonChallengeMessage
import net.jammos.realmserver.network.message.server.ServerAuthLogonChallengeResponse
import net.jammos.utils.ByteArrays
import net.jammos.utils.auth.Username
import net.jammos.utils.network.handler.JammosHandler

class LogonChallengeHandler(private val authManager: AuthManager): JammosHandler() {

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg !is ClientAuthLogonChallengeMessage) return pass(ctx, msg)
        if (msg.isReconnect) return pass(ctx, msg) // pass on reconnects to the reconnect handler

        // reject if already authenticating or authenticated
        attr(ctx, AUTH_CHALLENGE_ATTRIBUTE)?.let { throw IllegalStateException("Already authenticating - rejecting")}
        attr(ctx, USERID_ATTRIBUTE)?.let { throw IllegalStateException("Already authenticated - rejecting")}

        // get identity
        val I = msg.srpIdentity
        val username = Username.username(I.toString())

        logger.debug { "Handling logon challenge for username($username)" }

        // challenge logon
        val proofDemand = try {
            authManager.challengeLogon(username, msg.ip)

        // unknown user?
        } catch (e: UnknownUserException) {
            logger.info { "Unknown user attempted logon: $username" }
            return finalResponse(ctx, ServerAuthLogonChallengeResponse(UNKNOWN_ACCOUNT))

        // suspended user?
        } catch (e: SuspendedException) {
            logger.info { "Suspended user attempted logon by $username - ${e.message}" }

            return finalResponse(ctx, ServerAuthLogonChallengeResponse(when(e.temporary) {
                true -> AuthStatus.SUSPENDED
                false -> AuthStatus.BANNED
            }))
        }

        // we need to demand proof now
        logger.debug { "Happy with challenge for I{$I}, now I demand proof!"}

        // save proof attribute
        attr(ctx, AUTH_CHALLENGE_ATTRIBUTE, proofDemand)

        // and respond
        respond(ctx, ServerAuthLogonChallengeResponse(AuthStatus.SUCCESS,
                successData = ServerAuthLogonChallengeResponse.SuccessData(
                        g = proofDemand.g,
                        N = proofDemand.N,
                        B = proofDemand.B,
                        s = proofDemand.s,
                        unk3 = ByteArrays.randomBytes(16),
                        securityFlags = 0)))
    }

    companion object : KLogging()
}